package de.c4vxl.bot.handler

import de.c4vxl.bot.Bot
import de.c4vxl.data.Database
import de.c4vxl.utils.LoggerUtils.createLogger
import org.slf4j.Logger

/**
 * Wrapper around the central database limiting access to only the bot-specific data
 * @see de.c4vxl.data.Database
 */
class DataHandler(
    val bot: Bot,
    val logger: Logger = createLogger<DataHandler>()
) {
    init {
        logger.info("Initializing data handler for guild '${bot.guild.id}'")
    }

    /**
     * Loads the data object of a module
     * @param name The name of the module
     */
    fun data(name: String): MutableMap<String, Any> =
        Database.get(bot.guild.idLong)
            .entries.getOrPut(name) { mutableMapOf() }

    /**
     * Returns a specific value from the data object of a specific module
     * @param module The name of the module
     * @param key The key to the data element
     */
    inline fun <reified R> get(module: String, key: String): R? =
        data(module)[key] as? R

    /**
     * Set the value of a specific element in the data object of a module
     * @param module The name of the module
     * @param key The key to the data element
     * @param value The new value
     */
    fun set(module: String, key: String, value: Any) {
        data(module)[key] = value
        Database.makeDirty(bot.guild.idLong)
    }
}