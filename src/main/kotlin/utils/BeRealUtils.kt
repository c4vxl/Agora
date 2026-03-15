package de.c4vxl.utils

import de.c4vxl.bot.feature.game.bereal.BeRealFeature
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

object BeRealUtils {
    val days = listOf("mo", "tu", "we", "th", "fr", "sa", "su")

    /**
     * Returns the short name of the current day of the week
     */
    fun currentDay(): String =
        days[LocalDate.now().dayOfWeek.value - 1]

    /**
     * Generates a list of random times for a specific date
     * @param date The date
     * @param defaultAmount The default amount of posts a day (used if not set in bot config)
     * @param defaultStartHour The default starting hour of a day (used if not set in bot config)
     * @param defaultStartMinute The default starting minute of a day (used if not set in bot config)
     * @param defaultEndHour The default ending hour of a day (used if not set in bot config)
     * @param defaultEndMinute The default ending minute of a day (used if not set in bot config)
     */
    fun generateTimes(
        feature: BeRealFeature,
        date: LocalDate = LocalDate.now(),

        // By default, two posts a day
        defaultAmount: Int = 2,

        // Default start time: 7:30 PM
        defaultStartHour: Int = 7,
        defaultStartMinute: Int = 30,

        // Default end time: 11 PM
        defaultEndHour: Int = 23,
        defaultEndMinute: Int = 0
    ): List<LocalDateTime> {
        fun time(type: String, day: String): Int {
            // Get times
            var h = feature.bot.dataHandler.get<Int>(feature.name, "$type.$day.h")
            var m = feature.bot.dataHandler.get<Int>(feature.name, "$type.$day.m")

            // Fallback if no times in db
            if (h == null || m == null) {
                feature.logger.warn("Time missing in database. Falling back. (Day: $day)")

                // Fallback to global time
                if (day != "all")
                    return time(type, "all")

                // Fallback to default values
                h = if (type == "start") defaultStartHour
                    else defaultEndHour

                m = if (type == "min") defaultStartMinute
                    else defaultEndMinute
            }

            return LocalTime.of(h, m).toSecondOfDay()
        }

        var start = time("start", currentDay())
        var end = time("end", currentDay())

        // Flip times if start is after end
        if (start > end) {
            val tmp = end
            end = start
            start = tmp
        }

        // Get amount of posts per day
        val num = feature.bot.dataHandler.get<Int>(feature.name, "amount") ?: defaultAmount

        // Create list
        return (1..num).map {
            val randomSec = Random.nextInt(start, end)
            LocalDateTime.of(date, LocalTime.ofSecondOfDay(randomSec.toLong()))
        }.distinct().sorted()
    }
}