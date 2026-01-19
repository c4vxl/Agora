package de.c4vxl.bot.feature

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import net.dv8tion.jda.api.interactions.commands.build.Commands

/**
 * Example feature
 */
class PingPongFeature(bot: Bot) : Feature<PingPongFeature>(bot, PingPongFeature::class.java) {
    init {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("ping", "Play ping-pong with the bot!")
        ) { event ->
            event.reply("Pong!")
                .setEphemeral(true)
                .queue()
        }
    }

    override fun registerCommands() {
        // Nothing
    }
}