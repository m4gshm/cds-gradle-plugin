package com.github.m4gshm.cds.gradle

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.io.File

abstract class BaseDryRunnerTask : BaseGeneratingTask() {

    @get:Input
    val dryRunMainClass: Property<String> = project.objects.property(String::class.java)
        .value(project.extensions.getByType(CdsExtension::class.java).mainClass)

    @get:Input
    var dryRunnerClass = "m4gshm.DryRunner"

    @get:Input
    var runnerJar = "dry-runner.jar"

    init {
        group = CdsPlugin.group
        isIgnoreExitValue = true
        mainClass.convention(dryRunnerClass)
    }

    protected open fun initRunner() {
        addRunnerToClasspath()
        initRunnerArgs()
    }

    protected open fun unpackRunner(): File {
        val classPathRunner = "/META-INF/$runnerJar"
        val resource =
            javaClass.getResource(classPathRunner) ?: throw IllegalStateException("$classPathRunner is absent")

        logger.log(logLevel, "runner url $resource")

        val outDir = this.buildDirectory.get()
        val outRunnerJar = outDir.file(runnerJar).asFile

        logger.log(logLevel, "copying runner to $outRunnerJar")
        resource.openStream().copyTo(outRunnerJar.outputStream())
        return outRunnerJar
    }

    protected open fun addRunnerToClasspath() {
        classpath += project.files(unpackRunner())
    }

    protected open fun initRunnerArgs() {
        val dryRunMainClass = dryRunMainClass.get()
        logger.log(logLevel, "dry run main class $dryRunMainClass, runner class ${mainClass.get()}")

        val runnerArgs = listOf(dryRunMainClass) + (args ?: emptyList())
        args = runnerArgs
    }

}