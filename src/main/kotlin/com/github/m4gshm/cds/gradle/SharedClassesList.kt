package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.dumpLoadedClassListFileName
import com.github.m4gshm.cds.gradle.CdsPlugin.Tasks.sharedClassesJar
import com.github.m4gshm.cds.gradle.util.ClassSupportInfoService
import com.github.m4gshm.cds.gradle.util.SupportedClassesClassificatory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedWriter


abstract class SharedClassesList : BaseDryRunnerTask() {

    companion object {
        const val classFileExtension: String = "class"
        const val classFileEnd: String = ".$classFileExtension"
    }

    @get:Input
    abstract val staticList: Property<Boolean>

    private val dryRunnerClassPath = dryRunnerClass.map { it.replace(".", "/") }

    @get:OutputFile
    val dumpLoadedClassList: RegularFileProperty = objectFactory.fileProperty().convention(
        buildDirectory.file(dumpLoadedClassListFileName)
    )

    @get:Input
    abstract val options: Property<ClassListOptions>

    @get:Input
    val excludes: ListProperty<Regex> = objectFactory.listProperty(Regex::class.java).convention(options.map {
        it.exclude
    })

    init {
        val sharedClassesJar = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar
        dryRunMainClass.convention(sharedClassesJar.mainClass)

        dependsOn(sharedClassesJar)
    }

    @TaskAction
    override fun exec() {
        addRunnerArgs()
        super.exec()
        val jarDependencies = getJarManifestClassPath(jar.get().asFile)

        val dumpLoadedClassList = dumpLoadedClassList.get().asFile
        val logLevel = logLevel.get()
        logger.log(logLevel, "output file $dumpLoadedClassList")

        val options = options.get()

        logger.log(logLevel, "classes classificatory dependencies ${jarDependencies.asPath}")
        val (supported, unsupported) = SupportedClassesClassificatory(
            jarDependencies, logger, logLevel, ClassSupportInfoService(
                logger, logLevel, options
            )
        ).classify()

        if (options.logSupportedClasses) buildDirectory.file("supported.txt").get().asFile.bufferedWriter(
        ).use { writer ->
            supported.forEach {
                writer.write(it)
                writer.newLine()
            }
        }

        if (options.logUnsupportedClasses) buildDirectory.file("unsupported.txt").get().asFile.bufferedWriter(
        ).use { writer ->
            unsupported.forEach {
                writer.write(it)
                writer.newLine()
            }
        }

        if (staticList.get()) dumpLoadedClassList.bufferedWriter().use { writer ->
            supported.forEach {
                writer.write(it)
                writer.newLine()
            }
        } else {
            jvmArgs(
                "-Xshare:off",
                "-XX:DumpLoadedClassList=$dumpLoadedClassList"
            )

            super.exec()

            if (dumpLoadedClassList.exists()) {
                val excludes = excludes.get() + dryRunnerClassPath.get().toRegex()
                logger.log(logLevel, "exclude patterns $excludes")
                val classes = dumpLoadedClassList.readLines()
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

                BufferedWriter(dumpLoadedClassList.writer()).use { writer ->
                    filteredClasses.forEach { className ->
                        writer.write(className)
                        writer.newLine()
                    }
                }
            } else logger.error("filtering error: $dumpLoadedClassList doesn't exists")
        }
    }

}