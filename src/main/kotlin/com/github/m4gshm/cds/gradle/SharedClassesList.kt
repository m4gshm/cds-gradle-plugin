package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.dumpLoadedClassListFileName
import com.github.m4gshm.cds.gradle.CdsPlugin.Tasks.sharedClassesJar
import com.github.m4gshm.cds.gradle.util.BaseClassesSupportClassifier.ClassifierResult
import com.github.m4gshm.cds.gradle.util.ClassPathClassesSupportClassifier
import com.github.m4gshm.cds.gradle.util.ClassSupportInfoService
import com.github.m4gshm.cds.gradle.util.JreClassesSupportClassifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel.INFO
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
    val sort: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)

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
        val jarDependencies = getJarManifestClassPath(jar.get().asFile)

        val logLevel = logLevel.get()

        val dumpLoadedClassList = dumpLoadedClassList.get().asFile
        logger.log(logLevel, "output file $dumpLoadedClassList")

        val options = options.get()

        val classSupportInfoService = ClassSupportInfoService(logger, logLevel, options)
        logger.log(logLevel, "classes classificatory dependencies ${jarDependencies.asPath}")

        val (cpSupported, cpFound, cpUnsupported) = ClassPathClassesSupportClassifier(
            logger, logLevel, jarDependencies, classSupportInfoService
        ).classify()

        val includeJreClasses = options.includeJreClasses
        val (jreSupported, jreFound, jreUnsupported) = when {
            includeJreClasses -> JreClassesSupportClassifier(logger, logLevel, classSupportInfoService).classify()
            else -> ClassifierResult(emptySet(), emptySet(), emptySet())
        }

        val unsupported = jreUnsupported + cpUnsupported
        val found = jreFound + cpFound

        val supported = (jreSupported + cpSupported).let {
            if (options.supportedOnlyFound) it.filter { className -> found.contains(className) }.toSet()
            else it
        }

        logger.log(
            logLevel, "amounts of classified classes: supported ${supported.size}," +
                    " found ${found.size}, unsupported ${unsupported.size}"
        )

        if (options.logSupportedClasses) {
            mkBuildDir()
            buildDirectory.file("supported.txt").get().asFile.bufferedWriter().use { writer ->
                supported.sorted().forEach {
                    writer.write(it)
                    writer.newLine()
                }
            }
        }

        if (options.logUnsupportedClasses) {
            mkBuildDir()
            buildDirectory.file("unsupported.txt").get().asFile.bufferedWriter().use { writer ->
                unsupported.sorted().forEach {
                    writer.write(it)
                    writer.newLine()
                }
            }
        }

        if (staticList.get()) dumpLoadedClassList.bufferedWriter().use { writer ->
            (if (sort.get()) supported.sorted() else supported).forEach {
                writer.write(it)
                writer.newLine()
            }
        } else {
            unpackRunner()
            addRunnerArgs()

            jvmArgs(
                "-Xshare:off",
                "-XX:DumpLoadedClassList=$dumpLoadedClassList"
            )

            super.exec()

            if (dumpLoadedClassList.exists()) {
                val excludes = excludes.get() + dryRunnerClassPath.get().toRegex()
                logger.log(logLevel, "exclude patterns $excludes")
                val classes = LinkedHashSet(dumpLoadedClassList.readLines())
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
                filteredClasses = filteredClasses.filter { className ->
                    !unsupported.contains(className)
                }

                if (options.filterBySupported) filteredClasses = filteredClasses.filter { className ->
                    supported.contains(className)
                }

                logger.log(
                    if (logLevel > INFO) logLevel else INFO,
                    "class amount before support checking $beforeVersionFiltering and after ${filteredClasses.size}"
                )

                if (this.sort.get()) filteredClasses = filteredClasses.sorted()

                BufferedWriter(dumpLoadedClassList.writer()).use { writer ->
                    filteredClasses.forEach { className ->
                        writer.write(className)
                        writer.newLine()
                    }
                }
            } else logger.error("filtering error: $dumpLoadedClassList doesn't exists")
        }
    }

    private fun mkBuildDir() {
        val dir = buildDirectory.get().asFile
        if (!dir.exists()) dir.mkdirs()
    }

}