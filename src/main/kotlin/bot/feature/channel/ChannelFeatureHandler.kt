package de.c4vxl.bot.feature.channel

import de.c4vxl.bot.Bot
import de.c4vxl.enums.Permission
import de.c4vxl.utils.ChannelUtils.deleteIfEmpty
import de.c4vxl.utils.ChannelUtils.typeName
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.requests.restaction.ChannelAction

/**
 * Class for handling the data of the ChannelFeature
 * @see de.c4vxl.bot.feature.channel.ChannelFeature
 */
class ChannelFeatureHandler(val feature: ChannelFeature) {
    private val bot: Bot = feature.bot

    /**
     * Returns the config value for the maximum channels of a type a user is allowed to have
     * @param type The type of channel
     */
    fun maxChannelsPerUser(type: String): Int =
        bot.dataHandler.get<Int>(feature.name, "max_${type}") ?: 1

    /**
     * Checks if a user is the owner of a channel
     * @param channel The channel to check
     * @param user The user to check for
     */
    fun isOwner(channel: Channel, user: User): Boolean {
        return getChannels(
            channel.typeName ?: return false,
            user
        ).contains(channel.id)
    }

    /**
     * Creates a channel
     * @param type The type of channel (text or voice)
     * @param owner The owner of the channel
     * @param name The name of the channel
     * @param private If set to true, only the member will be able to view the channel
     */
    fun createChannel(type: String, owner: Member, name: String, private: Boolean): Channel? {
        // Exit early if user has too many channels already
        if (
            !owner.hasPermission(Permission.FEATURE_CHANNELS_UNLIMITED, bot)
            && getChannels(type, owner.user).size >= maxChannelsPerUser(type)
        ) return null

        // Get category
        val category: Category = getCategory(type)

        // Create channel
        val action: ChannelAction<*> = when (type) {
            "text"  -> bot.guild.createTextChannel(name, category)
            "voice" -> bot.guild.createVoiceChannel(name, category)
            else    -> return null
        }

        // Handle permissions
        action
            .addPermissionOverride(
                bot.guild.publicRole,

                // Grant permissions if channel is not private
                if (!private) mutableListOf(
                    net.dv8tion.jda.api.Permission.VIEW_CHANNEL,
                    net.dv8tion.jda.api.Permission.VOICE_CONNECT,
                    net.dv8tion.jda.api.Permission.VOICE_SPEAK,
                    net.dv8tion.jda.api.Permission.VOICE_STREAM,
                    net.dv8tion.jda.api.Permission.MESSAGE_SEND
                )
                else mutableListOf(),

                // Deny view permission to everyone if channel is private
                if (private) mutableListOf(net.dv8tion.jda.api.Permission.VIEW_CHANNEL) else mutableListOf()
            )
            .addMemberPermissionOverride(
                owner.idLong,
                net.dv8tion.jda.api.Permission.entries,  // Grant all permissions to owner
                mutableListOf()                          // Deny none
            )

        return action.complete()
            .apply {
                // Register channel
                register(this, owner.user)
            }
    }

    /**
     * Returns a list of all channels of a given type
     * @param type The type of channel (Either "text" or "voice")
     */
    fun getChannels(type: String): MutableMap<String, String> =
        bot.dataHandler.get<MutableMap<String, String>>(feature.name, type)
            // Filter out channels that don't exist anymore
            ?.filter {
                when (type) {
                    "text" -> bot.guild.getTextChannelById(it.key) != null
                    "voice" -> bot.guild.getVoiceChannelById(it.key) != null
                    else   -> false
                }
            }
            ?.toMutableMap()
            ?: mutableMapOf()

    /**
     * Returns a list of the ids of the channels a specific user has created
     * @param type The type of channel
     * @param owner The owner
     */
    fun getChannels(type: String, owner: User): MutableList<String> =
        getChannels(type).filter { it.value == owner.id }.keys.toMutableList()

    /**
     * Returns a list of the ids of all the channels a specific user has created
     * @param owner The owner
     */
    fun getChannels(owner: User): List<String> =
        getChannels("text", owner) + getChannels("voice", owner)

    /**
     * Returns the category where channels of a specific type will be created
     * @param type The channel type
     */
    fun getCategory(type: String): Category {
        val name: String = bot.dataHandler.get<String>(feature.name, "${type}_category") // get from data store
            ?: bot.language.translate("feature.channel.default_name.category.${type}") // default

        return bot.guild.getCategoriesByName(name, false)    // find category
            .firstOrNull() ?: bot.guild.createCategory(name).complete() // or create it
    }

    /**
     * Set the category where channels of a specific type will be created in
     * @param type The channel type
     * @param category The new category
     */
    fun setCategory(type: String, category: Category) =
        bot.dataHandler.set(feature.name, "${type}_category", category.name)

    /**
     * Add a channel to the registry
     * @param channel The channel to add
     * @param owner The owner of the channel
     */
    fun register(channel: Channel, owner: User) {
        val type = channel.typeName ?: return

        // Get registered channels
        val channels = getChannels(type)

        // Add channel
        channels[channel.id] = owner.id

        // Update config
        bot.dataHandler.set(feature.name, type, channels)
    }

    /**
     * Remove a channel from the registry and delete it
     * @param channel The channel to remove
     */
    fun remove(channel: Channel) {
        val type = channel.typeName ?: return

        // Get registered channels
        val channels = getChannels(type)

        if (!channels.containsKey(channel.id)) return

        // Add channel
        channels.remove(channel.id)

        // Update config
        bot.dataHandler.set(feature.name, type, channels)

        // Delete channel
        channel.delete().queue {
            // Delete category if empty
            getCategory(type).deleteIfEmpty()?.queue()
        }
    }
}