package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.classesListFileName
import com.github.m4gshm.cds.gradle.CdsPlugin.Plugins.sharedClassesJar
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.BufferedWriter


abstract class SharedClassesList : BaseGeneratingTask() {

    @get:Input
    var useSourceSetClassPath = true

    @get:Input
    var sourceSetName = "main"

    @get:Input
    val dryRunMainClass: Property<String> = objectFactory.property(String::class.java)
        .value(project.extensions.getByType(CdsExtension::class.java).mainClass)

    @get:Input
    var runnerJar = "dry-runner.jar"

    @get:Input
    var dryRunnerClass = "m4gshm.DryRunner"

    @get:Input
    val classesListExcludes: ListProperty<Regex> = objectFactory.listProperty(Regex::class.java).value(
        listOf(
            dryRunnerClass.replace(".", "/").toRegex(),
            ".+\\\$Proxy(\\d+)\$".toRegex()
        )
    )

    @Internal
    val outputFileName: Property<String> = objectFactory.property(String::class.java).value(classesListFileName)

    @get:OutputFile
    val outputFile: RegularFileProperty = objectFactory.fileProperty().value(buildDirectory.file(outputFileName))

    init {
        group = CdsPlugin.group
        isIgnoreExitValue = true
        mainClass.set(dryRunnerClass)
        val sharedClassesJar = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar
        dryRunMainClass.set(sharedClassesJar.mainClass)
        dependsOn(sharedClassesJar)
    }

    @TaskAction
    override fun exec() {
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
        }

        val classPathRunner = "/META-INF/$runnerJar"
        val resource =
            javaClass.getResource(classPathRunner) ?: throw IllegalStateException("$classPathRunner is absent")

        logger.log(logLevel, "runner url $resource")

        val outDir = this.buildDirectory.get()
        val outRunnerJar = outDir.file(runnerJar).asFile

        logger.log(logLevel, "copying runner to $outRunnerJar")
        resource.openStream().copyTo(outRunnerJar.outputStream())

        classpath += project.files(outRunnerJar)

        val dryRunMainClass = dryRunMainClass.get()
        logger.log(logLevel, "dry run main class $dryRunMainClass, runner class ${mainClass.get()}")

        val runnerArgs = listOf(dryRunMainClass) + (args ?: emptyList())
        args = runnerArgs
        val outputFile = outputFile.get().asFile
        logger.log(logLevel, "output file $outputFile")

        jvmArgs(
            "-Xshare:off",
            "-XX:DumpLoadedClassList=$outputFile"
        )

        super.exec()

        if (outputFile.exists()) {
            val excludes = classesListExcludes.get()
            val classes = outputFile.readLines()
            val filteredClasses = classes.filter { className ->
                val exclude = excludes.firstOrNull { excludeFilter -> excludeFilter.matches(className) } != null
                if (exclude) logger.log(logLevel, "exclude class $className")
                !exclude
            }

            logger.log(logLevel, "class amount before filtering ${classes.size} and after ${filteredClasses.size}")

            BufferedWriter(outputFile.writer()).use { writer ->
                filteredClasses.forEach { className ->
                    writer.write(className)
                    writer.newLine()
                }
            }
        } else logger.error("filtering error: $outputFile doesn't exists")

    }
}
