package de.c4vxl.bot.handler

import de.c4vxl.bot.Bot
import de.c4vxl.data.Database
import de.c4vxl.utils.FeatureUtils.featureName
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
     * Loads the feature specific data from the database
     */
    inline fun <reified T> featureData(): MutableMap<String, Any> =
        Database.get(bot.guild.idLong)
            .entries.getOrPut(
                featureName(T::class.java)
            ) { mutableMapOf() }

    /**
     * Returns a specific value from the data of a feature
     * @param key The key to the element
     */
    inline fun <reified T, reified R> get(key: String): R? =
        featureData<T>()[key] as? R

    /**
     * Sets a value in the data of a feature
     * @param key The name of the element
     * @param value The new value
     */
    inline fun <reified T> set(key: String, value: Any) {
        featureData<T>()[key] = value
        Database.makeDirty(bot.guild.idLong)
    }

}