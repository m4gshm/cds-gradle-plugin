package com.github.m4gshm.cds.gradle.util

import com.sun.org.apache.bcel.internal.classfile.ClassParser
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.InputStream
import java.util.jar.JarFile
import java.util.zip.ZipEntry

class ClassVersionSupportInfoService(
    private val logger: Logger,
    private val logLevel: LogLevel
) {

    companion object {
        const val JAVA_MAGIC = 0xCAFEBABE.toInt()
    }

    fun getSupportInfo(jarFile: JarFile, jarEntry: ZipEntry) = getSupportInfo(
        jarEntry.name, jarFile.getInputStream(jarEntry)
    )

    data class ClassSupportInfo(val supported: Boolean, val classFilePath: String, val parentClassNames: List<String>)

    fun getSupportInfo(classFilePath: String, stream: InputStream) = stream.use {
        val classInfo = parse(it, classFilePath)
        val supported = classInfo.major > 49
        val superclassName = classInfo.superclassName
        val interfaces = classInfo.interfaceNames
        val parentClassNames = (if (supported) when (superclassName) {
            "java.lang.Object" -> listOf(*interfaces)
            else -> listOf(superclassName, *interfaces)
        } else emptyList<String>()).map { parentClassName -> parentClassName.replace(".", "/") }

        ClassSupportInfo(supported, classFilePath.removeExtension(), parentClassNames)
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

    private fun parse(stream: InputStream, sourceFile: String) = ClassParser(stream, sourceFile).parse()

    private fun String.removeExtension(): String {
        val extensionIndex = length - indexOfLast { it == '.' }
        return if (length >= extensionIndex) substring(0, length - extensionIndex) else removeExtension()
    }
}


