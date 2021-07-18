package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.classesListFileName
import com.github.m4gshm.cds.gradle.CdsPlugin.Plugins.sharedClassesJar
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.BufferedWriter


abstract class SharedClassesList : BaseDryRunnerTask() {

    companion object {
        const val classFileExtension: String = "class"
        const val classFileEnd: String = ".$classFileExtension"
    }

    private val sharedClassesJarTask = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar

    @get:InputFile
    val jar: RegularFileProperty = objectFactory.fileProperty().convention(
        sharedClassesJarTask.archiveFile
    )

    @get:Input
    val staticClassesList: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(
        project.extensions.getByType(CdsExtension::class.java).staticClassesList
    )

    @get:Input
    var useSourceSetClassPath = false

    @get:Input
    var sourceSetName = "main"

    private val dryRunnerClassPath = dryRunnerClass.replace(".", "/")

    @get:Input
    val classesListExcludes: ListProperty<Regex> = objectFactory.listProperty(Regex::class.java).convention(
        listOf(
            dryRunnerClassPath,
            ".*\\\$Proxy(\\d+)\$",
            ".*\\\$\\\$FastClassBySpringCGLIB\\\$\\\$.{0,8}\$",
            ".*\\\$\\\$EnhancerBySpringCGLIB\\\$\\\$.{0,8}\$",
            ".*\\\$\\\$KeyFactoryByCGLIB\\\$\\\$.{0,8}\$"
        ).map { it.toRegex() }
    )

    @get:OutputFile
    val outputFile: RegularFileProperty =
        objectFactory.fileProperty().convention(buildDirectory.file(classesListFileName))

//    @get:Internal
//    val loadLogFile: RegularFileProperty = objectFactory.fileProperty().convention(
//        buildDirectory.file("class-load.log")
//    )

    init {
        val sharedClassesJar = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar
        dryRunMainClass.convention(sharedClassesJar.mainClass)
        dependsOn(sharedClassesJar)
    }

    @TaskAction
    override fun exec() {
        initClassPath()

        initRunner()

        val outputFile = outputFile.get().asFile
        logger.log(logLevel, "output file $outputFile")

        val supportedClasses = SupportedClassesExtractor(
            dryRunnerClassPath, classpath, logger, logLevel
        ).extractSupportedClasses()
        if (staticClassesList.get()) outputFile.bufferedWriter().use { writer ->
            supportedClasses.forEach {
                writer.write(it)
                writer.newLine()
            }
        } else {
//            val logFile = loadLogFile.get().asFile
            jvmArgs(
                "-Xshare:off",
//                "-Xlog:class+load=info:file=$logFile:tags",
                "-XX:DumpLoadedClassList=$outputFile"
            )

            super.exec()

            if (outputFile.exists()) {
                val excludes = classesListExcludes.get()
                val classes = outputFile.readLines()
                var filteredClasses = classes.filter { className ->
                    val exclude = excludes.firstOrNull { excludeFilter -> excludeFilter.matches(className) } != null
                    if (exclude) logger.log(logLevel, "exclude class $className")
                    !exclude
                }

                logger.log(
                    logLevel, "class amount before name filtering ${classes.size}" +
                            " and after ${filteredClasses.size}"
                )

                val before = filteredClasses.size
//                if (logFile.exists()) filteredClasses = SupportedClassesByLogFilter(
//                    logger, logLevel, logFile
//                ).filterByLog(filteredClasses)
//                else
                filteredClasses = filteredClasses.filter { supportedClasses.contains(it) }

                logger.log(
                    logLevel, "class amount before class version filtering $before" +
                            " and after ${filteredClasses.size}"
                )

                BufferedWriter(outputFile.writer()).use { writer ->
                    filteredClasses.forEach { className ->
                        writer.write(className)
                        writer.newLine()
                    }
                }
            } else logger.error("filtering error: $outputFile doesn't exists")
        }
    }

    private fun initClassPath() {
        if (useSourceSetClassPath) {
            val sourceSets = project.extensions.findByName("sourceSets")
            if (sourceSets is SourceSetContainer) {
                val sourceSetName = this.sourceSetName
                val sourceSet = sourceSets.findByName(sourceSetName)
                if (sourceSet != null) {
                    val runtimeClasspath = sourceSet.runtimeClasspath
                    logger.log(logLevel, "set main source set's classpath by default ${runtimeClasspath.asPath}")
                    classpath = runtimeClasspath
                } else logger.warn("$sourceSetName sourceSet is absent")
            } else logger.warn("sourceSets is absent")
        } else {
            val jarFile = jar.get().asFile
            logger.log(logLevel, "put jar file to classpath, $jarFile")
            classpath = project.files(jarFile)
        }
    }
}