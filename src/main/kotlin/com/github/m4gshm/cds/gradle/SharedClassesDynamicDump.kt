package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.sharedClassesFileName
import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.jar.JarFile


abstract class SharedClassesDynamicDump : BaseDryRunnerTask() {

    @get:InputFile
    val jar: RegularFileProperty = objectFactory.fileProperty()

    @get:OutputFile
    val archiveFile: RegularFileProperty = objectFactory.fileProperty()
        .convention(buildDirectory.file(sharedClassesFileName))

    @TaskAction
    override fun exec() {
        val minVersion = JavaVersion.VERSION_13
        val javaVersion = JavaVersion.current()
        val java13Compatible = javaVersion.isCompatibleWith(minVersion)
        if (!java13Compatible) logger.warn("task is not compatible with java version $javaVersion, must be $minVersion or higher")
        if (!dryRunMainClass.isPresent) dryRunMainClass.set(jar.flatMap {
            val file = it.asFile
            val jarMainClass = JarFile(file).manifest.mainAttributes.getValue("Main-Class")
            logger.log(logLevel, "${file.name} Main-Class: $jarMainClass")
            if (jarMainClass is String) {
                logger.log(logLevel, "init dryRunMainClass by $jarMainClass")
                objectFactory.property(String::class.java).value(jarMainClass)
            } else objectFactory.property(String::class.java)
        })
        initRunner()
        classpath += project.files(jar.get())

        val outputFile = archiveFile.get().asFile
        logger.log(logLevel, "output archive file $outputFile")

        jvmArgs(
            "-XX:ArchiveClassesAtExit=$outputFile"
        )

        super.exec()
    }

}
