package com.github.m4gshm.cds.gradle.util

import com.sun.org.apache.bcel.internal.classfile.ClassFormatException
import com.sun.org.apache.bcel.internal.classfile.ClassParser
import com.sun.org.apache.bcel.internal.generic.ArrayType
import com.sun.org.apache.bcel.internal.generic.ObjectType
import com.sun.org.apache.bcel.internal.generic.Type
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.InputStream
import java.io.Serializable
import java.util.jar.JarFile
import java.util.zip.ZipEntry

data class ClassListOptions(
    var superclass: Boolean = true,
    var interfaces: Boolean = true,
    var fields: Boolean = true,
    var onlyStaticFields: Boolean = false,
    var onlyFinalFields: Boolean = true,
    var methods: Boolean = true,
    var onlyStaticMethods: Boolean = true,
    var logSupportedClasses: Boolean = false,
    var logUnsupportedClasses: Boolean = false,
) : Serializable

class ClassVersionSupportInfoService(
    private val logger: Logger,
    private val logLevel: LogLevel,
    private val options: ClassListOptions
) {

    companion object {
        const val JAVA_MAGIC = 0xCAFEBABE.toInt()
    }

    fun getSupportInfo(jarFile: JarFile, jarEntry: ZipEntry) = getSupportInfo(
        jarEntry.name, jarFile.getInputStream(jarEntry)
    )

    data class ClassSupportInfo(
        val supported: Boolean,
        val classFilePath: String,
        val dependencies: Set<String>
    ) {
        fun handle(): Result {
            val unsupported = LinkedHashSet<String>()
            val supported = LinkedHashSet<String>()
            val unhandled = LinkedHashMap<String, LinkedHashSet<String>>()
            when {
                !this.supported -> unsupported.add(classFilePath)
                dependencies.isEmpty() -> supported.add(classFilePath)
                else -> dependencies.forEach { parent ->
                    unhandled.computeIfAbsent(parent) { LinkedHashSet() }.add(classFilePath)
                }
            }
            return Result(unsupported, supported, unhandled)
        }

        data class Result(
            val unsupported: Set<String>,
            val supported: Set<String>,
            val unhandled: Map<String, Set<String>>
        )
    }


    fun getSupportInfo(classFilePath: String, stream: InputStream): ClassSupportInfo {
        val classFilePathWithoutExtension = classFilePath.removeExtension()
        return try {
            val classInfo = parse(stream, classFilePath)
            val supported = classInfo.major > 49
            val superclassName = classInfo.superclassName
            val interfaces = classInfo.interfaceNames
            val fields = classInfo.fields.filter {
                (!options.onlyStaticFields || it.isStatic) && (!options.onlyFinalFields || it.isFinal)
            }.mapNotNull { field -> getClassName(field.type) }.toSet()
            val methods = classInfo.methods.filter {
                !options.onlyStaticMethods || it.isStatic
            }.flatMap { method ->
                listOf(method.returnType, *method.argumentTypes)
            }.toSet().mapNotNull { type -> getClassName(type) }

            val dependencies = when {
                supported -> hashSetOf<String>().apply {
                    if (options.superclass && superclassName != "java.lang.Object") add(superclassName)
                    if (options.interfaces) addAll(interfaces)
                    if (options.fields) addAll(fields)
                    if (options.methods) addAll(methods)
                }
                else -> emptySet()
            }.map { parentClassName -> parentClassName.replace(".", "/") }.toSet()

            ClassSupportInfo(supported, classFilePathWithoutExtension, dependencies)
        } catch (e: ClassFormatException) {
            logger.error("error on class parsing, $classFilePath, ${e.message}")
            ClassSupportInfo(true, classFilePathWithoutExtension, emptySet())
        }
    }

//    fun isSupportedClassFileVersion(
//        className: String, sourceFile: String, stream: DataInputStream
//    ) = if (stream.readInt() == JAVA_MAGIC) {
//        stream.readChar()
//        val majorVersion = stream.readChar().toInt()
//        if (majorVersion <= 49) {
//            logger.log(
//                logLevel,
//                "exclude unsupported class $className with majorVersion $majorVersion, source $sourceFile"
//            )
//            false
//        } else true
//    } else {
//        logger.log(logLevel, "exclude not java class file $className, source $sourceFile")
//        false
//    }


    private fun getClassName(type: Type): String? = when (type) {
        is ObjectType -> type.className
        is ArrayType -> getClassName(type.elementType)
        else -> null
    }

    private fun parse(stream: InputStream, sourceFile: String) = stream.use {
        ClassParser(it, sourceFile).parse()
    }

    private fun String.removeExtension(): String {
        val extensionIndex = length - indexOfLast { it == '.' }
        return if (length >= extensionIndex) substring(0, length - extensionIndex) else removeExtension()
    }
}


