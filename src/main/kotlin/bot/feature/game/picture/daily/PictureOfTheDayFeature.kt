package de.c4vxl.bot.feature.game.picture.daily

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.Feature
import de.c4vxl.bot.feature.game.picture.PictureFeature
import de.c4vxl.config.enums.Embeds
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

/**
 * A feature sending a random "picture of the day" every day at a fixed time into a selected channel
 */
class PictureOfTheDayFeature(bot: Bot) : Feature<PictureOfTheDayFeature>(bot, PictureOfTheDayFeature::class.java) {
    val handler = PictureOfTheDayFeatureHandler(this)

    init {
        registerCommands()

        handler.reloadSchedule()
    }

    override fun registerCommands() {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("picture-of-the-day", bot.language.translate("feature.picture_of_the_day.command.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR))
                .addSubcommands(
                    SubcommandData("trigger", bot.language.translate("feature.picture_of_the_day.command.trigger.desc")),

                    SubcommandData("set-category", bot.language.translate("feature.picture_of_the_day.command.set-category.desc"))
                        .addOption(OptionType.STRING, "category", bot.language.translate("feature.picture_of_the_day.command.set-category.category.desc"), true),

                    SubcommandData("set-channel", bot.language.translate("feature.picture_of_the_day.command.set-channel.desc"))
                        .addOption(OptionType.CHANNEL, "channel", bot.language.translate("feature.picture_of_the_day.command.set-channel.desc"), true),

                    SubcommandData("set-time", bot.language.translate("feature.picture_of_the_day.command.set-time.desc"))
                        .addOption(OptionType.STRING, "time", bot.language.translate("feature.picture_of_the_day.command.set-time.desc"), true),
                )
        ) { event ->
            when (event.subcommandName) {
                "trigger" -> {
                    // Trigger
                    val success = handler.send()

                    if (!success) event.replyEmbeds(
                        Embeds.FAILURE(bot)
                            .setDescription(bot.language.translate("feature.picture_of_the_day.command.trigger.failure"))
                            .build()
                    ).setEphemeral(true).queue()
                    else
                        event.replyEmbeds(
                            Embeds.SUCCESS(bot)
                                .setDescription(bot.language.translate("feature.picture_of_the_day.command.trigger.success"))
                                .build()
                        ).setEphemeral(true).queue()
                }

                "set-category" -> {
                    val category = event.getOption("category", OptionMapping::getAsString) ?: return@registerSlashCommand

                    // Set config
                    bot.dataHandler.set<PictureOfTheDayFeature>("category", category)

                    // Send feedback
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot).setDescription(bot.language.translate("feature.picture_of_the_day.command.set-category.success")).build()
                    ).setEphemeral(true).queue()
                }

                "set-channel" -> {
                    val channel = event.getOption("channel", OptionMapping::getAsChannel) ?: return@registerSlashCommand

                    // Set config
                    bot.dataHandler.set<PictureOfTheDayFeature>("channel", channel.id)

                    // Send feedback
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot).setDescription(bot.language.translate("feature.picture_of_the_day.command.set-channel.success")).build()
                    ).setEphemeral(true).queue()
                }

                "set-time" -> {
                    val time = event.getOption("time", OptionMapping::getAsString) ?: return@registerSlashCommand

                    // If set to none -> disable feature
                    if (time.lowercase().contains("none")) {
                        bot.dataHandler.set<PictureOfTheDayFeature>("time", null)
                    }

                    // Parse time string
                    else {
                        val parts = time.split(":")
                        val hour = parts.firstOrNull()?.toIntOrNull()
                        val mins = parts.lastOrNull()?.toIntOrNull()

                        hour?.takeIf { it in 0..24 }
                            ?.also { bot.dataHandler.set<PictureOfTheDayFeature>("h", it) }

                        mins?.takeIf { it in 0..60 }
                            ?.also { bot.dataHandler.set<PictureOfTheDayFeature>("m", it) }
                    }

                    // Send feedback
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot).setDescription(bot.language.translate("feature.picture_of_the_day.command.set-time.success")).build()
                    ).setEphemeral(true).queue()

                    handler.reloadSchedule()
                }
            }
        }
    }
}