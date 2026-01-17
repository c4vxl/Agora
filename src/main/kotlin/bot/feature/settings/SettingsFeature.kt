package de.c4vxl.bot.feature.settings

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.channel.ChannelFeature
import de.c4vxl.bot.feature.tickets.TicketFeature
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Embeds
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

class SettingsFeature(bot: Bot) : Feature<SettingsFeature>(bot, SettingsFeature::class.java) {
    init {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("settings", bot.language.translate("feature.settings.command.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(

                    // Feature: Channel
                    SubcommandData("channel", bot.language.translate("feature.settings.command.channel.desc"))
                        .addOption(OptionType.INTEGER, "max_voice_channels", bot.language.translate("feature.settings.command.channel.max_voice.desc"))
                        .addOption(OptionType.INTEGER, "max_text_channels", bot.language.translate("feature.settings.command.channel.max_text.desc"))
                        .addOption(OptionType.STRING, "voice_category", bot.language.translate("feature.settings.command.channel.voice_category.desc"))
                        .addOption(OptionType.STRING, "text_category", bot.language.translate("feature.settings.command.channel.text_category.desc"))
                        .addOption(OptionType.CHANNEL, "join_to_create_voice", bot.language.translate("feature.settings.command.channel.join_to_create_voice.desc")),

                    // Feature: Tickets
                    SubcommandData("ticket", bot.language.translate("feature.settings.command.tickets.desc"))
                        .addOption(OptionType.STRING, "open_category", bot.language.translate("feature.settings.command.tickets.open_category.desc"))
                        .addOption(OptionType.STRING, "saved_category", bot.language.translate("feature.settings.command.tickets.saved_category.desc"))

                )
        ) { event ->
            when (event.subcommandName) {
                // Feature: ticket
                "ticket" -> {
                    val openCategory = event.getOption("open_category", OptionMapping::getAsString)
                    val savedCategory = event.getOption("saved_category", OptionMapping::getAsString)

                    if (openCategory != null)
                        bot.dataHandler.set<TicketFeature>("open_category", openCategory)

                    if (savedCategory != null)
                        bot.dataHandler.set<TicketFeature>("saved_category", savedCategory)
                }

                // Feature: Channel
                "channel" -> {
                    val maxVoice = event.getOption("max_voice_channels", OptionMapping::getAsInt)
                    val maxText = event.getOption("max_text_channels", OptionMapping::getAsInt)
                    val voiceCategory = event.getOption("voice_category", OptionMapping::getAsString)
                    val textCategory = event.getOption("text_category", OptionMapping::getAsString)
                    val joinToCreate = event.getOption("join_to_create_voice", OptionMapping::getAsChannel)

                    if (joinToCreate != null) {
                        if (joinToCreate.type != ChannelType.VOICE) {
                            event.replyEmbeds(
                                Embeds.FAILURE(bot)
                                    .setDescription(bot.language.translate("feature.settings.command.error.no_voice"))
                                    .build()
                            ).setEphemeral(true).queue()
                            return@registerSlashCommand
                        }

                        bot.dataHandler.set<ChannelFeature>("join_to_create_voice", joinToCreate.id)
                    }

                    if (maxVoice != null)
                        bot.dataHandler.set<ChannelFeature>("max_voice", maxVoice)

                    if (maxText != null)
                        bot.dataHandler.set<ChannelFeature>("max_text", maxText)

                    if (voiceCategory != null)
                        bot.dataHandler.set<ChannelFeature>("voice_category", voiceCategory)

                    if (textCategory != null)
                        bot.dataHandler.set<ChannelFeature>("text_category", textCategory)
                }
            }

            // Send feedback
            event.replyEmbeds(
                Embeds.SUCCESS(bot)
                    .setDescription(bot.language.translate("feature.settings.command.success"))
                    .build()
            ).setEphemeral(true).queue()
        }
    }
}