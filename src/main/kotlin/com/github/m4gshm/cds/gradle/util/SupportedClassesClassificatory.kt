package com.github.m4gshm.cds.gradle.util

import com.github.m4gshm.cds.gradle.SharedClassesList
import com.github.m4gshm.cds.gradle.SharedClassesList.Companion.classFileEnd
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.File
import java.util.jar.JarFile

class SupportedClassesClassificatory(
    private val classpath: FileCollection,
    private val logger: Logger,
    private val logLevel: LogLevel,
    private val classSupportInfoService: ClassSupportInfoService
) {

    fun classify(): Pair<Set<String>, Set<String>> {

        val unsupported = LinkedHashSet<String>()
        val supported = LinkedHashSet<String>()
        val found = LinkedHashSet<String>()
        val unhandled = LinkedHashMap<String, LinkedHashSet<String>>()

        classpath.map { file ->
            when {
                file.isDirectory -> getDirClassesInfo(file)
                file.extension == "jar" -> getJarInfo(file)
                else -> {
                    logger.log(logLevel, "ignore file $file")
                    emptyList()
                }
            }
        }.flatten().forEach {
            found.add(it.classFilePath)
            unsupported.addAll(it.unsupported)
            supported.addAll(it.supported)

            it.unhandled.forEach { (k, v) ->
                unhandled.computeIfAbsent(k) { LinkedHashSet() }.addAll(v)
            }
        }

        var onCheckUnsupported: Collection<String> = unsupported
        do {
            onCheckUnsupported = onCheckUnsupported.flatMap { unsupportedClass ->
                val usedBy = unhandled.remove(unsupportedClass) ?: emptyList()
                if (usedBy.isNotEmpty()) logger.log(logLevel, "unsupported $unsupportedClass is used by $usedBy")
                usedBy
            }
            unsupported.addAll(onCheckUnsupported)
        } while (!onCheckUnsupported.isEmpty())

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

        val foundSupported = supported.filter { found.contains(it) }.toSet()
        logger.log(logLevel, "amounts of classes: supported ${supported.size} (found ${foundSupported.size})," +
                " unsupported ${unsupported.size}")

        return foundSupported to unsupported
    }

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