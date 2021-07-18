package com.github.m4gshm.cds.gradle


import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.classesListFileName
import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.pluginId
import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.sharedClassesFileName
import com.github.m4gshm.cds.gradle.CdsPlugin.Plugins.*
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
        assertEquals(classesListFileName, task.outputFile.get().asFile.name)
    }

    @Test
    fun testSharedClassesFileName() {
        val task = project().tasks.getByName(sharedClassesDump.taskName) as SharedClassesDump
        assertEquals(sharedClassesFileName, task.sharedArchiveFile.get().asFile.name)
    }

    @Test
    fun sharedClassesJarName() {
        val project = project()

        val task = project.tasks.getByName(sharedClassesJar.taskName) as SharedClassesJar
        val archiveFile = task.archiveFile.get().asFile.toPath()
        val buildDir = project.buildDir.toPath()
        val archivePath = buildDir.relativize(archiveFile)
        val nameCount = archivePath.nameCount
        assertEquals(3, nameCount)
        val archiveDir = archivePath.subpath(0, 2)
        assertEquals(Paths.get(CdsPlugin.buildDirName, "jar"), archiveDir)
    }

    @Test
    fun testRunSharedClassesJarFileName() {
        val task = project().tasks.getByName(runSharedClassesJar.taskName) as RunSharedClassesJar
        assertEquals(sharedClassesFileName, task.sharedArchiveFile.get().asFile.name)
    }


//    @Test
//    fun sharedClassesDynamicDumpMainClassName() {
//        val project = project()
//
//        val task = project.tasks.getByName(sharedClassesDynamicDump.taskName) as SharedClassesDynamicDump
//        val jar = project.tasks.getByName("jar") as Jar
//        jar.manifest.attributes["Main-Class"] = "app.Main"
//        task.jar.set(jar.archiveFile.get().asFile)
//        val dryRunMainClass = task.dryRunMainClass
//        val dryRunMainClassName = dryRunMainClass.get()
//        assertEquals("app.Main", dryRunMainClassName)
//    }

    private fun project(vararg plugins: Class<out Plugin<*>>): Project {
        val project = ProjectBuilder.builder().build()
        val pluginManager = project.pluginManager
        pluginManager.apply(JavaPlugin::class.java)
        pluginManager.apply(pluginId)
        plugins.forEach { pluginManager.apply(it) }
        return project
    }
}