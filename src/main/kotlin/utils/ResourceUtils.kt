package de.c4vxl.utils

import okio.IOException
import java.io.File
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path

object ResourceUtils {
    /**
     * Reads the content of a resource file
     * @param path The path to the resource
     */
    fun readResource(path: String): String {
        val stream = ResourceUtils.javaClass.getResourceAsStream("/${path}") ?: return ""

        return try {
            stream.bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            ""
        }
    }

    /**
     * Copies a resource to a destination
     * @param path The path to the resource
     * @param dest The destination path
     * @param replace If set to true, files will be overwritten
     */
    fun saveResource(path: String, dest: String, replace: Boolean = true): Boolean {
        val file = File(dest)
        file.parentFile?.mkdirs()

        if (replace)
            Files.deleteIfExists(Path.of(dest))

        Files.copy(
            ResourceUtils.javaClass.getResourceAsStream("/${path}") ?: return false,
            file.toPath()
        )

        return true
    }

    /**
     * Downloads a file from an url
     * @param url The url to the file
     * @param target The target file
     */
    fun downloadFile(url: String, target: File) {
        target.parentFile.mkdirs()
        target.createNewFile()

        URL(url).openStream().use { input ->
            Channels.newChannel(input).use { rbc ->
                target.outputStream().channel.use { out ->
                    out.transferFrom(rbc, 0, Long.MAX_VALUE)
                }
            }
        }
    }
}