package de.c4vxl.bot.handler.static

import java.util.concurrent.Executors

/**
 * Holds a global thread pool scheduler
 */
object Scheduler {
    val scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())
}