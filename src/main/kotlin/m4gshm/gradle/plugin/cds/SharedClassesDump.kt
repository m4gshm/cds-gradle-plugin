package m4gshm.gradle.plugin.cds

import m4gshm.gradle.plugin.cds.CdsPlugin.Companion.classesListFileName
import m4gshm.gradle.plugin.cds.CdsPlugin.Companion.sharedClassesFileName
import m4gshm.gradle.plugin.cds.CdsPlugin.Plugins.sharedClassesJar
import m4gshm.gradle.plugin.cds.CdsPlugin.Plugins.sharedClassesList
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class SharedClassesDump : BaseGeneratingTask() {

    @Internal
    val inputFileName: Property<String> = objectFactory.property(String::class.java).value(classesListFileName)

    @get:OutputFile
    val inputFile: RegularFileProperty = objectFactory.fileProperty().value(buildDirectory.file(inputFileName))

    @Internal
    val outputFileName: Property<String> = objectFactory.property(String::class.java).value(sharedClassesFileName)

    @get:OutputFile
    val outputFile = objectFactory.fileProperty().value(buildDirectory.file(outputFileName))

    init {
        group = CdsPlugin.group
        dependsOn(sharedClassesList.taskName)
        mainClass.set("")

        val sharedClassesJar = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar
        val archiveFile = sharedClassesJar.archiveFile
        classpath = project.files(archiveFile)
        inputs.file(archiveFile)
    }

    @TaskAction
    override fun exec() {
        val inputFile = inputFile.get().asFile
        logger.log(logLevel, "input file $inputFile")

        val outputFile = outputFile.get().asFile
        logger.log(logLevel, "output file $outputFile")

        jvmArgs(
            "-Xshare:dump",
            "-XX:SharedClassListFile=$inputFile",
            "-XX:SharedArchiveFile=$outputFile"
        )

        super.exec()
    }
}
