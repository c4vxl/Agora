package de.c4vxl.bot.feature.channel

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Color
import de.c4vxl.enums.Embeds
import de.c4vxl.utils.ChannelUtils.getChannel
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.util.*

/**
 * This feature allows users to create their custom voice or text channels
 */
class ChannelFeature(bot: Bot) : Feature<ChannelFeature>(bot, ChannelFeature::class.java) {
    val handler: ChannelFeatureHandler = ChannelFeatureHandler(this)

    init {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("channel", bot.language.translate("feature.channel.command.desc"))
                .addSubcommands(
                    // /channel voice [name] [private]
                    SubcommandData("voice", bot.language.translate("feature.channel.command.voice.desc"))
                        .addOption(OptionType.STRING, "name", bot.language.translate("feature.channel.command._.name.desc"))
                        .addOption(OptionType.BOOLEAN, "private", bot.language.translate("feature.channel.command._.private.desc")),

                    // /channel text [name] [private]
                    SubcommandData("text", bot.language.translate("feature.channel.command.text.desc"))
                        .addOption(OptionType.STRING, "name", bot.language.translate("feature.channel.command._.name.desc"))
                        .addOption(OptionType.BOOLEAN, "private", bot.language.translate("feature.channel.command._.private.desc")),

                    // /channel delete [channel]
                    SubcommandData("delete", bot.language.translate("feature.channel.command.delete.desc"))
                        .addOption(OptionType.CHANNEL, "channel", bot.language.translate("feature.channel.command.delete.channel.desc"), true),

                    // /channel deleteall
                    SubcommandData("deleteall", bot.language.translate("feature.channel.command.deleteall.desc"))
                )
        ) { event ->
            when (event.subcommandName) {
                "delete" -> {
                    val channel = event.getOption("channel", OptionMapping::getAsChannel) ?: return@registerSlashCommand

                    // Return if user is not owner of the channel
                    if (!handler.isOwner(channel, event.user)) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.channel.command.error.not_owner"))
                                .build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // Remove channel
                    handler.remove(channel)

                    // Send success message
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.channel.command.delete.success", channel.name))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "deleteall" -> {
                    // Get channels
                    val channels: List<Channel> = handler.getChannels(event.user)
                        .mapNotNull { bot.guild.getChannel(it) }

                    val names = channels.map { it.name }

                    // Send list
                    event.reply(
                        MessageCreateBuilder()
                            .addEmbeds(EmbedBuilder()
                                .setTitle(bot.language.translate("feature.channel.command.deleteall.embed.title"))
                                .setDescription(bot.language.translate(
                                    if (names.isEmpty()) "feature.channel.command.deleteall.embed.empty"
                                    else "feature.channel.command.deleteall.embed.desc"
                                ))
                                .apply {
                                    if (names.isNotEmpty())
                                        this.addField("- " + names
                                            .joinToString("\n- ") {
                                                it.lowercase(Locale.getDefault()) },
                                            "", false)
                                }
                                .withTimestamp()
                                .color(Color.PRIMARY)
                                .build())
                            .apply {
                                if (names.isNotEmpty())
                                    this.addComponents(ActionRow.of(
                                        Button.danger(
                                            "${this@ChannelFeature.name}_deleteall_confirm",
                                            bot.language.translate("feature.channel.command.deleteall.embed.btn")
                                        )
                                    ))
                            }
                            .build()
                    ).setEphemeral(true).queue()
                }

                else -> handleCreate(event)
            }
        }

        // Register deleteall confirm button
        bot.buttonHandler.register("${this@ChannelFeature.name}_deleteall_confirm") {
            val channels = handler.getChannels(it.user).mapNotNull { x -> bot.guild.getChannel(x) }
            handleDeleteAll(channels, it)
        }
    }

    private fun handleCreate(event: SlashCommandInteractionEvent) {
        // Get channel configuration
        val type: String = event.subcommandName ?: return
        val name: String =
            event.getOption("name", OptionMapping::getAsString)
                ?: bot.language.translate("feature.channel.default_name.${type}", event.user.name)
        val private: Boolean = event.getOption("private", OptionMapping::getAsBoolean) ?: false

        val user = event.member ?: return

        // Create channel
        val channel = handler.createChannel(type, user, name, private)

        // Handle limit
        if (channel == null) {
            event.replyEmbeds(
                Embeds.FAILURE(bot)
                    .setDescription(bot.language.translate(
                        "feature.channel.command.error.limit",
                        type,
                        handler.maxChannelsPerUser(type).toString())
                    )
                    .build()
            ).setEphemeral(true).queue()
            return
        }

        // Handle success
        event.replyEmbeds(
            Embeds.SUCCESS(bot)
                .setDescription(bot.language.translate("feature.channel.command.success", type))
                .addField(
                    bot.language.translate("feature.channel.command.success.name"),
                    name,
                    false
                )
                .addField(
                    bot.language.translate("feature.channel.command.success.type"),
                    type,
                    false
                )
                .addField(
                    bot.language.translate("feature.channel.command.success.channel"),
                    channel.asMention,
                    false
                )
                .addField(
                    bot.language.translate("feature.channel.command.success.private"),
                    private.toString(),
                    false
                )
                .build()
        ).setEphemeral(true).queue()
    }

    private fun handleDeleteAll(channels: List<Channel>, event: ButtonInteractionEvent) {
        channels.forEach { c -> handler.remove(c) }

        event.replyEmbeds(
            Embeds.SUCCESS(bot)
                .setDescription(bot.language.translate("feature.channel.command.deleteall.success"))
                .build()
        ).setEphemeral(true).queue()
    }
}