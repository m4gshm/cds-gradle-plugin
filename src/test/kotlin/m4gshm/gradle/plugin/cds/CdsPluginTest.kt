package m4gshm.gradle.plugin.cds


import m4gshm.gradle.plugin.cds.CdsPlugin.Companion.classesListFileName
import m4gshm.gradle.plugin.cds.CdsPlugin.Companion.pluginId
import m4gshm.gradle.plugin.cds.CdsPlugin.Companion.sharedClassesFileName
import m4gshm.gradle.plugin.cds.CdsPlugin.Plugins.sharedClassesList
import m4gshm.gradle.plugin.cds.CdsPlugin.Plugins.sharedClassesDump
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import kotlin.test.assertEquals


class CdsPluginTest {
    @Test
    fun testGenerateClassesListFileName() {
        val task = project().tasks.getByName(sharedClassesList.name) as SharedClassesList
        assertEquals(classesListFileName, task.outputFileName.get())
        assertEquals(classesListFileName, task.outputFile.get().asFile.name)
    }

    @Test
    fun testSharedClassesFileName() {
        val task = project().tasks.getByName(sharedClassesDump.name) as SharedClassesDump
        assertEquals(sharedClassesFileName, task.outputFileName.get())
        assertEquals(sharedClassesFileName, task.outputFile.get().asFile.name)
    }

    private fun project(): Project {
        val project = ProjectBuilder.builder().build()
        val pluginManager = project.pluginManager
        pluginManager.apply(pluginId)
        return project
    }
}