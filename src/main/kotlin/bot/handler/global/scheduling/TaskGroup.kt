package de.c4vxl.bot.handler.global.scheduling

import net.dv8tion.jda.api.entities.Guild
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * A group for keeping track of scheduled tasks
 */
class TaskGroup(val guild: Guild) {
    init {
        // Register this task group
        Scheduler.registeredGroups.getOrPut(guild.idLong) { mutableListOf() }
            .add(this)
    }

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

    /**
     * Unregisters this group and cancels all tasks
     */
    fun destroy() {
        cancelAll()
        Scheduler.registeredGroups.getOrPut(guild.idLong) { mutableListOf() }
            .remove(this)
    }
}