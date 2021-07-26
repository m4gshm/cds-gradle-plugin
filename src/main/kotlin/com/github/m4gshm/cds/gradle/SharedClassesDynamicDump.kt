package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.sharedClassesFileName
import org.gradle.api.JavaVersion
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.jar.JarFile


abstract class SharedClassesDynamicDump : BaseDryRunnerTask(), SharedArchiveFileTaskSpec {

    @get:OutputFile
    override val sharedArchiveFile: RegularFileProperty = objectFactory.fileProperty().convention(
        buildDirectory.file(sharedClassesFileName)
    )

    @get:OutputFile
    val dumpLoadedClassList: RegularFileProperty = objectFactory.fileProperty().convention(
        buildDirectory.file(CdsPlugin.dumpLoadedClassListFileName)
    )

    @TaskAction
    override fun exec() {
        val minVersion = JavaVersion.VERSION_13
        val javaVersion = JavaVersion.current()
        val java13Compatible = javaVersion.isCompatibleWith(minVersion)
        if (!java13Compatible) logger.warn(
            "task is not compatible with java version $javaVersion, must be $minVersion or higher"
        )
        if (!dryRunMainClass.isPresent) dryRunMainClass.set(getDryRunMainClassFromJar())

        val outputFile = sharedArchiveFile.get().asFile
        val logLevel = logLevel.get()
        logger.log(logLevel, "output archive file $outputFile")

        val dumpLoadedClassList = dumpLoadedClassList.get().asFile
        logger.log(logLevel, "output class list file $dumpLoadedClassList")
        logger.log(logLevel, "classpath ${classpath.asPath}")
        jvmArgs(
            "-XX:ArchiveClassesAtExit=$outputFile",
            "-XX:DumpLoadedClassList=$dumpLoadedClassList"
        )
        unpackRunner()
        addRunnerArgs()
        super.exec()
    }

    private fun getDryRunMainClassFromJar(): Provider<String> = jar.flatMap {
        val file = it.asFile
        val jarMainClass = JarFile(file).manifest.mainAttributes.getValue("Main-Class")
        logger.log(logLevel.get(), "${file.name} Main-Class: $jarMainClass")
        if (jarMainClass is String) {
            logger.log(logLevel.get(), "init dryRunMainClass by $jarMainClass")
            objectFactory.property(String::class.java).value(jarMainClass)
        } else objectFactory.property(String::class.java)
    }

}
