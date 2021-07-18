package com.github.m4gshm.cds.gradle

import com.github.m4gshm.cds.gradle.SharedClassesList.Companion.classFileEnd
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.File
import java.util.jar.JarFile

class SupportedClassesExtractor(
    private val dryRunnerClassPath: String,
    private val classpath: FileCollection,
    private val logger: Logger,
    private val logLevel: LogLevel,
    private val supportedClassFileVersionChecker: SupportedClassFileVersionChecker = SupportedClassFileVersionChecker(logger, logLevel)
) {

    fun extractSupportedClasses(): Set<String> {
        val classes = classpath.flatMap { classPathFile ->
            when {
                classPathFile.isDirectory -> extractDirClasses(classPathFile)
                classPathFile.extension == "jar" -> extractJarClasses(classPathFile)
                else -> {
                    logger.log(logLevel, "ignore file $classPathFile")
                    emptyList()
                }
            }
        }.map {
            val length = it.length
            val endLength = classFileEnd.length
            if (length >= endLength) it.substring(0, length - endLength)
            else ""
        }.filter { it.isNotBlank() }.toSet()
        logger.log(logLevel, "listed classes amount is ${classes.size}")
        return classes
    }

    private fun extractJarClasses(jar: File) = JarFile(jar).let { jarFile ->
        jarFile.entries().toList().filter { jarEntry ->
            val name = jarEntry.name
            !jarEntry.isDirectory && name.endsWith(classFileEnd) && name != dryRunnerClassPath + classFileEnd
                    && supportedClassFileVersionChecker.isSupportedClassFileVersion(name, jarFile, jarEntry)
        }.map { it.name }
    }

    private fun extractDirClasses(directory: File) = directory.walkTopDown().filter {
        it.extension == SharedClassesList.classFileExtension
    }.map {
        classPath(it.relativeTo(directory))
    }.asIterable().toList()

    private fun classPath(it: File) = when (File.separator) {
        "\\" -> it.path.replace(File.separator, "/")
        else -> it.path
    }

}