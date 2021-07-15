package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.buildDirName
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import java.io.File

abstract class SharedClassesJar : Jar() {
    @Internal
    var logLevel = LogLevel.DEBUG

    @Internal
    var baseJakTaskName = "jar"

    @Input
    var libsDirName = "lib"

    @Input
    var dependenciesConfigurationName = "runtimeClasspath"

    @Input
    val mainClass = project.objects.property(String::class.java)
        .value(project.extensions.getByType(CdsExtension::class.java).mainClass)

    init {
        dependsOn("assemble")
        group = CdsPlugin.group

        destinationDirectory.set(File("${project.buildDir}/$buildDirName/jar"))

        val baseJarTask: Jar = project.tasks.getByName(baseJakTaskName) as Jar
        with(baseJarTask)
        inputs.files(baseJarTask.inputs.files)
    }

    @TaskAction
    override fun copy() {
        val jarFile = archiveFile.get().asFile
        val jarDir = jarFile.parentFile

        val absoluteLibsDir = jarDir.toPath().resolve(libsDirName).toFile()
        absoluteLibsDir.mkdirs()
        val classpath = project.configurations.getByName(dependenciesConfigurationName).files
        classpath.forEach {
            val target = File(absoluteLibsDir, it.name)
            project.logger.log(logLevel, "copy $it -> $target")
            it.copyTo(target, true)
        }
        manifest.attributes(
            mapOf(
                "Main-Class" to mainClass.get(),
                "Class-Path" to classpath.joinToString(" ") { file -> libsDirName + "/" + file.name }
            )
        )
        super.copy()
    }

}