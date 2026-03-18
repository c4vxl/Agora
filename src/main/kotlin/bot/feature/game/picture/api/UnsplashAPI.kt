package de.c4vxl.bot.feature.game.picture.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import de.c4vxl.bot.feature.game.picture.PictureFeature
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.PictureUtils
import net.dv8tion.jda.api.EmbedBuilder
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class UnsplashAPI(val feature: PictureFeature) {
    private val API_KEY: String?
        get() {
            // Fetch API key
            val apiKey = feature.bot.dataHandler.get<String>(feature.name, "unsplash_key")

            // Fail if null
            if (apiKey == null) {
                feature.logger.warn("Tried to access unsplash API without a valid API key!")
                return null
            }

            return apiKey
        }

    val BASE_URL = "https://api.unsplash.com"

    /**
     * Fetches from the API
     * @param endpoint The url endpoint
     */
    fun fetch(endpoint: String = ""): JsonObject? {
        // Create request
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL/${endpoint}"))
            .headers("Authorization", "Client-ID $API_KEY")
            .GET()
            .build()

        // Send request
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            feature.logger.warn("Unsplash API has returned an error for guild '${feature.bot.guild.id}' (${response.statusCode()}): \n${response.body()}")
            return null
        }

        // Parse result
        return Gson().fromJson(
            response.body(),
            JsonObject::class.java
        )
    }

    /**
     * Appends queries to api endpoint
     * @param queries The queries to add
     */
    private fun appendQueries(endpoint: String, vararg queries: String) =
        buildString {
            append(endpoint)

            if (queries.isNotEmpty())
                append("?query=" + queries.joinToString(",") { it.trim().replace(" ", "+") })
        }

    /**
     * Fetches a random image from unsplash
     * @param queries Optional queries to narrow down search
     */
    fun random(feature: PictureFeature, vararg queries: String): PictureFeatureAPIResponse {
        val response = fetch(appendQueries("photos/random", *queries))
            ?: return PictureFeatureAPIResponse(Embeds.FAILURE(feature.bot)
                .setDescription(feature.bot.language.translate("feature.picture.embed.unsplash.failure.invalid_key"))
                .build(), ephemeral = true)

        val picURL = response.getAsJsonObject("urls")
            .get("regular").asString
        val author = response.getAsJsonObject("user").get("username").asString

        val type = if (queries.isEmpty()) "random" else "specific"
        val queriesString = queries.joinToString(",")


        return PictureFeatureAPIResponse(
            EmbedBuilder()
                .setTitle(feature.bot.language.translate("feature.picture.embed.unsplash.title.$type", queriesString.replace(",", " ")))
                .setDescription(feature.bot.language.translate("feature.picture.embed.unsplash.desc.$type", queriesString))
                .setImage("attachment://picture.jpg")
                .setFooter(feature.bot.language.translate("feature.picture.embed.unsplash.credits", author))
                .color(Color.PRIMARY)
                .build(),
            PictureUtils.fetchImage(picURL)
        )
    }
}