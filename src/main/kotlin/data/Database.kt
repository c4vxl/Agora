package de.c4vxl.data

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * The central database interface for loading and saving data
 */
object Database {
    // Path to database directory
    private val basePath: Path = Paths.get("db/")

    // Cache of data
    private val cache = ConcurrentHashMap<Long, DataCache>()

    // Json en-/decoder
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Get the data of a specific guild
     * @param guildId The id of the guild to load the data of
     */
    fun get(guildId: Long): GuildData {
        return cache.computeIfAbsent(guildId) {
            loadData(guildId)
        }.data
    }

    /**
     * Loads the data of a specific guild into cache
     * @param guildId The id of the guild to load the data of
     */
    private fun loadData(guildId: Long): DataCache {
        val path: Path = basePath.resolve("${guildId}.db")

        val data: MutableMap<String, MutableMap<String, Any>> =
            if (Files.exists(path))
                gson.fromJson(Files.readString(path), object : TypeToken<MutableMap<String, MutableMap<String, Any>>>() {}.type)
            else
                mutableMapOf()

        return DataCache(
            GuildData(
                guildId,
                data
            )
        )
    }

    /**
     * Saves a specific guilds data from cache to disk and cleans cache
     * @param guildId The id of the guild to save the data of
     */
    fun save(guildId: Long) {
        val cached = cache[guildId] ?: return

        // Return if cache hasn't changed
        if (!cached.dirty) return

        val path = basePath.resolve("${guildId}.db")
        Files.createDirectories(path.parent)

        // Save to file
        Files.writeString(
            path,
            gson.toJson(cached.data.entries)
        )

        // Set cache as clean
        cached.dirty = false
    }

    /**
     * Saves the entire cache to disk
     */
    fun saveAll() =
        cache.forEach { (id, _) -> save(id) }

    /**
     * Mark the cache of a specific guilds data as dirty
     */
    fun makeDirty(guildId: Long) {
        cache[guildId]?.dirty = true
    }
}