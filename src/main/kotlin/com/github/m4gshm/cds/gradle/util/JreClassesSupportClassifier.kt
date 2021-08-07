package com.github.m4gshm.cds.gradle.util

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import java.io.IOException
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems.getFileSystem
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.spi.FileSystemProvider
import java.util.Collections.*

class JreClassesSupportClassifier(
    logger: Logger, logLevel: LogLevel,
    private val classSupportInfoService: ClassSupportInfoService
) : BaseClassesSupportClassifier(logger, logLevel) {

    override fun extractSupportInfo() = extractJreClasses().map { path ->
        val url = path.toUri().toURL()
        val pathPrefix = "modules"
        val urlPrefix = "/$pathPrefix"
        var urlPath = url.path
        urlPath = if (urlPath.startsWith(urlPrefix)) urlPath.substring(urlPrefix.length, urlPath.length) else urlPath
        val fixedUrl = URL(url.protocol, null, urlPath)

        val removablePrefixCount = if (path.startsWith(pathPrefix)) 2 else 1
        val classFilePath = path.subpath(removablePrefixCount, path.nameCount).toString()
        try {
            classSupportInfoService.getSupportInfo(classFilePath, fixedUrl.openStream())
        } catch (e: IOException) {
            logger.error("error on class file reading $url", e)
            ClassSupportInfo(classFilePath, singleton(classFilePath), emptySet(), emptyMap())
        }
    }

    private fun extractJreClasses(): List<Path> {
        val fs = getFileSystem(URI.create("jrt:/"))
        return extract(fs.getPath("modules"), fs.provider())
    }

    private fun extract(p: Path, provider: FileSystemProvider): List<Path> = when {
        isDirectory(p, provider) -> provider.newDirectoryStream(p) { true }.flatMap { extract(it, provider) }
        p.fileName.toString().endsWith(".class") -> singletonList(p)
        else -> emptyList()
    }

    private fun isDirectory(p: Path, provider: FileSystemProvider) = provider.getFileAttributeView(
        p, BasicFileAttributeView::class.java, LinkOption.NOFOLLOW_LINKS
    ).readAttributes().isDirectory


}