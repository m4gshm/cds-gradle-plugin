package com.github.m4gshm.cds.gradle

import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSetContainer
import java.io.File
import java.util.jar.JarFile

abstract class BaseDryRunnerTask : BaseGeneratingTask() {

    @get:Input
    abstract val dryRunMainClass: Property<String>

    @get:Input
    val dryRunnerClass: Property<String> = project.objects.property(String::class.java).convention(
        "m4gshm.DryRunner"
    )

    @get:Input
    var runnerJar = "dry-runner.jar"

    @get:Internal
    var sourceSetName = "main"

    @get:InputFile
    val jar: RegularFileProperty = objectFactory.fileProperty().convention(
        (project.tasks.getByName(CdsPlugin.Tasks.sharedClassesJar.taskName) as SharedClassesJar).archiveFile
    )

    @get:Internal
    var useSourceSetClassPath = false

    @get:Internal
    val runnerJarOutputFile: Provider<RegularFile> = buildDirectory.map { it.file(runnerJar) }

    init {
        group = CdsPlugin.group
        mainClass.convention(dryRunnerClass)
    }

    fun postInit() {
        initClasspath()
        addRunnerToClasspath()
    }

    private fun addRunnerToClasspath() {
        classpath += project.files(runnerJarOutputFile)
    }

    protected fun unpackRunner() {
        val classPathRunner = "/META-INF/$runnerJar"
        val resource = javaClass.getResource(classPathRunner)
            ?: throw IllegalStateException("$classPathRunner is absent")

        val logLevel = logLevel.get()
        logger.log(logLevel, "runner url $resource")

        val runnerJarOutputFile = runnerJarOutputFile.get().asFile
        val parentDir = runnerJarOutputFile.parentFile
        if (!parentDir.exists()) {
            val mkdirs = parentDir.mkdirs()
            if (!mkdirs && !parentDir.exists()) logger.error("cannot create directory $parentDir")
        }
        logger.log(logLevel, "copying runner to $runnerJarOutputFile")

        resource.openStream().copyTo(runnerJarOutputFile.outputStream())
    }

    protected fun addRunnerArgs() {
        val dryRunMainClass = dryRunMainClass.get()
        logger.log(logLevel.get(), "dry run main class $dryRunMainClass, runner class ${mainClass.get()}")

        args = listOf(dryRunMainClass) + (args ?: emptyList())
    }

    private fun initClasspath() = if (useSourceSetClassPath) {
        val sourceSets = project.extensions.findByName("sourceSets")
        if (sourceSets is SourceSetContainer) {
            val sourceSetName = this.sourceSetName
            val sourceSet = sourceSets.findByName(sourceSetName)
            if (sourceSet != null) {
                val runtimeClasspath = sourceSet.runtimeClasspath
                classpath = runtimeClasspath
            } else logger.warn("$sourceSetName sourceSet is absent")
        } else logger.warn("sourceSets is absent")
    } else classpath = project.files(jar)

    protected fun getJarManifestClassPath(jarFile: File): FileCollection = project.files(jarFile) + (JarFile(
        jarFile
    ).manifest.mainAttributes.getValue(
        "Class-Path"
    )?.let { jarClassPath ->
        val parentFile = jarFile.parentFile
        project.files(jarClassPath.split(" ").map { File(parentFile, it) })
    } ?: project.files())

}