package de.c4vxl.bot

import de.c4vxl.utils.LoggerUtils.createLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import org.slf4j.Logger

/**
 * A guild-level bot class for handling guild-specific features
 */
class Bot(
    val jda: JDA,
    val guild: Guild,
    val logger: Logger = createLogger<Bot>()
) {
    // Initialize Bot
    init {
        logger.info("Initializing bot for guild '${guild.id}'")
    }
}