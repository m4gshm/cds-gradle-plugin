package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Plugins.sharedClassesDump
import com.github.m4gshm.cds.gradle.CdsPlugin.Plugins.sharedClassesJar
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*


abstract class RunSharedClassesJar : JavaExec() {
    enum class Share {
        on, off, auto;

        val value = "-Xshare:$name"
    }

    @Internal
    var logLevel = project.extensions.getByType(CdsExtension::class.java).logLevel

    @Input
    val share: Property<Share> = objectFactory.property(Share::class.java).convention(Share.on)

    private val sharedClassesDumpTask = project.tasks.getByName(sharedClassesDump.taskName) as SharedClassesDump

    @InputFile
    val sharedArchiveFile: RegularFileProperty = objectFactory.fileProperty().convention(
        sharedClassesDumpTask.sharedArchiveFile
    )

    private val sharedClassesJarTask = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar

    @InputFile
    val jar: RegularFileProperty = objectFactory.fileProperty().convention(sharedClassesJarTask.archiveFile)

    init {
        group = "application"
        classpath = project.files()
        mainClass.convention("")
    }

    @TaskAction
    override fun exec() {
        val sharedArchiveFile = sharedArchiveFile.get().asFile
        logger.log(logLevel, "shared archive file $sharedArchiveFile")
        jvmArgs(
            share.get().value,
            "-XX:SharedArchiveFile=$sharedArchiveFile",
            "-jar", jar.get().asFile.absolutePath
        )
        super.exec()
    }

}