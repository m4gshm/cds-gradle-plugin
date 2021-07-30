package com.github.m4gshm.cds.gradle.util

data class ClassSupportInfo(
    val classFilePath: String,
    val unsupported: Set<String>,
    val supported: Set<String>,
    val unhandled: Map<String, Set<String>>
) {
}