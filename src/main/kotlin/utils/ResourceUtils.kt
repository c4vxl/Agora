package de.c4vxl.utils

import okio.IOException

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
}