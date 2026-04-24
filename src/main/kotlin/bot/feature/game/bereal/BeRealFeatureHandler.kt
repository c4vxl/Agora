package de.c4vxl.bot.feature.game.bereal

import de.c4vxl.bot.feature.onboarding.RulesFeature
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.utils.BeRealUtils
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.math.max

class BeRealFeatureHandler(val feature: BeRealFeature) {
    val bot = feature.bot
    val logger = feature.logger

    val thumbsUp = Emoji.fromUnicode("\uD83D\uDC4D")
    val thumbsDown = Emoji.fromUnicode("\uD83D\uDC4E")

    /**
     * Returns a role used for participants
     */
    private val participantRole: Role
        get() = bot.guild.getRolesByName("BeReal", false).firstOrNull()
            ?: bot.guild.createRole()
                .setName("BeReal")
                .setMentionable(true)
                .complete()

    init {
        // Create participant role beforehand
        participantRole

        // Load participant cache
        bot.guild.findMembersWithRoles(participantRole).onSuccess {
            participantCache = it.map { u -> u.user.id }.toMutableSet()
        }.onError { logger.warn("Failed to build participant cache: $it") }
    }

    /**
     * Returns {@code true} if the game is enabled
     */
    val isEnabled: Boolean
        get() = bot.dataHandler.get<Boolean>(this.feature.name, "enabled") ?: false

    /**
     * Holds a cache of participants
     */
    private var participantCache: MutableSet<String> = mutableSetOf()

    /**
     * Returns a list of all participants
     */
    val participants: List<String>
        get() = participantCache.toList()

    /**
     * Adds a member as a participant of the game
     * @param member The guild member to add
     */
    fun addMember(member: User): Boolean {
        if (participants.contains(member.id))
            return false

        try { bot.guild.addRoleToMember(member, participantRole).queue() }
        catch (_: Exception) { return false }

        // Add member to cache
        participantCache.add(member.id)

        return true
    }

    /**
     * Removes a member as a participant from the game
     * @param member The guild member to remove
     */
    fun removeMember(member: User): Boolean {
        if (!participants.contains(member.id))
            return false

        try { bot.guild.removeRoleFromMember(member, participantRole).queue() }
        catch (_: Exception) { return false }

        // Remove member from cache
        participantCache.remove(member.id)

        return true
    }

    /**
     * Holds the channel where BeReals will be posted in to
     */
    var channel: TextChannel?
        get() {
            val channelName = bot.dataHandler.get<String>(this.feature.name, "channel") ?: return null
            return bot.guild.getTextChannelById(channelName)
        }
        set(value) {
            bot.dataHandler.set(this.feature.name, "channel", value?.id ?: "")
            reloadView()
        }

    /**
     * Holds the streaks of the participants
     */
    var streaks: MutableMap<String, Int>
        get() =
            bot.dataHandler.get<MutableMap<String, Double>>(this.feature.name, "streak")
                ?.mapValues { it.value.toInt() }?.toMutableMap()
                ?: mutableMapOf()
        set(value) = bot.dataHandler.set(this.feature.name, "streak", value)

    /**
     * Holds whether a BeReal is currently active
     */
    var hasActiveBeReal: Boolean = false

    /**
     * Holds a list of people and their BeReal messages they posted.
     * This list gets cleared all 24 hours at midnight
     */
    val posts = mutableMapOf<String, String>()

    /**
     * Returns {@code true} if 'BeReal of the day' is enabled
     */
    val useOfTheDay
        get() = bot.dataHandler.get<Boolean>(this.feature.name, "use_otd") ?: false

    private var ofTheDayTask: ScheduledFuture<*>? = null

