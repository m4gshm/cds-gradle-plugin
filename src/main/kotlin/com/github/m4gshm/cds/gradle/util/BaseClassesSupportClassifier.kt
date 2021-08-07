package com.github.m4gshm.cds.gradle.util

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

abstract class BaseClassesSupportClassifier(
    protected val logger: Logger,
    protected val logLevel: LogLevel
) {

    data class ClassifierResult(
        val supported: Set<String>,
        val found: Set<String>,
        val unsupported: Set<String>
    )

    fun classify(): ClassifierResult {
        val unsupported = LinkedHashSet<String>()
        val supported = LinkedHashSet<String>()
        val found = LinkedHashSet<String>()
        val unhandled = LinkedHashMap<String, LinkedHashSet<String>>()

        extractSupportInfo().forEach {
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

//        val foundSupported = supported.filter { found.contains(it) }.toSet()
//        logger.log(
//            logLevel,
//            "amounts of classified classes by '$classifierType': supported ${supported.size} (found ${foundSupported.size})," +
//                    " unsupported ${unsupported.size}"
//        )

//        return foundSupported to unsupported
        return ClassifierResult(supported = supported, found = found, unsupported = unsupported)
    }

    protected abstract fun extractSupportInfo(): List<ClassSupportInfo>
}