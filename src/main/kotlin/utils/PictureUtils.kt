package de.c4vxl.utils

import net.dv8tion.jda.api.utils.FileUpload
import java.net.URL

/**
 * A collection of picture utilities
 */
object PictureUtils {
    /**
     * Fetches an image and returns it as a FileUpload
     * @param url The url of the image
     */
    fun fetchImage(url: String) =
        URL(url).openStream().use {
            FileUpload.fromData(it.readAllBytes(), "picture.jpg")
        }
}