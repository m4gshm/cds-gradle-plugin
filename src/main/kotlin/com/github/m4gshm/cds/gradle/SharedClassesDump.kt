package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.sharedClassesFileName
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SharedClassesDump : BaseGeneratingTask(), SharedArchiveFileTaskSpec {

    private val sharedClassesList =
        project.tasks.getByName(CdsPlugin.Tasks.sharedClassesList.taskName) as SharedClassesList

    @get:InputFile
    val sharedClassListFile: RegularFileProperty =
        objectFactory.fileProperty().convention(sharedClassesList.dumpLoadedClassList)

    @get:InputFile
    val archiveFile: RegularFileProperty = objectFactory.fileProperty().convention(
        (project.tasks.getByName(CdsPlugin.Tasks.sharedClassesJar.taskName) as SharedClassesJar).archiveFile
    )

    @get:OutputFile
    override val sharedArchiveFile: RegularFileProperty = objectFactory.fileProperty().convention(
        buildDirectory.file(sharedClassesFileName)
    )

    init {
        group = CdsPlugin.group
        mainClass.convention("")
        classpath = project.files(archiveFile)
    }

    @TaskAction
    override fun exec() {

        val logLevel = logLevel.get()
        logger.log(logLevel, "classpath ${classpath.asPath}")
        val inputFile = sharedClassListFile.get()
        logger.log(logLevel, "input class list file $inputFile")

        val outputFile = sharedArchiveFile.get().asFile
        logger.log(logLevel, "output archive file $outputFile")

        jvmArgs(
            "-Xshare:dump",
            "-XX:SharedClassListFile=$inputFile",
            "-XX:SharedArchiveFile=$outputFile",
        )

        super.exec()
    }
}
