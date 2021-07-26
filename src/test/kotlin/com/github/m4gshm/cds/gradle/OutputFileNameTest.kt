package com.github.m4gshm.cds.gradle


import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.dumpLoadedClassListFileName
import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.pluginId
import com.github.m4gshm.cds.gradle.CdsPlugin.Companion.sharedClassesFileName
import com.github.m4gshm.cds.gradle.CdsPlugin.Tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import java.nio.file.Paths
import kotlin.test.assertEquals


class OutputFileNameTest {
    companion object {
        fun project(vararg plugins: Class<out Plugin<*>>): Project {
            val project = ProjectBuilder.builder().build()
            val pluginManager = project.pluginManager
            pluginManager.apply(JavaPlugin::class.java)
            pluginManager.apply(pluginId)
            project.extensions.getByType(CdsExtension::class.java).apply {
                logLevel = LogLevel.WARN
                mainClass = "com.github.m4gshm.cds.test.Main"
            }
            plugins.forEach { pluginManager.apply(it) }
            return project
        }
    }

    @Test
    fun testGenerateClassesListFileName() {
        val task = project().tasks.getByName(sharedClassesList.taskName) as SharedClassesList
        assertEquals(dumpLoadedClassListFileName, task.dumpLoadedClassList.get().asFile.name)
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
        assertEquals(sharedClassesFileName, task.sharedArchiveFile.get().asFile.get().name)
    }

}