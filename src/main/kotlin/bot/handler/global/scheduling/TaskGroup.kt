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
     * Registers a scheduled future to cache
     * @param handler The function creating the future
     */
    fun register(handler: () -> ScheduledFuture<*>) =
        handler.invoke().also { tasks += it }

    /**
     * Schedules a task
     * @param delay The delay for when the task should run
     * @param task The task to run
     * @param unit The time unit the delay is in
     */
    fun schedule(delay: Long, task: () -> Unit, unit: TimeUnit = TimeUnit.SECONDS) =
        register { Scheduler.executor.schedule(task, delay, unit) }

    /**
     * Schedules a task at a fixed rate
     * @param initialDelay The initial delay to wait before running the task the first time
     * @param period The waiting period between two runs
     * @param task The task to run
     * @param unit The time unit the delay is in
     */
    fun scheduleAtFixedRate(initialDelay: Long, period: Long, task: () -> Unit, unit: TimeUnit = TimeUnit.SECONDS) =
        register { Scheduler.executor.scheduleAtFixedRate(task, initialDelay, period, unit) }

    /**
     * Unregisters and cancels a specific task
     * @param task The task to stop
     */
    fun cancelSpecific(task: ScheduledFuture<*>?) {
        tasks.remove(task)
        task?.cancel(false)
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