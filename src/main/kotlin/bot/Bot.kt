package de.c4vxl.bot

import de.c4vxl.bot.feature.*
import de.c4vxl.bot.feature.channel.ChannelFeature
import de.c4vxl.bot.feature.settings.ConfigFeature
import de.c4vxl.bot.feature.settings.LanguageFeature
import de.c4vxl.bot.feature.settings.PermissionFeature
import de.c4vxl.bot.feature.settings.SettingsFeature
import de.c4vxl.bot.feature.tickets.TicketFeature
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.bot.handler.ComponentHandler
import de.c4vxl.bot.handler.CommandHandler
import de.c4vxl.bot.handler.DataHandler
import de.c4vxl.bot.handler.PermissionHandler
import de.c4vxl.config.Config
import de.c4vxl.language.Language
import de.c4vxl.utils.ClassUtils.className
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
    val componentHandler: ComponentHandler = ComponentHandler(this)

    // Initialize language
    var language: Language = Language.fromName(dataHandler.get<String>("metadata", "lang") ?: Language.DEFAULT)

    // Contains all the feature instances of this bot
    private val featureRegistry: MutableMap<String, Feature<*>> = mutableMapOf()

    // Initialize Bot
    init {
        logger.info("Initializing bot for guild '${guild.id}'")

        // Register features
        registerFeature<PingPongFeature>(this)
        registerFeature<PermissionFeature>(this)
        registerFeature<LanguageFeature>(this)
        registerFeature<EmbedFeature>(this)
        registerFeature<ChannelFeature>(this)
        registerFeature<SettingsFeature>(this)
        registerFeature<RulesFeature>(this)
        registerFeature<TicketFeature>(this)
        registerFeature<DefaultRoleFeature>(this)
        registerFeature<ActivityFeature>(this)
        registerFeature<WelcomeFeature>(this)
        registerFeature<ConfigFeature>(this)

        // Initialize command handler
        commandHandler.initHandlers()
        commandHandler.update()
    }

    /**
     * Instantiates and registers a feature to the bots feature-registry
     * @param args The constructor arguments needed for the feature
     */
    private inline fun <reified T : Feature<*>> registerFeature(vararg args: Any) {
        val name = className(T::class.java)
        if (Config.get<List<String>>("disable_features").any { it.contentEquals(name) }) {
            logger.warn("Won't enable feature '$name' as it is disabled in config.")
            return
        }

        val instance: Any = T::class.java.constructors[0]?.newInstance(*args) ?: return
        val feature: Feature<*> = instance as? Feature<*> ?: return

        featureRegistry[feature.name] = feature
    }
}