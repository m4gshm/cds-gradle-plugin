package com.github.m4gshm.cds.gradle

import org.gradle.api.file.RegularFileProperty
import org.gradle.process.JavaExecSpec

interface SharedArchiveFileTaskSpec : JavaExecSpec {

    val sharedArchiveFile: RegularFileProperty

}