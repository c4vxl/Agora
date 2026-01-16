package de.c4vxl.data

/**
 * Data object for holding metadata to a cached data object
 * @see de.c4vxl.data.GuildData
 */
data class DataCache(
    val data: GuildData,
    var dirty: Boolean = false,
    var lastAccess: Long = System.currentTimeMillis()
)