    /**
     * Sends the BeReal of the day
     */
    fun sendOfTheDay(): Boolean {
        // Feature not enabled
        if (!useOfTheDay)
            return false

        // Rank posts
        val dislikeWeight = bot.dataHandler.get<Double>(feature.name, "otd.dislike_weight") ?: 0.0

        // Fetch messages
        val futures = posts.values
            .mapNotNull { channel?.retrieveMessageById(it)?.submit() }

        // No messages
        if (futures.isEmpty()) {
            logger.warn("Tried to post BeReal of the day for guild ${bot.guild.id} but no posts were found!")
            return false
        }

        CompletableFuture.allOf(*futures.toTypedArray()).thenAccept {
            val messages = futures.map { it.getNow(null) }

            // Compute ranks
            val ranked = messages.map { msg ->
                val (up, down) = msg.reactions.fold(0 to 0) { (u, d), r ->
                    when (r.emoji.name) {
                        thumbsUp.name -> (u + r.count) to d
                        thumbsDown.name -> u to (d + r.count)
                        else -> u to d
                    }
                }

                val score = up - dislikeWeight * down
                Triple(msg, up, down) to score
            }.sortedByDescending { it.second }

            // Get highest ranked
            val topScore = ranked.firstOrNull()?.second ?: run {
                logger.warn("No valid posts after fetching reactions for guild '${bot.guild.id}'!")
                return@thenAccept
            }
            val topEntries = ranked.filter { it.second == topScore }
            val highest = topEntries.random().first

            // Get channel
            val targetChannel =
                bot.dataHandler.get<String>(feature.name, "otd.channel")
                    ?.let { bot.guild.getTextChannelById(it) }
                    ?: channel
                    ?: run {
                        logger.warn("Tried to send BeReal of the day, but channel was not set!")
                        return@thenAccept
                    }

            // Base embed
            var embed = EmbedBuilder()
                .setTitle(bot.language.translate("feature.be-real.of_the_day.embed.title"))
                .setImage(if (topEntries.size == 1) highest.first.attachments.firstOrNull()?.url else null)
                .color(Color.PRIMARY)
                .withTimestamp()

            // One winner embed
            if (topEntries.size == 1)
                embed = embed.setDescription(bot.language.translate(
                    "feature.be-real.of_the_day.embed.desc",
                    highest.first.author.asMention,
                    max(highest.second - 1, 0).toString(),
                    max(highest.third - 1, 0).toString(),
                    highest.first.jumpUrl
                ))

            // Tie embed
            else
                embed = embed.setDescription(bot.language.translate(
                    "feature.be-real.of_the_day.embed.tie.desc",
                    topEntries.size.toString(),
                    max(highest.second - 1, 0).toString(),
                    max(highest.third - 1, 0).toString(),
                    topEntries.joinToString("\n") { "- ${it.first.first.jumpUrl} (${it.first.first.author.asMention})" }
                ))

            // Send embed
            targetChannel.sendMessageEmbeds(embed.build()).queue()
        }

        return true
    }

    /**
     * Creates the schedule for posting the BeReal of the day
     */
    fun scheduleOfTheDay() {
        if (!useOfTheDay)
            return

        // Cancel old schedule
        feature.tasks.cancelSpecific(ofTheDayTask)

        // Get time
        val time = LocalTime.of(
            bot.dataHandler.get<Int>(this.feature.name, "otd.h") ?: 22,
            bot.dataHandler.get<Int>(this.feature.name, "otd.m") ?: 0
        )
        val initial = Duration.between(LocalTime.now(), time).toSeconds()

        ofTheDayTask = feature.tasks.scheduleAtFixedRate(initial, 24 * 60 * 60, {
            sendOfTheDay()
        })
    }

    /**
     * Takes care of who is allowed to view the BeReal channel
     */
    fun reloadView() {
        val allowed = bot.dataHandler.get<Boolean>(this.feature.name, "view_without_participating") == true
        val viewRole = bot.dataHandler.get<String>(feature.name, "view_role")?.let { bot.guild.getRoleById(it) }

        // Global view allowed
        // Every user can view the channel
        if (allowed) {
            channel?.upsertPermissionOverride(bot.guild.publicRole)
                ?.grant(Permission.VIEW_CHANNEL)
                ?.queue {
                    // Update permissions for rules
                    bot.getFeature<RulesFeature>()?.updatePerms()
                }
        }

        // Global view not allowed
        // Only participants can view
        else {
            // Deny for everyone
            channel?.upsertPermissionOverride(bot.guild.publicRole)
                ?.deny(Permission.VIEW_CHANNEL)
                ?.queue {
                    // Update permissions for rules
                    bot.getFeature<RulesFeature>()?.updatePerms()
                }
        }

        // Let viewers view, but not post
        viewRole?.let {
            channel?.upsertPermissionOverride(it)
                ?.grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_ADD_REACTION)
                ?.deny(Permission.MESSAGE_SEND)
                ?.queue()
        }

