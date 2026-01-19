package de.c4vxl.bot.feature.channel

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Color
import de.c4vxl.enums.Embeds
import de.c4vxl.enums.Permission
import de.c4vxl.utils.ChannelUtils.getChannel
import de.c4vxl.utils.ChannelUtils.typeName
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.util.*

/**
 * This feature allows users to create their custom voice or text channels
 */
class ChannelFeature(bot: Bot) : Feature<ChannelFeature>(bot, ChannelFeature::class.java) {
    val handler: ChannelFeatureHandler = ChannelFeatureHandler(this)

    init {
        registerCommands()

        // Register deleteall confirm button
        bot.componentHandler.registerButton("${this@ChannelFeature.name}_deleteall_confirm") {
            val channels = handler.getChannels(it.user).mapNotNull { x -> bot.guild.getChannel(x) }
            handleDeleteAll(channels, it)
        }

        // Register buttons
        bot.componentHandler.registerButton("${this@ChannelFeature.name}_create_voice") {
            // Check for permission
            if (it.member?.hasPermission(Permission.FEATURE_CHANNELS_VOICE, bot) != true) {
                it.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerButton
            }

            it.replyModal(createModal("voice")).queue()
        }

        bot.componentHandler.registerButton("${this@ChannelFeature.name}_create_text") {
            // Check for permission
            if (it.member?.hasPermission(Permission.FEATURE_CHANNELS_TEXT, bot) != true) {
                it.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerButton
            }

            it.replyModal(createModal("text")).queue()
        }

        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
                if (event.guild.id != bot.guild.id) return

                // Delete voice channels after everyone left
                event.channelLeft?.let { channel ->
                    if (channel.members.size <= 0)
                        handler.remove(channel)
                }

                event.channelJoined?.let { channel ->
                    if (channel.id != bot.dataHandler.get<String>(name, "join_to_create_voice"))
                        return@let

                    // Create channel
                    val voice = handler.createChannel(
                        "voice",
                        event.member,
                        bot.language.translate("feature.channel.default_name.voice", event.member.user.name),
                        false
                    )

                    if (voice == null) {
                        event.guild.kickVoiceMember(event.member.user)
                            .queue()
                        return@let
                    }

                    // Move user to new voice channel
                    bot.guild.moveVoiceMember(event.member.user, voice as VoiceChannel)
                        .queue()
                }
            }

            override fun onModalInteraction(event: ModalInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return
                val prefix = "${this@ChannelFeature.name}_create_"
                if (!event.modalId.startsWith(prefix)) return

                // Get config
                val type = event.modalId.removePrefix(prefix)
                val name =
                    event.getValue("${prefix}name")?.asString
                        ?: bot.language.translate("feature.channel.default_name.${type}", event.user.name)

                // Create channel
                val channel = handler.createChannel(type, event.member ?: return, name, false)

                // Handle limit
                if (channel == null) {
                    event.replyEmbeds(
                        limitEmbed(type)
                    ).setEphemeral(true).queue()
                    return
                }

                // Send feedback
                event.replyEmbeds(
                    successEmbed(type, channel, false)
                ).setEphemeral(true).queue()
            }

            override fun onChannelDelete(event: ChannelDeleteEvent) {
                if (event.guild.id != bot.guild.id) return

                val type = event.channel.typeName ?: return

                // Return if channel is not from feature
                if (!handler.getChannels(type).contains(event.channel.id))
                    return

                // Log
                logger.warn("Detected external channel removal. Updating config...")

                // Remove
                handler.remove(event.channel)
            }
        })
    }

    override fun registerCommands() {
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
                    SubcommandData("deleteall", bot.language.translate("feature.channel.command.deleteall.desc")),

                    // /channel buttons
                    SubcommandData("buttons", bot.language.translate("feature.channel.command.buttons.desc"))
                        .addOption(OptionType.STRING, "voice", bot.language.translate("feature.channel.command.buttons.voice.desc"))
                        .addOption(OptionType.STRING, "text", bot.language.translate("feature.channel.command.buttons.text.desc"))
                )
        ) { event ->
            when (event.subcommandName) {
                "buttons" -> {
                    if (event.member?.hasPermission(Permission.FEATURE_CHANNELS_BUTTONS, bot) != true) {
                        event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    val voice = event.getOption("voice", OptionMapping::getAsString)
                    val text = event.getOption("text", OptionMapping::getAsString)

                    // Exit if no buttons should be created
                    if (voice == null && text == null) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.channel.command.buttons.error.none_created"))
                                .build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // Send buttons
                    event.channel.sendMessageComponents(
                        ActionRow.of(mutableListOf<ActionRowChildComponent?>().apply {
                            if (voice != null)
                                this.add(Button.primary("${this@ChannelFeature.name}_create_voice", voice))

                            if (text != null)
                                this.add(Button.primary("${this@ChannelFeature.name}_create_text", text))
                        })
                    ).queue()

                    // Send feedback
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.channel.command.buttons.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }

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
    }

    private fun createModal(type: String): Modal {
        return Modal.create(
            "${this@ChannelFeature.name}_create_${type}",
            bot.language.translate("feature.channel.command.modal.create_${type}")
        )
            .addComponents(
                Label.of(
                    bot.language.translate("feature.channel.command.modal.label.name"),
                    TextInput.create("${this@ChannelFeature.name}_create_name", TextInputStyle.SHORT)
                        .build()
                )
            )
            .build()
    }

    private fun handleCreate(event: SlashCommandInteractionEvent) {
        // Get channel configuration
        val type: String = event.subcommandName ?: return
        val name: String =
            event.getOption("name", OptionMapping::getAsString)
                ?: bot.language.translate("feature.channel.default_name.${type}", event.user.name)
        val private: Boolean = event.getOption("private", OptionMapping::getAsBoolean) ?: false

        val user = event.member ?: return

        // Check for permission
        if (!user.hasPermission(Permission.valueOf("FEATURE_CHANNELS_${type.uppercase(Locale.getDefault())}"), bot)) {
            event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
            return
        }

        // Create channel
        val channel = handler.createChannel(type, user, name, private)

        // Handle limit
        if (channel == null) {
            event.replyEmbeds(
                limitEmbed(type)
            ).setEphemeral(true).queue()
            return
        }

        // Handle success
        event.replyEmbeds(successEmbed(type, channel, private)).setEphemeral(true).queue()
    }

    private fun limitEmbed(type: String) =
        Embeds.FAILURE(bot)
            .setDescription(bot.language.translate(
                "feature.channel.command.error.limit",
                type,
                handler.maxChannelsPerUser(type).toString())
            )
            .build()

    private fun successEmbed(type: String, channel: Channel, private: Boolean) =
        Embeds.SUCCESS(bot)
            .setDescription(bot.language.translate("feature.channel.command.success", type))
            .addField(
                bot.language.translate("feature.channel.command.success.name"),
                channel.name,
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

    private fun handleDeleteAll(channels: List<Channel>, event: ButtonInteractionEvent) {
        channels.forEach { c -> handler.remove(c) }

        event.replyEmbeds(
            Embeds.SUCCESS(bot)
                .setDescription(bot.language.translate("feature.channel.command.deleteall.success"))
                .build()
        ).setEphemeral(true).queue()
    }
}