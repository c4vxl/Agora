package de.c4vxl.bot.feature.game.joke.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URL

/**
 * Access point to the "official joke api" by 15Dkatz
 */
object JokesAPI {
    const val BASE_URL: String = "https://official-joke-api.appspot.com/"

    /**
     * Parses the response of the api
     * @param response The response as a string
     */
    private fun parse(response: String): Joke? {
        val obj = Gson().fromJson(response, JsonObject::class.java)

        return try {
            Joke(
                obj.get("setup").asString,
                obj.get("punchline").asString,
                obj.get("type").asString
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns a random joke
     * @param category An optional category to look for
     */
    fun random(category: String? = null): Joke? {
        val url = buildString {
            // Base url
            append("$BASE_URL/jokes/")

            // Append category
            category?.let { append("$it/") }

            // Random joke
            append("random")
        }

        return parse(URL(url).readText())
    }
}