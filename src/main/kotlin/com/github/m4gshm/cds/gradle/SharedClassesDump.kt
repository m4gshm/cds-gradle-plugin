package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.sharedClassesFileName
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SharedClassesDump : BaseGeneratingTask() {

    private val sharedClassesList =
        project.tasks.getByName(CdsPlugin.Plugins.sharedClassesList.taskName) as SharedClassesList

    @get:InputFile
    val sharedClassListFile: RegularFileProperty = objectFactory.fileProperty().convention(sharedClassesList.outputFile)

    private val sharedClassesJar =
        project.tasks.getByName(CdsPlugin.Plugins.sharedClassesJar.taskName) as SharedClassesJar

    @get:InputFile
    val archiveFile: RegularFileProperty = objectFactory.fileProperty().convention(sharedClassesJar.archiveFile)

    @get:OutputFile
    val sharedArchiveFile: RegularFileProperty = objectFactory.fileProperty().convention(
        buildDirectory.file(sharedClassesFileName)
    )

    init {
        group = CdsPlugin.group
        mainClass.convention("")
        classpath = project.files(archiveFile)
    }

    @TaskAction
    override fun exec() {

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
