package de.c4vxl.bot.handler.static

import net.dv8tion.jda.api.entities.Guild
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Holds global thread pool schedulers
 */
object Scheduler {
    private val schedulers: MutableMap<Long, ScheduledExecutorService> = mutableMapOf()

    /**
     * Get the scheduler of a guild
     * @param guild The guild
     */
    fun get(guild: Guild): ScheduledExecutorService =
        schedulers.getOrPut(guild.idLong) {
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())
        }

    /**
     * Delete the scheduler of a guild
     * @param guild The guild
      */
    fun remove(guild: Guild) {
        schedulers.remove(guild.idLong)
    }
}