        // Give participants full access
        channel?.upsertPermissionOverride(participantRole)
            ?.grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_ADD_REACTION)
            ?.queue()
    }

    var failStreaks: MutableMap<String, Int>
        get() = bot.dataHandler.get<MutableMap<String, Int>>(this.feature.name, "failed_streak") ?: mutableMapOf()
        set(value) = bot.dataHandler.set<BeRealFeature>("failed_streak", value)

    /**
     * Handler for when a member fails a BeReal
     * @param user The user that failed
     */
    private fun onFail(user: User) {
        val fails = failStreaks

        // Keep track of amount of fails
        val numFails = fails.getOrDefault(user.id, 0) + 1
        fails[user.id] = numFails

        // If amount of fails exceeds threshold
        val failsAllowed = bot.dataHandler.get<Int>(this.feature.name, "leave_after_fails") ?: -1
        if (failsAllowed in 1..<numFails) {
            user.openPrivateChannel().queue {
                it.sendMessage(
                    MessageCreateBuilder()
                        .addEmbeds(
                            EmbedBuilder()
                                .withTimestamp()
                                .color(Color.DANGER)
                                .setTitle(bot.language.translate("feature.be-real.notification.quit_due_to_fail.title"))
                                .setDescription(bot.language.translate("feature.be-real.notification.quit_due_to_fail.desc", numFails.toString(), bot.guild.name))
                                .build()
                        )
                        .addComponents(Embeds.DM_ACTION_ROW(this.bot))
                        .build()
                ).queue()
            }

            // Reset lost streak
            fails.remove(user.id)
            failStreaks = fails

            // Remove participant
            removeMember(user)
        }
    }

    /**
     * Ends the currently running BeReal
     */
    fun end() {
        if (!hasActiveBeReal) return

        // Update streaks
        val failed = participants.filter { !posts.keys.contains(it) }
        val numLost = failed.filter { (streaks[it] ?: 0) != 0 }.size
        streaks = streaks.apply {
            posts.keys.forEach {
                this[it] = this.getOrDefault(it, 0) + 1
            }

            failed.forEach {
                this.remove(it)

                bot.jda.retrieveUserById(it).queue { user ->
                    onFail(user)
                }
            }
        }

        // Send end message
        channel?.sendMessage(
            MessageCreateBuilder()
                .addEmbeds(
                    EmbedBuilder()
                        .withTimestamp()
                        .color(Color.PRIMARY)
                        .setTitle(bot.language.translate("feature.be-real.notification.end.title"))
                        .setDescription(bot.language.translate("feature.be-real.notification.end.desc", numLost.toString(), posts.size.toString()))
                        .setFooter(bot.language.translate("feature.be-real.notification.end.footer"))
                        .build()
                )
                .addComponents(
                    ActionRow.of(
                        Button.primary(
                            "${this.feature.name}_btn_leaderboard",
                            bot.language.translate("feature.be-real.notification.end.btn.leaderboard")
                        )
                    ))
                .build()
        )?.queue()

        // Reset
        hasActiveBeReal = false

        // Logging
        logger.info("BeReal has stopped for guild '${bot.guild.id}'")
    }

    /**
     * Starts a BeReal
     */
    fun start() {
        if (hasActiveBeReal) return

        if (channel == null)
            return

        // Time for BeReal
        val time = bot.dataHandler.get<Int>(this.feature.name, "time") ?: 5

        // Send public announcement
        fun get(x: String, vararg args: String): String = bot.language.translate("feature.be-real.notification.${x}", *args)
        channel?.sendMessage(
            MessageCreateBuilder()
                .addEmbeds(
                    EmbedBuilder()
                        .withTimestamp()
                        .color(Color.DANGER)
                        .setTitle(get("public.title"))
                        .setDescription(get("public.desc", time.toString(), participants.size.toString()))
                        .build()
                )
                .addComponents(
                    ActionRow.of(
                        Button.danger(
                            "${this.feature.name}_btn_leave",
                            bot.language.translate("feature.be-real.notification.public.btn.quit")
                        ),
                        Button.primary(
                            "${this.feature.name}_btn_join",
                            bot.language.translate("feature.be-real.notification.public.btn.join")
                        )
                    ))
                .build()
        )?.queue()

        // Send dms
        participants.forEach {
            val user = bot.guild.retrieveMemberById(it).complete()?.user ?: return@forEach

            user.openPrivateChannel().queue { pc ->
                pc.sendMessage(
                    MessageCreateBuilder()
                        .addEmbeds(
                            EmbedBuilder()
                                .withTimestamp()
                                .color(Color.DANGER)
                                .setTitle(get("title"))
                                .setDescription(get("desc", user.asMention, bot.guild.name, channel!!.asMention, time.toString()))
                                .setFooter(get("footer", bot.guild.name))
                                .build()
                        )
                        .addComponents(Embeds.DM_ACTION_ROW(this.bot))
                        .build()
                ).queue()
            }
        }

        hasActiveBeReal = true
        posts.clear()

        // schedule end
        this.feature.tasks.schedule(time.toLong(), {
            end()
        }, TimeUnit.MINUTES)

        // Logging
        logger.info("BeReal has been started for guild '${bot.guild.id}'")
    }

    /**
     * Generates an embed with a leaderboard
     * @param self The user that requested the leaderboard
     */
    fun leaderboard(self: User): MessageEmbed {
        val entries = streaks
            .asSequence()
            .sortedByDescending { it.value }
            .mapNotNull { (id, streak) ->
                val member = try {
                    bot.guild.retrieveMemberById(id).complete()
                } catch (_: Exception) { null } ?: return@mapNotNull null
                
                member to streak
            }
            .toList()

        // Get own rank
        val ownIndex = entries.indexOfFirst { it.first.id == self.id }
        val ownStreak = if (ownIndex != -1) entries[ownIndex].second else 0
        val ownRank = if (ownIndex != -1) ownIndex + 1 else "`<NONE>`"

        // Get top 3
        val top = entries.take(3)
        fun topMention(i: Int) = top.getOrNull(i)?.first?.asMention ?: "/"
        fun topStreak(i: Int) = top.getOrNull(i)?.second?.toString() ?: "/"

        // Get rest of the leaderboard
        var i = 3
        val rest = entries.drop(3)
            .take(7) // only show top 10
            .joinToString {
                i += 1
                "\n${bot.language.translate("feature.be-real.notification.leaderboard.line", i.toString(), it.first.asMention, it.second.toString())}"
            }

        return EmbedBuilder()
            .color(Color.PRIMARY)
            .setTitle(bot.language.translate("feature.be-real.notification.leaderboard.title"))
            .setDescription(bot.language.translate(
                "feature.be-real.notification.leaderboard.desc",
                topMention(0), topStreak(0),
                topMention(1), topStreak(1),
                topMention(2), topStreak(2),
                rest,
                "#$ownRank",
                ownStreak.toString()
            ))
            .withTimestamp()
            .build()
    }

    /**
     * Schedules a BeReal for this day
     * @param hour The hour the BeReal happens in
     * @param min The minute the BeReal starts
     */
    fun schedule(hour: Int, min: Int) {
        if (!isEnabled) return

        val time = LocalTime.of(hour, min)
        val now = LocalTime.now()

        // Already over
        if (now.isAfter(time)) return

        // Schedule
        this.feature.tasks.schedule(Duration.between(now, time).toSeconds(), {
            start()
        })
    }

    /**
     * Clears all scheduled BeReals
     */
    fun clearScheduled() {
        this.feature.tasks.cancelAll()

        // Need to do this here again because .clearAll also removed this task
        scheduleDailyReload()
        scheduleOfTheDay()
    }

    /**
     * Registers new BeReal times
     */
    fun reload(): List<LocalDateTime> {
        clearScheduled()

        if (!isEnabled)
            return listOf()

        val times = BeRealUtils.generateTimes(this.feature)
        val now = LocalDateTime.now()

        times.filter { it.isAfter(now) }.forEach { time ->
            val delay = Duration.between(now, time).toSeconds()

            this.feature.tasks.schedule(delay, {
                start()
            })
        }

        // Logging
        logger.info("Scheduled BeReal times for guild '${bot.guild.id}': ${times.joinToString(", ") { "${it.hour}:${it.minute}" }}")

        return times
    }

    /**
     * Reschedules BeReal times daily
     */
    fun scheduleDailyReload() {
        if (!isEnabled) return

        val now = LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()

        val initialDelay = Duration.between(now, midnight).toMillis()

        this.feature.tasks.scheduleAtFixedRate(initialDelay, TimeUnit.DAYS.toMillis(1), {
            // Schedule new times
            reload()

            // Clear posts list
            // posts.clear()

            // Recalculate channel view
            reloadView()
        }, TimeUnit.MILLISECONDS)
    }
}