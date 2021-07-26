package com.github.m4gshm.cds.gradle

import org.gradle.api.logging.LogLevel

interface CdsExtension {

    var logLevel: LogLevel

    var mainClass: String

    var staticClassList: Boolean

    var dynamicDump: Boolean

    var classListOptions: ClassListOptions

}