package de.c4vxl.bot.feature.game.picture

import de.c4vxl.bot.feature.game.picture.api.PublicPictureAPIs
import de.c4vxl.bot.feature.game.picture.api.UnsplashAPI
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Handler taking care of picture feature
 */
class PictureFeatureHandler(val feature: PictureFeature) {
    init {
        val now = LocalTime.now()
        val nextHour = now.withMinute(0).withSecond(0)
            .plusHours(1)

        feature.tasks.scheduleAtFixedRate(
            Duration.between(now, nextHour).toMinutes(),
            60,
            {
                unsplashUses.clear()
            },
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
}