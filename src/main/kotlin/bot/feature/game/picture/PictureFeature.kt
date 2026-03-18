package de.c4vxl.bot.feature.game.picture

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.Feature
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.utils.EmbedUtils.color
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData

/**
 * Feature for fetching random pictures
 */
class PictureFeature(bot: Bot) : Feature<PictureFeature>(bot, PictureFeature::class.java) {
    init {
        registerCommands()

        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onButtonInteraction(event: ButtonInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return

                if (event.componentId.startsWith("${name}_delete_")) {
                    val allowed = event.componentId.endsWith(event.user.id) || event.componentId.endsWith("null")

                    if (!allowed) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.picture.embed.reply.delete_btn.error"))
                                .build()
                        ).setEphemeral(true).queue()
                        return
                    }

                    event.channel.deleteMessageById(event.messageId).queue()
                }
            }
        })
    }

    override fun registerCommands() {
        fun commandDesc(name: String) =
            bot.language.translate(
                "feature.picture.command.type.desc",
                bot.language.translate("feature.picture.type.$name").lowercase()
            )

        bot.commandHandler.registerSlashCommand(
            Commands.slash("picture", bot.language.translate("feature.picture.command.desc"))
                .addSubcommands(
                    SubcommandData("cat", commandDesc("cat"))
                        .addOption(OptionType.STRING, "query", bot.language.translate("feature.picture.command.type.query.desc")),
                    SubcommandData("dog", commandDesc("dog")),
                )
        ) { event ->
            val queries = event.getOption("query", OptionMapping::getAsString)
                ?.replace("; ", ";")
                ?.split(";")
                ?.toTypedArray()
                ?: arrayOf()

            when (event.subcommandName) {
                "cat" -> {
                    sendReply(
                        "cat",
                        PictureAPI.cat(*queries),
                        event
                    )
                }

                "dog" -> {
                    sendReply("dog", PictureAPI.dog(), event)
                }
            }
        }
    }

    /**
     * Sends an embed with the image
     * @param type The type/feature of image
     * @param image The actual image
     * @param event The command event to reply to
     * @param user The user that requested the image
     */
    private fun sendReply(type: String, image: FileUpload?, event: SlashCommandInteractionEvent, user: User? = null) {
        if (image == null) {
            event.replyEmbeds(Embeds.FAILURE(bot)
                .setDescription(bot.language.translate("feature.picture.embed.reply.fetch_error"))
                .build())
                .setEphemeral(true).queue()
            return
        }

        event.reply(
            MessageCreateData.fromEmbeds(
                EmbedBuilder()
                    .setTitle(bot.language.translate("feature.picture.type.$type"))
                    .setDescription(bot.language.translate(
                        "feature.picture.embed.reply.desc",
                        bot.language.translate("feature.picture.type.$type").lowercase()
                    ))
                    .setImage("attachment://${image.name}")
                    .color(Color.PRIMARY)
                    .build()
            )
        )
            .addComponents(ActionRow.of(
                Button.danger("${name}_delete_${user?.id}", bot.language.translate("feature.picture.embed.reply.delete_btn.label"))
            ))
            .addFiles(image)
            .queue()
    }
}