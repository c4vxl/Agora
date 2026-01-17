package de.c4vxl.utils

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.Category

/**
 * Utilities used for channel and categories
 */
object ChannelUtils {
    /**
     * Deletes a category if it doesn't contain any channels
     */
    fun Category.deleteIfEmpty() =
        if (this.channels.isEmpty()) this.delete() else null

    /**
     * Returns a string of the type of channel
     */
    val Channel.typeName: String?
        get() = when (this.type) {
            ChannelType.VOICE -> "voice"
            ChannelType.TEXT  -> "text"
            else              -> null
        }

    fun Guild.getChannel(id: String): Channel? {
        return this.getTextChannelById(id)
            ?: this.getVoiceChannelById(id)
            ?: this.getStageChannelById(id)
            ?: this.getForumChannelById(id)
    }
}