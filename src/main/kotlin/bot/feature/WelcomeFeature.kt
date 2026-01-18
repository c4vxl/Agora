package de.c4vxl.bot.feature

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Color
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

/**
 * A simple feature that sends custom welcome messages in a specified channel when a new user joins
 */
class WelcomeFeature(bot: Bot) : Feature<WelcomeFeature>(bot, WelcomeFeature::class.java) {
    init {
        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
                if (event.guild.id != bot.guild.id) return

                welcome(event.user)
            }
        })
    }

    private fun welcome(user: User) {
        val channelId = bot.dataHandler.get<String>(this@WelcomeFeature.name, "channel") ?: return
        val channel = bot.guild.getTextChannelById(channelId)

        if (channel == null) {
            logger.warn("Channel id was set in config, but channel doesn't exist!")
            return
        }

        val title = get<String>("title", user)
        val description = get<String>("description", user)
        val footer = get<String>("footer", user)
        val image = get<String>("image", user)
        val thumb = get<String>("thumb", user)

        if (listOfNotNull(title, description, footer, image, thumb).isEmpty())
            return

        channel.sendMessageEmbeds(
            EmbedFeature.createEmbed(
                description, title,
                null, null, null, null,
                footer, null, image, thumb, null,
                null, get("color", user) ?: Color.PRIMARY.asInt, true
            )
        ).queue()
    }

    private inline fun <reified T> get(x: String, user: User): T? {
        var out: T? = bot.dataHandler.get<T>(this@WelcomeFeature.name, x)

        if (out is String)
            out = out.replace("\$user", user.asMention)
                .replace("\$user_icon", user.avatarUrl ?: "") as T?

        return out
    }
}