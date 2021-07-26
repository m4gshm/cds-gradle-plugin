package com.github.m4gshm.cds.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel


class CdsPlugin : Plugin<Project> {

    companion object {
        const val dumpLoadedClassListFileName = "classes.txt"
        const val sharedClassesFileName = "shared.jsa"
        const val buildDirName = "cds"
        const val group = "cds"
        const val extension = "cds"
        const val pluginId = "com.github.m4gshm.cds"
    }

    enum class Tasks(val taskClass: Class<out Task>) {
        sharedClassesList(SharedClassesList::class.java) {
            override fun config(cds: CdsExtension, task: Task) {
                super.config(cds, task)
                if (task is SharedClassesList) {
                    task.staticList.set(cds.staticClassList)
                    task.options.set(cds.classListOptions)
                }
            }
        },
        sharedClassesDump(SharedClassesDump::class.java) {
            override fun config(cds: CdsExtension, task: Task) {
                super.config(cds, task)
            }
        },
        sharedClassesJar(SharedClassesJar::class.java) {
            override fun config(cds: CdsExtension, task: Task) {
                super.config(cds, task)
                if (task is SharedClassesJar) {
                    task.mainClass.set(cds.mainClass)
                }
            }
        },
        runSharedClassesJar(RunSharedClassesJar::class.java) {
            override fun config(cds: CdsExtension, task: Task) {
                super.config(cds, task)
                if (task is RunSharedClassesJar) {
                    task.mainClass.set(cds.mainClass)
                    task.dynamicDump.set(cds.dynamicDump)
                }
            }
        },
        sharedClassesDynamicDump(SharedClassesDynamicDump::class.java),
        ;

        val taskName = this.name

        open fun config(cds: CdsExtension, task: Task) {
            if (task is LogLevelAware) task.apply {
                logLevel.set(cds.logLevel)
            }
            if (task is BaseDryRunnerTask) task.apply {
                dryRunMainClass.set(cds.mainClass)
                postInit()
            }
        }
    }

    override fun apply(project: Project) {
        val cdsExtension = project.extensions.create(extension, CdsExtension::class.java).apply {
            logLevel = LogLevel.DEBUG
            staticClassList = false
            dynamicDump = true
            classListOptions = ClassListOptions()
        }

        val tasks = Tasks.values().map { it to project.tasks.register(it.taskName, it.taskClass) }

        //configuring tasks only after all have been registered
        tasks.forEach { (taskDesc, provider) ->
            provider.configure { task ->
                taskDesc.config(cdsExtension, task)
            }
        }
    }
}