package de.c4vxl.bot.handler.scheduling

import net.dv8tion.jda.api.entities.Guild
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * A group for keeping track of scheduled tasks
 */
class TaskGroup(val guild: Guild) {
    init {
        // Register this task group
        Scheduler.registeredGroups.getOrPut(guild.idLong) { mutableListOf() }
            .add(this)
    }

    var zoneId: ZoneId = ZoneId.systemDefault()

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
     * Runs a specific task once at a specified time today
     * If that time has already passed, the task will be ignored
     *
     * @param hour The hour to run the task at
     * @param minute The minute
     * @param second The second
     * @param task The task to run
     */
    fun runAt(hour: Int, minute: Int = 0, second: Int = 0, task: () -> Unit): ScheduledFuture<*>? {
        // Get target time
        val now = LocalDateTime.now(zoneId)
        val target = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(second)
            .withNano(0)

        // Exit if same time
        if (target.isBefore(now) || target.isEqual(now))
            return null

        // Schedule task
        val delay = Duration.between(now, target).toMillis()
        return schedule(delay, task, TimeUnit.MILLISECONDS)
    }

    /**
     * Runs a task every day at a specific time
     * If time has already passed for today, task won't run for today
     *
     * @param hour The hour to run the task at
     * @param minute The minute to run the task at
     * @param second The second to run the task at
     * @param task The task to run
     */
    fun scheduleDaily(hour: Int, minute: Int = 0, second: Int = 0, task: () -> Unit): ScheduledFuture<*> {
        // Calculate time until first run
        val now = LocalDateTime.now(zoneId)
        var firstRun = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(second)
            .withNano(0)

        // Skip to next day if already passed
        if (firstRun.isBefore(now) || firstRun.isEqual(now))
            firstRun = firstRun.plusDays(1)

        // Schedule daily run
        val initialDelay = Duration.between(now, firstRun).toMillis()
        val dailyPeriod = TimeUnit.DAYS.toMillis(1)
        return scheduleAtFixedRate(initialDelay, dailyPeriod, task, TimeUnit.MILLISECONDS)
    }

    /**
     * Unregisters and cancels a specific task
     * @param task The tasks to stop
     */
    fun cancel(vararg task: ScheduledFuture<*>?) {
        task.forEach {
            tasks.remove(it)
            it?.cancel(false)
        }
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