package m4gshm.gradle.plugin.cds

import m4gshm.gradle.plugin.cds.CdsPlugin.Plugins.sharedClassesDump
import m4gshm.gradle.plugin.cds.CdsPlugin.Plugins.sharedClassesJar
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskAction


abstract class RunSharedClassesJar : JavaExec() {
    enum class Share {
        on, off, auto;

        val value = "-Xshare:$name"
    }

    @Internal
    var logLevel = LogLevel.DEBUG

    @Input
    val share: Property<Share> = objectFactory.property(Share::class.java).value(Share.on)

    private val sharedClassesDumpTask = project.tasks.getByName(sharedClassesDump.taskName) as SharedClassesDump
    private val sharedClassesJarTask = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar

    init {
        dependsOn(sharedClassesDumpTask)
        group = "application"
        classpath = project.files()
        mainClass.set("")
    }

    @TaskAction
    override fun exec() {
        val sharedClassesFile = sharedClassesDumpTask.outputFile.asFile.get()

        jvmArgs(
            share.get().value,
            "-XX:SharedArchiveFile=$sharedClassesFile",
            "-jar", sharedClassesJarTask.archiveFile.get().asFile.absolutePath
        )
        super.exec()
    }

}