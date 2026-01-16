package de.c4vxl.bot.feature.type

import de.c4vxl.bot.Bot
import de.c4vxl.utils.LoggerUtils.createLogger
import org.slf4j.Logger

/**
 * The base for guild-specific features
 */
open class Feature<T>(
    val bot: Bot,
    val clazz: Class<T>,
    val name: String = clazz.simpleName
) {
    val logger: Logger = createLogger(clazz)

    init {
        logger.info("Initializing feature '${name}' for guild '${bot.guild.id}'")
    }
}