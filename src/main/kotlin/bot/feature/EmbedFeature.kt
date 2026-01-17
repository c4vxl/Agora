package de.c4vxl.bot.feature

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Color
import de.c4vxl.enums.Embeds
import de.c4vxl.enums.Permission
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.util.*

/**
 * A Command allowing users to send custom embeds via a command
 */
class EmbedFeature(bot: Bot) : Feature<EmbedFeature>(bot, EmbedFeature::class.java) {
    init {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("embed", bot.language.translate("feature.embed.command.desc"))
                .addOption(OptionType.BOOLEAN, "timestamp", bot.language.translate("feature.embed.command.timestamp"))
                .apply {
                    listOf(
                        "description", "title", "title_url", "author", "author_url", "author_icon",
                        "footer", "footer_icon", "image", "thumbnail", "url", "fields", "color", "links"
                    ).forEach {
                        this.addOption(OptionType.STRING, it, bot.language.translate("feature.embed.command.${it}"))
                    }
                }

        ) { event ->
            if (event.member?.hasPermission(Permission.FEATURE_EMBED, bot) != true) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            // Read configuration
            val description: String? = event.getOption("description", OptionMapping::getAsString)
            val title: String? = event.getOption("title", OptionMapping::getAsString)
            val title_url: String? = event.getOption("title_url", OptionMapping::getAsString)
            val author: String? = event.getOption("author", OptionMapping::getAsString)
            val author_url: String? = event.getOption("author_url", OptionMapping::getAsString)
            val author_icon: String? = event.getOption("author_icon", OptionMapping::getAsString)
            val footer: String? = event.getOption("footer", OptionMapping::getAsString)
            val footer_icon: String? = event.getOption("footer_icon", OptionMapping::getAsString)
            val image: String? = event.getOption("image", OptionMapping::getAsString)
            val thumbnail: String? = event.getOption("thumbnail", OptionMapping::getAsString)
            val url: String? = event.getOption("url", OptionMapping::getAsString)
            val fields: String? = event.getOption("fields", OptionMapping::getAsString)
            val links: String? = event.getOption("links", OptionMapping::getAsString)
            val color: Int = event.getOption("color", OptionMapping::getAsString)?.removePrefix("#")?.toIntOrNull(16) ?: Color.PRIMARY.asInt
            val timestamp: Boolean = event.getOption("timestamp", OptionMapping::getAsBoolean) ?: false

            // Ensure that at least one value has been passed
            if (mutableListOf(description, title, title_url, author, author_url, author_icon,
                    footer, footer_icon, image, thumbnail, url, fields).filterNotNull().isEmpty()) {
                event.replyEmbeds(
                    Embeds.FAILURE(bot)
                        .setDescription(bot.language.translate("feature.embed.command.error.empty"))
                        .build()
                ).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            // Send embed
            event.channel.sendMessage(
                MessageCreateBuilder()

                    // Add embed
                    .addEmbeds(createEmbed(description, title, title_url, author, author_url, author_icon,
                        footer, footer_icon, image, thumbnail, url, fields, color, timestamp))

                    // Add link buttons
                    .addComponents((links ?: "").split("; ").chunked(5).map { chunk ->
                        ActionRow.of(
                            chunk.map {
                                var url = it.split("##").getOrNull(1) ?: "https://discord.com/"
                                if (!url.startsWith("http"))
                                    url = "http://${url}"

                                Button.link(
                                    url,
                                    it.split("##").getOrNull(0) ?: "Unset"
                                )
                            }
                        )})
                    .build()
            ).queue {
                // Send confirmation
                event.replyEmbeds(
                    Embeds.SUCCESS(bot)
                        .setDescription(bot.language.translate("feature.embed.command.success"))
                        .build()
                ).setEphemeral(true).queue()
            }
        }
    }

    private fun createEmbed(description: String?, title: String?, title_url: String?, author: String?, author_url: String?,
                            author_icon: String?, footer: String?, footer_icon: String?, image: String?, thumbnail: String?,
                            url: String?, fields: String?, color: Int, timestamp: Boolean) =
        EmbedBuilder()
            .setTitle(title, title_url)
            .setAuthor(author, author_url, author_icon)
            .setFooter(footer, footer_icon)
            .setTimestamp(if (timestamp) Date().toInstant() else null)
            .setDescription(description)
            .setColor(color)
            .setThumbnail(thumbnail)
            .setImage(image)
            .setUrl(url)
            .apply {
                fields?.split("; ")?.forEach {
                    val parts = it.split("##")
                    val inline = parts.getOrNull(0).contentEquals("y")
                    val key = parts.getOrNull(1)
                    val value = parts.getOrNull(2)

                    if (key == "<br>") this.addBlankField(inline)
                    else if (key != null && value != null) this.addField(key, value, inline)
                }
            }
            .build()
}