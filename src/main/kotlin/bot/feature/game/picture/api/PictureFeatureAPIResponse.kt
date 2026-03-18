package de.c4vxl.bot.feature.game.picture.api

import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.FileUpload

data class PictureFeatureAPIResponse(
    val embed: MessageEmbed,
    val file: FileUpload? = null,
    val ephemeral: Boolean = false
)