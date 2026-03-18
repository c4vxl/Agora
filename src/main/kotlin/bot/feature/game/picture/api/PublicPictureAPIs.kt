package de.c4vxl.bot.feature.game.picture.api

import com.google.gson.Gson
import de.c4vxl.bot.feature.game.picture.PictureFeature
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.PictureUtils.fetchImage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.FileUpload
import java.net.URL

/**
 * Implementations for various picture APIs
 */
class PublicPictureAPIs(val feature: PictureFeature) {
    /**
     * Creates an embed to send
     * @param apiName The type of api (e.g. cat or dog).
     * @param image The image to send
     */
    private fun createEmbed(apiName: String, image: FileUpload?): MessageEmbed {
        if (image == null)
            return Embeds.FAILURE(feature.bot)
                .setDescription(feature.bot.language.translate("feature.picture.embed.reply.fetch_error"))
                .build()

        return EmbedBuilder()
            .setTitle(feature.bot.language.translate("feature.picture.type.$apiName"))
            .setDescription(feature.bot.language.translate(
                "feature.picture.embed.reply.desc",
                feature.bot.language.translate("feature.picture.type.$apiName").lowercase()
            ))
            .setImage("attachment://${image.name}")
            .color(Color.PRIMARY)
            .build()
    }

    /**
     * Fetches a random cat picture
     * @param queries Optional queries to narrow down search
     */
    fun cat(vararg queries: String): PictureFeatureAPIResponse {
        val baseURL = "https://cataas.com/cat"

        val image = fetchImage(buildString {
            append(baseURL)
            
            // Add queries
            if (queries.isNotEmpty())
                append("/${queries.joinToString(",")}")
        })

        return PictureFeatureAPIResponse(
            createEmbed("cat", image),
            image,
            image == null
        )
    }

    /**
     * Fetches a random dog picture
     */
    fun dog(): PictureFeatureAPIResponse {
        val url = URL("https://api.thedogapi.com/v1/images/search?size=med&mime_types=jpg&format=json")

        val content = Gson().fromJson<List<Map<String, String>>>(
            url.readText(),
            List::class.java
        )

        val image = content.firstOrNull()?.get("url")?.let { fetchImage(it) }
        return PictureFeatureAPIResponse(
            createEmbed("dog", image),
            image,
            image == null
        )
    }
}