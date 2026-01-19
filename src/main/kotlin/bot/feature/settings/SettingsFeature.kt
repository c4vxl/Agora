package de.c4vxl.bot.feature.settings

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.WelcomeFeature
import de.c4vxl.bot.feature.channel.ChannelFeature
import de.c4vxl.bot.feature.tickets.TicketFeature
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Color
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
        registerCommands()
    }

    override fun registerCommands() {
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
                        .addOption(OptionType.STRING, "saved_category", bot.language.translate("feature.settings.command.tickets.saved_category.desc")),

                    // Feature: Welcome
                    SubcommandData("welcome", bot.language.translate("feature.settings.command.welcome.desc"))
                        .addOption(OptionType.CHANNEL, "channel", bot.language.translate("feature.settings.command.welcome.channel.desc"))
                        .apply {
                            listOf("title", "image", "thumbnail", "description", "footer", "color").forEach {
                                this.addOption(OptionType.STRING, it, bot.language.translate(
                                    "feature.settings.command.welcome.${it}.desc",
                                    "\$user_name, \$user_ping, \$user_icon"
                                ))
                            }
                        }
                        .addOption(OptionType.BOOLEAN, "reset", bot.language.translate("feature.settings.command.welcome.reset.desc")),
                )
        ) { event ->
            when (event.subcommandName) {
                // Feature: Welcome
                "welcome" -> {
                    val channel = event.getOption("channel", OptionMapping::getAsChannel)
                    val title = event.getOption("title", OptionMapping::getAsString)
                    val image = event.getOption("image", OptionMapping::getAsString)
                    val thumbnail = event.getOption("thumbnail", OptionMapping::getAsString)
                    val description = event.getOption("description", OptionMapping::getAsString)
                    val footer = event.getOption("footer", OptionMapping::getAsString)
                    val color = event.getOption("color", OptionMapping::getAsString)?.removePrefix("#")?.toIntOrNull(16) ?: Color.PRIMARY.asInt
                    val reset = event.getOption("reset", OptionMapping::getAsBoolean) ?: false

                    if (reset) {
                        // Reset database
                        bot.dataHandler.delete<WelcomeFeature>()

                        event.replyEmbeds(
                            Embeds.SUCCESS(bot)
                                .setDescription(bot.language.translate("feature.settings.command.welcome.success.reset")).build()
                        ).setEphemeral(true).queue()

                        return@registerSlashCommand
                    }

                    // Exit if empty
                    if (mutableListOf(title, image, description, footer, thumbnail)
                            .filterNotNull().isEmpty()) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.settings.command.welcome.error.empty")).build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // Save to config
                    bot.dataHandler.set<WelcomeFeature>("color", color)
                    channel?.let { bot.dataHandler.set<WelcomeFeature>("channel", it.id) }
                    title?.let { bot.dataHandler.set<WelcomeFeature>("title", it) }
                    image?.let { bot.dataHandler.set<WelcomeFeature>("image", it) }
                    thumbnail?.let { bot.dataHandler.set<WelcomeFeature>("thumb", it) }
                    description?.let { bot.dataHandler.set<WelcomeFeature>("description", it) }
                    footer?.let { bot.dataHandler.set<WelcomeFeature>("footer", it) }
                }

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