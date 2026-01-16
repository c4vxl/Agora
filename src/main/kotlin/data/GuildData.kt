package de.c4vxl.data

/**
 * The actual data container
 */
data class GuildData(
    val identifier: Long,
    val entries: MutableMap<String, MutableMap<String, Any>> = mutableMapOf()
)