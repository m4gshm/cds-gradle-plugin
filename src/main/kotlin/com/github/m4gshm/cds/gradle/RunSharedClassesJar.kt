package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.CdsPlugin.Tasks.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*


abstract class RunSharedClassesJar : JavaExec(), LogLevelAware {
    enum class Share {
        on, off, auto;

        val value = "-Xshare:$name"
    }

//    @Internal
//    val logClassLoading = true

    @get:Input
    abstract val dynamicDump: Property<Boolean>

    @Input
    val share: Property<Share> = objectFactory.property(Share::class.java).convention(Share.on)

    @Internal
    val sharedArchiveFileTask: Provider<SharedArchiveFileTaskSpec> = dynamicDump.map { dynamicDump ->
        if (dynamicDump) (project.tasks.getByName(sharedClassesDynamicDump.taskName) as SharedClassesDynamicDump)
        else (project.tasks.getByName(sharedClassesDump.taskName) as SharedClassesDump)
    }

    @InputFile
    val sharedArchiveFile: Provider<RegularFileProperty> = sharedArchiveFileTask.map {
        it.sharedArchiveFile
    }

    @InputFile
    val jar: RegularFileProperty = objectFactory.fileProperty().convention(
        (project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar).archiveFile
    )

    init {
        group = "application"

    }

    @TaskAction
    override fun exec() {
        val sharedArchiveFile = this.sharedArchiveFile.get().asFile.get()
        logger.log(logLevel.get(), "shared archive file $sharedArchiveFile")
        jvmArgs(share.get().value, "-XX:SharedArchiveFile=$sharedArchiveFile")
        if (dynamicDump.get()) {
            logger.log(logLevel.get(), "classpath ${classpath.asPath}")
            classpath = sharedArchiveFileTask.get().classpath
        } else {
            val jar = jar.get().asFile.absolutePath
            logger.log(logLevel.get(), "jar $jar")
            jvmArgs("-jar", jar)
        }
        super.exec()
    }

}