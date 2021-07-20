package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.util.ClassListOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel


class CdsPlugin : Plugin<Project> {

    companion object {
        const val classesListFileName = "classes.txt"
        const val sharedClassesFileName = "shared.jsa"
        const val buildDirName = "cds"
        const val group = "cds"
        const val extension = "cds"
        const val pluginId = "com.github.m4gshm.cds"
    }

    enum class Plugins(val pluginClass: Class<out Task>) {
        sharedClassesList(SharedClassesList::class.java),
        sharedClassesDump(SharedClassesDump::class.java),
        sharedClassesJar(SharedClassesJar::class.java),
        runSharedClassesJar(RunSharedClassesJar::class.java),
        sharedClassesDynamicDump(SharedClassesDynamicDump::class.java),
        ;

        val taskName = this.name
    }

    override fun apply(project: Project) {
        project.extensions.create(extension, CdsExtension::class.java).apply {
            logLevel = LogLevel.DEBUG
            mainClass
            staticClassList = false
            classListOptions = ClassListOptions()
        }

        Plugins.values().forEach {
            project.tasks.register(it.taskName, it.pluginClass)
        }
    }
}