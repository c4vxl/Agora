package de.c4vxl.enum

import de.c4vxl.bot.Bot
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

object Embeds {
    fun INSUFFICIENT_PERMS(bot: Bot): MessageEmbed =
        EmbedBuilder()
            .setTitle(bot.language.translate("global.title.failure"))
            .setDescription(bot.language.translate("global.error.insufficient_perms"))
            .addBlankField(false)
            .withTimestamp()
            .color(Color.DANGER)
            .build()

    fun SUCCESS(bot: Bot): EmbedBuilder =
        EmbedBuilder()
            .setTitle(bot.language.translate("global.title.success"))
            .withTimestamp()
            .color(Color.SUCCESS)

    fun FAILURE(bot: Bot): EmbedBuilder =
        EmbedBuilder()
            .setTitle(bot.language.translate("global.title.failure"))
            .withTimestamp()
            .color(Color.DANGER)
}