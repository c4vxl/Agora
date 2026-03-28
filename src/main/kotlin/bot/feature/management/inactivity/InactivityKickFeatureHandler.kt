package bot.feature.management.inactivity

import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class InactivityKickFeatureHandler(val feature: InactivityKickFeature) {
    val bot = feature.bot

    private var incrementTask: ScheduledFuture<*>? = null

    /**
     * If true, the feature is enabled
     */
    var isEnabled: Boolean
        get() = bot.dataHandler.get<Boolean>(feature.name, "enabled") ?: false
        set(value) = bot.dataHandler.set(feature.name, "enabled", value)

    /**
     * If true, a rejoin link will be provided
     */
    var rejoinLink: Boolean
        get() = bot.dataHandler.get<Boolean>(feature.name, "rejoinLink") ?: false
        set(value) = bot.dataHandler.set(feature.name, "rejoinLink", value)

    /**
     * The amount of days to wait until a member is kicked
     */
    var kickAfter: Int
        get() = bot.dataHandler.get<Int>(feature.name, "kick_after") ?: -1
        set(value) = bot.dataHandler.set(feature.name, "kick_after", value)

    /**
     * Holds the inactivity times of members
     */
    private var inactivity: MutableMap<String, Int>
        get() = bot.dataHandler.get<MutableMap<String, Double>>(feature.name, "inactivity")

            // GSON for some reason stores the int values as doubles
            // so we need to do some extra parsing here
            ?.mapValues { it.value.toInt() }?.toMutableMap()
            ?: mutableMapOf()

        set(value) = bot.dataHandler.set(feature.name, "inactivity", value)

    /**
     * Returns the inactivity of a user
     */
    fun getInactivity(user: User): Int =
        inactivity.getOrDefault(user.id, 0)

    /**
     * Sets the inactivity value of a user
     * @param user The user
     * @param value The value to store
     */
    fun setInactivity(user: User, value: Int? = null) {
        this.inactivity = inactivity.apply {
            value
                ?.let { this[user.id] = it }
                ?: run { this.remove(user.id) }
        }
    }

    /**
     * Increments the inactivity of a user by one
     * @param user The user
     */
    fun incrementInactivity(user: User) =
        setInactivity(user, getInactivity(user) + 1)

    /**
     * Kicks a member from the guild and sends a message
     * @param member The member to kick
     */
    fun kick(member: Member) {
        val invite = if (rejoinLink) {
            bot.guild.defaultChannel
                ?.createInvite()
                ?.setMaxAge(0)?.setMaxUses(1)
                ?.complete()
        }
        else null

        // Send dm
        fun combineActionRows(existingRow: ActionRow, button: Button?) =
            ActionRow.of(
                existingRow.buttons.toMutableList().apply { button?.let { add(it) } }
            )

        member.user.openPrivateChannel().queue { pc ->
            pc.sendMessage(
                MessageCreateBuilder()
                    .addEmbeds(
                        EmbedBuilder()
                            .withTimestamp()
                            .color(Color.DANGER)
                            .setTitle(bot.language.translate("feature.inactivity.notification.kick.title"))
                            .setDescription(bot.language.translate("feature.inactivity.notification.kick.desc", bot.guild.name, getInactivity(member.user).toString()))
                            .setFooter(bot.language.translate("feature.inactivity.notification.kick.footer"))
                            .build()
                    )
                    .addComponents(
                        combineActionRows(
                            Embeds.DM_ACTION_ROW(bot),
                            invite?.let { Button.link(invite.url,
                                bot.language.translate("feature.inactivity.notification.kick.rejoin_btn", bot.guild.name)
                            ) }
                        )
                    )
                    .build()
            ).queue()
        }

        // Kick
        if (!member.isOwner)
            member.kick().queue()
    }


    /**
     * Schedules a daily increment of inactivity count
     */
    fun scheduleIncrements() {
        fun handleIncrement() {
            val inactivityClone = inactivity

            bot.guild.loadMembers().onSuccess { members ->
                members
                    .filterNot { it.user.isBot || it.user.isSystem }
                    .forEach { member ->
                        // Increment
                        incrementInactivity(member.user)

                        // Kick members that have been inactive for too long
                        if (kickAfter >= 1 && (inactivityClone[member.user.id] ?: 0) > kickAfter)
                            kick(member)
                    }
            }
        }

        // Cancel task
        feature.tasks.cancelSpecific(incrementTask)

        // Exit if feature is not enabled
        if (!isEnabled) return

        // Increment once
        handleIncrement()

        // Calculate time until next midnight
        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val initialDelay = Duration.between(now, midnight).toMinutes()

        incrementTask = this.feature.tasks.scheduleAtFixedRate(initialDelay, TimeUnit.DAYS.toMinutes(1), {
            // Increment for each user
            handleIncrement()
        }, TimeUnit.MINUTES)
    }
}