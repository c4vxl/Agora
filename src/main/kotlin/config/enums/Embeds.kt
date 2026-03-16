package de.c4vxl.config.enums

import de.c4vxl.bot.Bot
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.MessageEmbed

object Embeds {
    fun INSUFFICIENT_PERMS(bot: Bot): MessageEmbed =
        EmbedBuilder()
            .setTitle(bot.language.translate("global.title.failure"))
            .setDescription(bot.language.translate("global.error.insufficient_perms"))
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

    fun DM_ACTION_ROW(bot: Bot): ActionRow =
        ActionRow.of(
            Button.danger(
                "agora_dm_delete_all",
                bot.language.translate("global.button.dm_delete_all")
            ),
            Button.primary(
                "agora_delete_message",
                bot.language.translate("global.button.delete_msg")
            )
        )

}