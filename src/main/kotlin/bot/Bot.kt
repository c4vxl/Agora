package de.c4vxl.bot

import de.c4vxl.bot.feature.LanguageFeature
import de.c4vxl.bot.feature.PermissionFeature
import de.c4vxl.bot.feature.PingPongFeature
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.bot.handler.CommandHandler
import de.c4vxl.bot.handler.DataHandler
import de.c4vxl.bot.handler.PermissionHandler
import de.c4vxl.language.Language
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
    // Initialize handlers
    val commandHandler: CommandHandler = CommandHandler(this)
    val dataHandler: DataHandler = DataHandler(this)
    val permissionHandler: PermissionHandler = PermissionHandler(this)

    // Initialize language
    var language: Language = Language.fromName(dataHandler.get<String>("metadata", "lang") ?: Language.DEFAULT)

    // Contains all the feature instances of this bot
    val featureRegistry: MutableMap<String, Feature<*>> = mutableMapOf()

    // Initialize Bot
    init {
        logger.info("Initializing bot for guild '${guild.id}'")

        registerFeature<PingPongFeature>(this)
        registerFeature<PermissionFeature>(this)
        registerFeature<LanguageFeature>(this)

        // Initialize command handler
        commandHandler.initHandlers()
        commandHandler.update()
    }

    /**
     * Instantiates and registers a feature to the bots feature-registry
     * @param args The constructor arguments needed for the feature
     */
    private inline fun <reified T : Feature<*>> registerFeature(vararg args: Any) {
        val instance: Any = T::class.java.constructors[0]?.newInstance(*args) ?: return
        val feature: Feature<*> = instance as? Feature<*> ?: return

        featureRegistry[feature.name] = feature
    }
}