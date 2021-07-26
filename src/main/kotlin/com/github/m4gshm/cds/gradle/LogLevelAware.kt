package com.github.m4gshm.cds.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

interface LogLevelAware {
    @get:Internal
    val logLevel: Property<LogLevel>
}