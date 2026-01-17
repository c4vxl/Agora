package de.c4vxl

import de.c4vxl.bot.Bot
import de.c4vxl.config.Config
import de.c4vxl.data.Database
import de.c4vxl.utils.LoggerUtils.createLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger
import kotlin.concurrent.fixedRateTimer

fun main() {
    val logger: Logger = createLogger("Agora")

    // Create JDA instance
    logger.info("Creating JDA instance")
    val jda: JDA = JDABuilder.createDefault(Config.get<String>("token"))
        .setActivity(Activity.customStatus(Config.get<String>("activity")))
        .setStatus(OnlineStatus.fromKey(Config.get<String>("status")))
        .build().awaitReady()

    // Create bot-instance for every guild the bot is in
    logger.info("Enabling bot for guilds")
    jda.guilds.forEach { Bot(jda, it) }

    // Create bot-instance when joining a guild
    jda.addEventListener(object : ListenerAdapter() {
        override fun onGuildJoin(event: GuildJoinEvent) {
            logger.info("Joined new guild: ${event.guild.id}")
            Bot(jda, event.guild)
        }
    })

    // Register dataset autosave
    fixedRateTimer("dataset-autosave", daemon = true, period = Config.get<Double>("db_cache_save_interval").toLong()) {
        Database.saveAll()
    }

    // Register shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down...")
        Database.saveAll()
        jda.shutdown()
    })


}