package com.github.m4gshm.cds.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.File
import java.net.URL
import java.nio.file.Paths
import java.util.jar.JarFile

class SupportedClassesByLogFilter(
    private val logger: Logger,
    private val logLevel: LogLevel,
    private val logFile: File,
    private val logParser: Regex = Regex("(?<tags>\\[.+\\]+) (?<class>.+) source: (?<source>.+)")
) {

    fun filterByLog(filteredClasses: List<String>): List<String> {
        val unsupportedByFileVersion = extractUnsupportedFilesFromLog()

        return filteredClasses.filter { className ->
            val unsupported = unsupportedByFileVersion.contains(className)
            if (unsupported) logger.log(logLevel, "exclude unsupported version class $className")
            !unsupported
        }
    }

    private fun extractUnsupportedFilesFromLog(): Set<String> {
        return mapClassNameToJarFile().mapKeys {
            it.key.replace(".", "/")
        }.filter { (className, jarFile) ->
            val classPath = "$className.class"
            val jarEntry = jarFile.getEntry(classPath)
            jarEntry != null && !SupportedClassFileVersionChecker(logger, logLevel).isSupportedClassFileVersion(
                className, jarFile, jarEntry
            )
        }.keys
    }

    private fun mapClassNameToJarFile() = logFile.readLines().mapNotNull { logLine ->
        val groupValues = logParser.find(logLine)?.groupValues ?: emptyList()
        if (groupValues.size > 3) {
            val className = groupValues[2]
            val source = groupValues[3]
            if (source.startsWith("file:")) {
                val file = Paths.get(URL(source).toURI()).toFile()
                if (file.extension == "jar") className to JarFile(file) else null
            } else null
        } else null
    }.toMap()

}