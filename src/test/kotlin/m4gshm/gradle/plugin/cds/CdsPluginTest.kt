package m4gshm.gradle.plugin.cds


import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import kotlin.test.assertEquals


class CdsPluginTest {
    @Test
    fun testGenerateClassesListFileName() {
        val project = ProjectBuilder.builder().build()
        val pluginManager = project.pluginManager
        pluginManager.apply("m4gshm.gradle.plugin.cds")

        val generateClassesList = project.tasks.getByName("generateClassesList") as GenerateClassesList
        assertEquals("classes.txt", generateClassesList.outputFileName.get())
        val outputFile = generateClassesList.outputFile
        assertEquals("classes.txt", outputFile.get().asFile.name)
    }
}