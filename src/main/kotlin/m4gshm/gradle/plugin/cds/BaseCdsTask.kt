package m4gshm.gradle.plugin.cds

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.logging.LogLevel.DEBUG

abstract class BaseCdsTask : JavaExec() {
    @Internal
    var logLevel = DEBUG

    @Internal
    val buildDirName: Property<String> = objectFactory.property(String::class.java).value(CdsPlugin.buildDirName)

    @Internal
    val buildDirectory: DirectoryProperty = objectFactory.directoryProperty().value(project.layout.buildDirectory.dir(buildDirName))
}