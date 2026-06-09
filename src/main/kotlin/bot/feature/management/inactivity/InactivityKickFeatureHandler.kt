package bot.feature.management.inactivity

import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.config.enums.Permission
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.time.Duration
import java.util.concurrent.ScheduledFuture

class InactivityKickFeatureHandler(val feature: InactivityKickFeature) {
    val bot = feature.bot

    private var task: ScheduledFuture<*>? = null

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
     * Holds the last active time of each member
     */
    var lastActivity: MutableMap<String, Long>
        get() = bot.dataHandler.get<MutableMap<String, Long>>(feature.name, "inactivity")
            ?: mutableMapOf()

        set(value) = bot.dataHandler.set(feature.name, "inactivity", value)

    /**
     * Marks the user as to have done an activity
     * @param user The user
     */
    fun markActivity(user: User) {
        if (user.isBot || user.isSystem)
            return

        lastActivity = lastActivity.apply { set(user.id, System.currentTimeMillis()) }
    }

    /**
     * Returns the amount of days since a timestamp
     * @param timestamp The ms timestamp
     */
    fun daysSince(timestamp: Long): Int =
        Duration.ofMillis(
            System.currentTimeMillis() - timestamp
        ).toDays().toInt()

    /**
     * Returns the inactivity of a user (in days)
     * @param userId The user to check
     */
    fun getInactivity(userId: String): Int {
        return daysSince(
            lastActivity[userId] ?: return 0
        )
    }

    /**
     * Kicks a member from the guild and sends a message
     * @param member The member to kick
     * @param days Overwrite the amount of inactivity shown to the member
     */
    fun kick(member: Member, days: Int? = null) {
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

        val inactivity = days ?: getInactivity(member.id)
        member.user.openPrivateChannel().queue { pc ->
            pc.sendMessage(
                MessageCreateBuilder()
                    .addEmbeds(
                        EmbedBuilder()
                            .withTimestamp()
                            .color(Color.DANGER)
                            .setTitle(bot.language.translate("feature.inactivity.notification.kick.title"))
                            .setDescription(bot.language.translate("feature.inactivity.notification.kick.desc", bot.guild.name, inactivity.toString()))
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
    fun schedule() {
        // Cancel task
        feature.tasks.cancel(task)

        // Exit if feature is not enabled
        if (!isEnabled) return

        task = feature.tasks.scheduleDaily(0) {
            // Exit if invalid inactivity lock
            if (kickAfter < 1)
                return@scheduleDaily

            lastActivity
                .map { it.key to getInactivity(it.key) }
                .filter { (_, days) -> days > kickAfter }
                .forEach { (memberId, days) ->
                    bot.guild.retrieveMemberById(memberId)
                        .queue { member ->
                            kick(member, days)
                        }
                }
        }
    }
}