package de.c4vxl.utils

import de.c4vxl.enum.Color
import net.dv8tion.jda.api.EmbedBuilder
import java.util.Date

object EmbedUtils {
    fun EmbedBuilder.withTimestamp() = this.setTimestamp(Date().toInstant())
    fun EmbedBuilder.color(color: Color) = this.setColor(color.asInt)
}