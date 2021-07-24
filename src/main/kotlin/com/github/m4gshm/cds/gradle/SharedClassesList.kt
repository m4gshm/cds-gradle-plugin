package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.classesListFileName
import com.github.m4gshm.cds.gradle.CdsPlugin.Plugins.sharedClassesJar
import com.github.m4gshm.cds.gradle.util.ClassSupportInfoService
import com.github.m4gshm.cds.gradle.util.SupportedClassesClassificatory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.BufferedWriter
import java.io.File
import java.util.jar.JarFile


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
    val staticList: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(
        project.extensions.getByType(CdsExtension::class.java).staticClassList
    )

    @get:Input
    var useSourceSetClassPath = false

    @get:Input
    var sourceSetName = "main"

    private val dryRunnerClassPath = dryRunnerClass.replace(".", "/")

    @get:Input
    val excludes: ListProperty<Regex> = objectFactory.listProperty(Regex::class.java).convention(
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

    @get:Input
    val options: Property<ClassListOptions> = objectFactory.property(ClassListOptions::class.java).convention(
        project.extensions.getByType(CdsExtension::class.java).classListOptions
    )

    init {
        val sharedClassesJar = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar
        dryRunMainClass.convention(sharedClassesJar.mainClass)
        dependsOn(sharedClassesJar)
    }


    @TaskAction
    override fun exec() {
        val usedClasspathSources = initClassPath()

        initRunner()

        val outputFile = outputFile.get().asFile
        logger.log(logLevel, "output file $outputFile")

        val options = options.get()
        val (supported, unsupported) = SupportedClassesClassificatory(
            usedClasspathSources, logger, logLevel, ClassSupportInfoService(
                logger, logLevel, options
            )
        ).classify()

        if (options.logSupportedClasses) buildDirectory.file("supported.txt").get().asFile.bufferedWriter()
            .use { writer ->
                supported.forEach {
                    writer.write(it)
                    writer.newLine()
                }
            }

        if (options.logUnsupportedClasses) buildDirectory.file("unsupported.txt").get().asFile.bufferedWriter()
            .use { writer ->
                unsupported.forEach {
                    writer.write(it)
                    writer.newLine()
                }
            }

        if (staticList.get()) outputFile.bufferedWriter().use { writer ->
            supported.forEach {
                writer.write(it)
                writer.newLine()
            }
        } else {
            jvmArgs(
                "-Xshare:off",
                "-XX:DumpLoadedClassList=$outputFile"
            )

            super.exec()

            if (outputFile.exists()) {
                val excludes = excludes.get()
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

                val beforeVersionFiltering = filteredClasses.size
                filteredClasses = filteredClasses.filter { !unsupported.contains(it) }

                logger.log(
                    logLevel, "class amount before support checking $beforeVersionFiltering" +
                            " and after ${filteredClasses.size}"
                )

//                val beforeFoundFiltering = filteredClasses.size
//                filteredClasses = filteredClasses.filter {
//                    val contains = supported.contains(it)
//                    if (!contains) logger.log(
//                        logLevel, "class is listed but not found in classpath $it"
//                    )
//                    contains
//                }
//                logger.log(
//                    logLevel, "class amount before class found filtering $beforeFoundFiltering" +
//                            " and after ${filteredClasses.size}"
//                )

                BufferedWriter(outputFile.writer()).use { writer ->
                    filteredClasses.forEach { className ->
                        writer.write(className)
                        writer.newLine()
                    }
                }
            } else logger.error("filtering error: $outputFile doesn't exists")
        }
    }

    private fun initClassPath(): FileCollection {
        return if (useSourceSetClassPath) {
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
            classpath
        } else {
            val jarFile = jar.get().asFile
            logger.log(logLevel, "put jar file to classpath, $jarFile")
            classpath = project.files(jarFile)
            val jarClassPath = JarFile(jarFile).manifest.mainAttributes.getValue("Class-Path")
            if (jarClassPath != null) {
                val parentFile = jarFile.parentFile
                val libs = jarClassPath.split(" ").map { File(parentFile, it) }
                classpath + project.files(libs)
            } else classpath
        }
    }
}