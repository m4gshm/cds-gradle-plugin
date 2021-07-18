package com.github.m4gshm.cds.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.DataInputStream
import java.util.jar.JarFile
import java.util.zip.ZipEntry

class SupportedClassFileVersionChecker(
    private val logger: Logger,
    private val logLevel: LogLevel
) {

    companion object {
        const val JAVA_MAGIC = 0xCAFEBABE.toInt()
    }

    fun isSupportedClassFileVersion(
        className: String, jarFile: JarFile, jarEntry: ZipEntry
    ) = DataInputStream(jarFile.getInputStream(jarEntry)).use { stream ->
        isSupportedClassFileVersion(className, jarFile.name, stream)
    }

    private fun isSupportedClassFileVersion(
        className: String, sourceFile: String, stream: DataInputStream
    ) = if (stream.readInt() == JAVA_MAGIC) {
        stream.readChar()
        val majorVersion = stream.readChar().toInt()
        val b = if (majorVersion <= 49) {
            logger.log(
                logLevel,
                "exclude unsupported class $className with majorVersion $majorVersion, source $sourceFile"
            )
            false
        } else {
            true
        }
        b
    } else {
        logger.log(logLevel, "exclude not java class file $className, source $sourceFile")
        false
    }
}