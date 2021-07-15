package com.github.m4gshm.cds.gradle

import org.gradle.api.provider.Property

interface CdsExtension {

    val mainClass: Property<String>

}