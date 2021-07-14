package m4gshm.gradle.plugin.cds


import m4gshm.gradle.plugin.cds.CdsPlugin.Companion.classesListFileName
import m4gshm.gradle.plugin.cds.CdsPlugin.Companion.pluginId
import m4gshm.gradle.plugin.cds.CdsPlugin.Companion.sharedClassesFileName
import m4gshm.gradle.plugin.cds.CdsPlugin.Plugins.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.nio.file.Paths
import kotlin.test.assertEquals


class CdsPluginTest {
    @Test
    fun testGenerateClassesListFileName() {
        val task = project().tasks.getByName(sharedClassesList.taskName) as SharedClassesList
        assertEquals(classesListFileName, task.outputFileName.get())
        assertEquals(classesListFileName, task.outputFile.get().asFile.name)
    }

    @Test
    fun testSharedClassesFileName() {
        val task = project().tasks.getByName(sharedClassesDump.taskName) as SharedClassesDump
        assertEquals(sharedClassesFileName, task.outputFileName.get())
        assertEquals(sharedClassesFileName, task.outputFile.get().asFile.name)
    }

    @Test
    fun sharedClassesJarName() {
        val project = project(JavaPlugin::class.java)

        val task = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar
        val archiveFile = task.archiveFile.get().asFile.toPath()
        val buildDir = project.buildDir.toPath()
        val archivePath = buildDir.relativize(archiveFile)
        val nameCount = archivePath.nameCount
        assertEquals(3, nameCount)
        val archiveDir = archivePath.subpath(0, 2)
        assertEquals(Paths.get(CdsPlugin.buildDirName, "jar"), archiveDir)
    }

    private fun project(vararg plugins: Class<out Plugin<*>>): Project {
        val project = ProjectBuilder.builder().build()
        val pluginManager = project.pluginManager
        pluginManager.apply(pluginId)
        plugins.forEach { pluginManager.apply(it) }
        return project
    }
}