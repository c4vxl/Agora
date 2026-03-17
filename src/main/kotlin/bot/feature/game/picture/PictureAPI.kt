package de.c4vxl.bot.feature.game.picture

import net.dv8tion.jda.api.utils.FileUpload
import java.net.URL
import java.util.UUID

object PictureAPI {
    /**
     * Fetches a random cat picture
     */
    fun cat(): FileUpload {
        val url = URL("https://cataas.com/cat")

        url.openStream().use {
            return FileUpload.fromData(it.readAllBytes(), "${UUID.randomUUID()}.jpg")
        }
    }
}