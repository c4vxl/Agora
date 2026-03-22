package de.c4vxl.bot.feature.game.joke.api

/**
 * Data object of a joke
 */
data class Joke(
    val setup: String? = null,
    val punchline: String? = null,
    val category: String,
    val fullJoke: String = "$setup\n$punchline"
)