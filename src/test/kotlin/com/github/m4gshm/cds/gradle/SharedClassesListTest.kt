package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.OutputFileNameTest.Companion.project
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Objects.requireNonNull

class SharedClassesListTest {

    private fun getVersion() = System.getProperty("java.version").let {
        val dot = it.indexOf(".")
        if (dot != -1) it.substring(0, dot) else it
    }.toInt()

    @Test
    fun testGenerateClassesListFile() {
        val supportedVersion = 11
        val currentVersion = getVersion()
        assertTrue(
            "java version must be equals or great then $supportedVersion, current $currentVersion",
            currentVersion >= supportedVersion
        )
        val jarProperty = "cds.test.jar"
        val jarFilePath = requireNonNull(System.getProperty(jarProperty), "$jarProperty file is absent")

        val file = File(jarFilePath)
        assertTrue("file not exists, $file", file.exists())

        val task = project().tasks.getByName(CdsPlugin.Tasks.sharedClassesList.taskName) as SharedClassesList
        task.options.get().apply {
            includeJreClasses = true
            logSupportedClasses = true
            logUnsupportedClasses = true
        }
        task.jar.set(file)
        task.exec()

        val classListFile = task.dumpLoadedClassList.get().asFile
        assertTrue("class list file doesn't created, $classListFile", classListFile.exists())

        val expected = task.dryRunMainClass.get().replace(".", "/")
        val list = classListFile.readLines()
        val classes = list.toSet()
        assertTrue("cannot find $expected from $classes", classes.contains(expected))
        val notExpected = task.mainClass.get().replace(".", "/")
        assertFalse("$notExpected must not be in classes list $classes", classes.contains(notExpected))
    }

}