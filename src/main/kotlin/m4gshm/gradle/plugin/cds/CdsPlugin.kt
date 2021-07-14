package m4gshm.gradle.plugin.cds

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task


class CdsPlugin : Plugin<Project> {

    companion object {
        const val classesListFileName = "classes.txt"
        const val sharedClassesFileName = "shared.jsa"
        const val buildDirName = "cds"
        const val group = "cds"
        const val pluginId = "m4gshm.gradle.plugin.cds"
    }

    enum class Plugins(val pluginClass: Class<out Task>) {
        sharedClassesList(SharedClassesList::class.java),
        sharedClassesDump(SharedClassesDump::class.java)
    }

    override fun apply(project: Project) {
        Plugins.values().forEach {
            project.tasks.register(it.name, it.pluginClass)
        }
    }
}