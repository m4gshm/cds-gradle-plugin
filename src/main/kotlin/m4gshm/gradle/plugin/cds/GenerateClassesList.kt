package m4gshm.gradle.plugin.cds

import org.gradle.api.logging.LogLevel.DEBUG
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*


abstract class GenerateClassesList : JavaExec() {

    @Internal
    var logLevel = DEBUG

    @get:Input
    var useSourceSetClassPath = true
    @get:Input
    var sourceSetName = "main"
    @get:Input
    lateinit var dryRunMainClass: String
    @get:Input
    var runnerJar = "dry-runner.jar"

    @Internal
    val outputFileName: Property<String> = objectFactory.property(String::class.java).value("classes.txt")
    @Internal
    val outputDirName: Property<String> = objectFactory.property(String::class.java).value("cds")
    @Internal
    val outputDirectory = objectFactory.directoryProperty().value(project.layout.buildDirectory.dir(outputDirName))

    @get:OutputFile
    val outputFile = objectFactory.fileProperty().value(outputDirectory.file(outputFileName))

    init {
        group = "build"

        mainClass.set("m4gshm.DryRunner")

        val jarTask = project.tasks.findByPath("jar")
        val bootJarTask = project.tasks.findByPath("bootJar")
        if (bootJarTask != null) dependsOn(bootJarTask)
        else if (jarTask != null) dependsOn(jarTask)
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

        val outDir = this.outputDirectory.get()
        val outRunnerJar = outDir.file(runnerJar).asFile

        logger.log(logLevel, "copying runner to $outRunnerJar")
        resource.openStream().copyTo(outRunnerJar.outputStream())

        classpath += project.files(outRunnerJar)

        logger.log(logLevel, "dry run main class $dryRunMainClass, runner class ${mainClass.get()}")

        val runnerArgs = listOf(dryRunMainClass) + (args ?: emptyList())
        args = runnerArgs
        val outputFile = outputFile.get().asFile
        logger.log(logLevel, "output File $outputFile")

        jvmArgs(
            "-Xshare:off",
            "-XX:DumpLoadedClassList=$outputFile"
        )

        super.exec()
    }
}
