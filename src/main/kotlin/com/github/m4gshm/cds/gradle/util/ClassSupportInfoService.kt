package com.github.m4gshm.cds.gradle.util

import com.github.m4gshm.cds.gradle.ClassListOptions
import org.apache.bcel.classfile.ClassFormatException
import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.ConstantClass
import org.apache.bcel.generic.ArrayType
import org.apache.bcel.generic.ObjectType
import org.apache.bcel.generic.Type
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.InputStream
import java.util.jar.JarFile
import java.util.zip.ZipEntry

class ClassSupportInfoService(
    private val logger: Logger,
    private val logLevel: LogLevel,
    private val options: ClassListOptions
) {

    fun getSupportInfo(jarFile: JarFile, jarEntry: ZipEntry): ClassSupportInfo {
        val classFilePath = jarEntry.name
        return if (options.checkSigned && isSigned(jarFile, classFilePath)) {
            logger.log(logLevel, "unsupported signed class $classFilePath ${jarFile.name}")
            build(false, classFilePath, emptySet())
        } else getSupportInfo(
            classFilePath, jarFile.getInputStream(jarEntry)
        )
    }

    private fun isSigned(jarFile: JarFile, classFilePath: String) =
        (jarFile.manifest.entries[classFilePath] ?: emptyMap()).filterKeys { attr ->
            attr.toString().contains("-Digest")
        }.isNotEmpty()

    fun getSupportInfo(classFilePath: String, stream: InputStream): ClassSupportInfo {
        val classFilePathWithoutExtension = classFilePath.removeExtension()
        return try {
            val classInfo = parse(stream, classFilePath)
            val major = classInfo.major
            val supported = major > 49
            if (!supported) logger.log(logLevel, "class $classFilePath unsupported by version $major")
            val superclassName = classInfo.superclassName
            val interfaces = classInfo.interfaceNames

            val constantPoolClasses = if (options.analyzeConstantPool) classInfo.constantPool.let { constantPool ->
                (0 until constantPool.length).asSequence().map { constantPool.getConstant(it) }.mapNotNull {
                    (it as? ConstantClass)?.getConstantValue(constantPool)
                }.filterIsInstance<String>().filter { className ->
                    !(className == "java/lang/Object" || className == classFilePathWithoutExtension)
                }.toSet()
            } else emptySet()

            val fields = if (options.fields) classInfo.fields.filter {
                (!options.onlyStaticFields || it.isStatic) && (!options.onlyFinalFields || it.isFinal)

            }.mapNotNull { field -> getClassName(field.type) }.toSet() else emptySet()

            val methods = if (options.methods) classInfo.methods.filter {
                !options.onlyStaticMethods || it.isStatic
            }.flatMap { method ->
                listOf(method.returnType, *method.argumentTypes)
            }.toSet().mapNotNull { type -> getClassName(type) } else emptySet()

            val dependencies = when {
                supported -> hashSetOf<String>().apply {
                    if (options.superclass && superclassName != "java.lang.Object") add(superclassName)
                    if (options.interfaces) addAll(interfaces)
                    addAll(constantPoolClasses)
                    addAll(fields)
                    addAll(methods)
                }
                else -> emptySet()
            }.map { parentClassName -> parentClassName.replace(".", "/") }.toSet()

            build(supported, classFilePathWithoutExtension, dependencies)
        } catch (e: ClassFormatException) {
            logger.log(logLevel, "error on class parsing, $classFilePath, ${e.message}")
            build(true, classFilePathWithoutExtension, emptySet())
        }
    }

    private fun build(
        supported: Boolean, classFilePath: String, dependencies: Set<String>
    ): ClassSupportInfo {
        val unsupportedClasses = LinkedHashSet<String>()
        val supportedClasses = LinkedHashSet<String>()
        val unhandledClasses = LinkedHashMap<String, LinkedHashSet<String>>()
        if (!supported) unsupportedClasses.add(classFilePath)
        else if (dependencies.isEmpty()) supportedClasses.add(classFilePath)
        else dependencies.forEach { parent ->
            unhandledClasses.computeIfAbsent(parent) { LinkedHashSet() }.add(classFilePath)
        }
        return ClassSupportInfo(unsupportedClasses, supportedClasses, unhandledClasses)
    }

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


