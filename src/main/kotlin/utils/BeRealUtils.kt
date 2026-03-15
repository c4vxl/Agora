package de.c4vxl.utils

import de.c4vxl.bot.feature.game.bereal.BeRealFeature
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random

object BeRealUtils {
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
        // Get start time (default: 7:30 PM)
        val start = LocalTime.of(
            feature.bot.dataHandler.get<Int>(feature.name, "start.h") ?: defaultStartHour,
            feature.bot.dataHandler.get<Int>(feature.name, "start.m") ?: defaultStartMinute
        ).toSecondOfDay()

        // Get end time (default: 11:00 PM)
        val end = LocalTime.of(
            feature.bot.dataHandler.get<Int>(feature.name, "end.h") ?: defaultEndHour,
            feature.bot.dataHandler.get<Int>(feature.name, "end.m") ?: defaultEndMinute
        ).toSecondOfDay()

        // Get amount of posts per day (default: 2)
        val num = feature.bot.dataHandler.get<Int>(feature.name, "amount") ?: defaultAmount

        // Create list
        return (1..num).map {
            val randomSec = Random.nextInt(start, end)
            LocalDateTime.of(date, LocalTime.ofSecondOfDay(randomSec.toLong()))
        }.distinct().sorted()
    }
}