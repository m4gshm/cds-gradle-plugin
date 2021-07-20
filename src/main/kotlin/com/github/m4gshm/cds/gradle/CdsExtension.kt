package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.util.ClassListOptions
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property

interface CdsExtension {

    var logLevel: LogLevel

    var mainClass: String

    var staticClassList: Boolean

    var classListOptions: ClassListOptions

}