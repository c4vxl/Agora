package de.c4vxl

import de.c4vxl.bot.Bot
import de.c4vxl.data.Database
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import kotlin.concurrent.fixedRateTimer

fun main() {
    // Create JDA instance
    val jda: JDA = JDABuilder.createDefault("MTQ2MTc4MjcxMTgzMTIzNjczOA.G9Os8g.qdjhDYVIFlz_zR4HHAxQI6-L9wvcBa9bSI2yEQ")
        .setActivity(Activity.customStatus("Just vibing"))
        .setStatus(OnlineStatus.IDLE)
        .build().awaitReady()

    // Create bot-instance for every guild the bot is in
    jda.guilds.forEach { Bot(jda, it) }

    // Create bot-instance when joining a guild
    jda.addEventListener(object : ListenerAdapter() {
        override fun onGuildJoin(event: GuildJoinEvent) {
            Bot(jda, event.guild)
        }
    })

    // Register dataset autosave
    fixedRateTimer("dataset-autosave", daemon = true, period = 30_000) {
        Database.saveAll()
    }

    // Register shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        jda.shutdown()
    })


}