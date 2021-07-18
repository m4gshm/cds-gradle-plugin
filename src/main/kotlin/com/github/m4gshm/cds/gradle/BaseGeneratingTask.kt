package com.github.m4gshm.cds.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec

abstract class BaseGeneratingTask : JavaExec() {
    @Internal
    var logLevel = project.extensions.getByType(CdsExtension::class.java).logLevel

    @Internal
    val buildDirName: Property<String> = objectFactory.property(String::class.java).convention(CdsPlugin.buildDirName)

    @Internal
    val buildDirectory: DirectoryProperty = objectFactory.directoryProperty().convention(
        project.layout.buildDirectory.dir(buildDirName)
    )
}