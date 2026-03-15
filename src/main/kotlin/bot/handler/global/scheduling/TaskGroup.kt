package de.c4vxl.bot.handler.global.scheduling

import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * A group for keeping track of scheduled tasks
 */
class TaskGroup {
    private val tasks = mutableListOf<ScheduledFuture<*>>()

    /**
     * Schedules a task
     * @param delay The delay for when the task should run
     * @param task The task to run
     * @param unit The time unit the delay is in
     */
    fun schedule(delay: Long, task: () -> Unit, unit: TimeUnit = TimeUnit.SECONDS) {
        tasks += Scheduler.executor.schedule(task, delay, unit)
    }

    /**
     * Cancels all running tasks
     */
    fun cancelAll() {
        tasks.forEach { it.cancel(false) }
        tasks.clear()
    }
}