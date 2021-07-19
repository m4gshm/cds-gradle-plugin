package com.github.m4gshm.cds.gradle.util

import com.github.m4gshm.cds.gradle.SharedClassesList
import com.github.m4gshm.cds.gradle.SharedClassesList.Companion.classFileEnd
import com.github.m4gshm.cds.gradle.util.ClassVersionSupportInfoService.ClassSupportInfo
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.File
import java.util.jar.JarFile

class SupportedClassesClassificatory(
    private val dryRunnerClassPath: String,
    private val classpath: FileCollection,
    private val logger: Logger,
    private val logLevel: LogLevel,
    private val classVersionSupportInfoService: ClassVersionSupportInfoService = ClassVersionSupportInfoService(
        logger,
        logLevel
    )
) {

    fun classify(): Pair<Set<String>, Set<String>> {

        val unsupported = LinkedHashSet<String>()
        val unhandled = LinkedHashMap<String, LinkedHashSet<String>>()

        classpath.map { file ->
            when {
                file.isDirectory -> initByDirClasses(file)
                file.extension == "jar" -> initByJar(file)
                else -> {
                    logger.log(logLevel, "ignore file $file")
                    emptyList()
                }
            }
        }.flatten().forEach { it.handle(unsupported, unhandled) }

        var onCheckUnsupported: Collection<String> = unsupported
        do {
            onCheckUnsupported = onCheckUnsupported.flatMap { unsupportedClass ->
                unhandled.remove(unsupportedClass) ?: emptyList()
            }
            unsupported.addAll(onCheckUnsupported)
        } while (!onCheckUnsupported.isEmpty())

        val supported = LinkedHashSet<String>()
        val supportedIt = unhandled.iterator()
        while (supportedIt.hasNext()) {
            val (className, children) = supportedIt.next()
            supported.add(className)
            children.forEach { child ->
                if (!unsupported.contains(child)) supported.add(child)
            }
            supportedIt.remove()
        }

        assert(unhandled.isEmpty()) { "unhandled thees is not empty $unhandled" }

        logger.log(logLevel, "amounts of classes: supported ${supported.size}, unsupported ${unsupported.size}")
        return supported to unsupported
    }

    private fun initByJar(jar: File): List<ClassSupportInfo> = JarFile(jar).let { jarFile ->
        jarFile.entries().toList().filter { jarEntry ->
            val name = jarEntry.name
            !jarEntry.isDirectory && name.endsWith(classFileEnd) && name != dryRunnerClassPath + classFileEnd
        }.map { jarEntry -> classVersionSupportInfoService.getSupportInfo(jarFile, jarEntry) }
    }

    private fun ClassSupportInfo.handle(
        unsupported: LinkedHashSet<String>,
        unhandled: LinkedHashMap<String, LinkedHashSet<String>>
    ) {
        when {
            !supported -> unsupported.add(classFilePath)
            else -> dependencies.forEach { parent ->
                unhandled.computeIfAbsent(parent) { LinkedHashSet() }.add(classFilePath)
            }
        }
    }

    private fun initByDirClasses(directory: File): List<ClassSupportInfo> = directory.walkTopDown().filter {
        it.extension == SharedClassesList.classFileExtension
    }.map { classFile ->
        classVersionSupportInfoService.getSupportInfo(
            classFile.name,
            classFile.inputStream()
        )
    }.toList()

}