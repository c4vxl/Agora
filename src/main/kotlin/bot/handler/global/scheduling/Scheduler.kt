package de.c4vxl.bot.handler.global.scheduling

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Holds a global thread pool scheduler
 */
object Scheduler {
    val executor: ScheduledExecutorService =
        Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())
}