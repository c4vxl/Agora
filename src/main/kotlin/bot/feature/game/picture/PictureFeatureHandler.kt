package de.c4vxl.bot.feature.game.picture

import de.c4vxl.bot.feature.game.picture.api.PublicPictureAPIs
import de.c4vxl.bot.feature.game.picture.api.UnsplashAPI
import de.c4vxl.config.enums.Color
import de.c4vxl.utils.EmbedUtils.color
import net.dv8tion.jda.api.EmbedBuilder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Handler taking care of picture feature
 */
class PictureFeatureHandler(val feature: PictureFeature) {
    init {
        // Reload uses
        feature.tasks.scheduleAtFixedRate(
            0, 60,
            { unsplashUses.clear() },
            TimeUnit.MINUTES
        )
    }

    val publicAPIs = PublicPictureAPIs(feature)
    val unsplashAPI = UnsplashAPI(feature)

    /**
     * Returns the maximum amount of pictures a member can do each hour
     */
    val unsplashMaxUsesPerHour: Int
        get() = feature.bot.dataHandler.get<Int>(feature.name, "unsplash_max_pics") ?: 5

    /**
     * Holds the amount of times users have used the unsplash api
     */
    var unsplashUses: MutableMap<String, Int> = mutableMapOf()

    private var picOfTheDayTask: ScheduledFuture<*>? = null

    /**
     * Sends the picture of the day
     */
    fun sendPicOfTheDay(): Boolean {
        feature.logger.info("Sending picture of the day for '${feature.bot.guild.id}'")

        val categories = feature.bot.dataHandler.get<String>(feature.name, "potd.category")
            ?.lowercase()
            ?.replace(", ", ",")
            ?.split(",") ?: listOf()
        val channel =
            feature.bot.dataHandler.get<String>(feature.name, "potd.channel")?.let {
                feature.bot.guild.getTextChannelById(it)
            }

        if (channel == null) {
            feature.logger.warn("Tried to send pic of the day, but no channel is set! (${feature.bot.guild.id})")
            return false
        }

        val first = categories.firstOrNull()
        val queries = categories.drop(1).toTypedArray()
        val response = when (first) {
            "cat" -> publicAPIs.cat(*queries)
            "dog" -> publicAPIs.dog()
            else -> unsplashAPI.random(*queries)
        }

        // Send
        channel.sendMessageEmbeds(EmbedBuilder()
            .setTitle(feature.bot.language.translate("feature.picture_of_the_day.embed.title"))
            .setDescription(feature.bot.language.translate("feature.picture_of_the_day.embed.desc", first ?: "none"))
            .setImage("attachment://${response.file?.name}")
            .color(Color.PRIMARY)
            .apply { response.creditsString?.let { this.setFooter(it) } }
            .build())
            .apply {
                response.file?.let { addFiles(it) }
            }
            .queue()

        return true
    }

    /**
     * Reloads the pic of the day timer
     */
    fun reloadPicOfTheDay() {
        // Cancel task
        feature.tasks.cancelSpecific(picOfTheDayTask)

        // Return if not enabled
        if (feature.bot.dataHandler.get<Boolean>(feature.name, "potd.enabled") != true)
            return

        val now = LocalDateTime.now()
        val time = LocalDateTime.of(
            LocalDate.now(),
            LocalTime.of(
            feature.bot.dataHandler.get<Int>(feature.name, "potd.h") ?: 12,
            feature.bot.dataHandler.get<Int>(feature.name, "potd.m") ?: 0
        ))

        val initial =
            // Time already over
            // Use time the next day
            if (now.isAfter(time)) Duration.between(now, time.plusDays(1)).toSeconds()

            // Time in future
            // Use distance
            else Duration.between(now, time).toSeconds()

        picOfTheDayTask = feature.tasks.scheduleAtFixedRate(initial, 60 * 60 * 24, {
            // Send pic of the day
            sendPicOfTheDay()
        })
    }
}