package m4gshm.gradle.plugin.cds

import org.gradle.api.Plugin
import org.gradle.api.Project

class CdsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("generateClassesList", GenerateClassesList::class.java)
    }
}