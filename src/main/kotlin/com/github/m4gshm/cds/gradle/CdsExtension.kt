package com.github.m4gshm.cds.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property

interface CdsExtension {

    var logLevel: LogLevel

    var mainClass: Property<String>

    var staticClassesList: Property<Boolean>

}