package com.github.m4gshm.cds.gradle.util

import com.github.m4gshm.cds.gradle.SharedClassesList
import com.github.m4gshm.cds.gradle.SharedClassesList.Companion.classFileEnd
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.File
import java.util.jar.JarFile

class ClassPathClassesSupportClassifier(
    logger: Logger,
    logLevel: LogLevel, private val classpath: FileCollection,
    private val classSupportInfoService: ClassSupportInfoService
) : BaseClassesSupportClassifier(logger, logLevel) {

    override fun extractSupportInfo() = classpath.map { file ->
        when {
            file.isDirectory -> getDirClassesInfo(file)
            file.extension == "jar" -> getJarInfo(file)
            else -> {
                logger.log(logLevel, "ignore file $file")
                emptyList()
            }
        }
    }.flatten()

    private fun getJarInfo(jar: File): List<ClassSupportInfo> = JarFile(jar).let { jarFile ->
        jarFile.entries().toList().filter { jarEntry ->
            val name = jarEntry.name
            !jarEntry.isDirectory && name.endsWith(classFileEnd)
        }.map { jarEntry -> classSupportInfoService.getSupportInfo(jarFile, jarEntry) }
    }

    private fun getDirClassesInfo(directory: File): List<ClassSupportInfo> = directory.walkTopDown().filter {
        it.extension == SharedClassesList.classFileExtension
    }.map { classFile ->
        classSupportInfoService.getSupportInfo(
            classFile.name,
            classFile.inputStream()
        )
    }.toList()

}