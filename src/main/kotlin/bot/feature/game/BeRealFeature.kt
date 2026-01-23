package de.c4vxl.bot.feature.game

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.Feature
import de.c4vxl.bot.handler.static.Scheduler
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.config.enums.Permission
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * A game feature prompting users to post a picture of what they're doing at the moment at random times a day
 */
class BeRealFeature(bot: Bot) : Feature<BeRealFeature>(bot, BeRealFeature::class.java) {
    // Cache of scheduled tasks
    private val scheduledTasks: MutableList<ScheduledFuture<*>> = mutableListOf()
    private var successfullyUploaded: MutableList<String> = mutableListOf()
    private var hasActiveBeReal = false

    init {
        registerCommands()

        // Generate new random times every midnight
        scheduleReload()

        // Initial reload to get new times as soon as the feature gets registered
        reload()


        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onMessageReceived(event: MessageReceivedEvent) {
                // Wrong guild
                if (!event.isFromGuild || event.guild.id != bot.guild.id) return

                // No BeReal active
                if (!hasActiveBeReal) return

                // Author is bot
                if (event.author.isBot) return

                // Feature disabled
                if (!isEnabled) return

                // Wrong channel
                if (event.channel.id != channel?.id) return

                // Not participant in BeReal feature
                if (!participants.contains(event.author.id)) return

                // Has already uploaded
                if (successfullyUploaded.contains(event.author.id)) return

                // Get attachment
                val image =
                    event.message.attachments.find { it.isImage }                        // Get image
                        ?: event.message.embeds.find { it.image != null }?.image         // Get image from embeds (common on phones)
                        ?: event.message.embeds.find { it.thumbnail != null }?.thumbnail // Get thumbnail from embeds (as fallback)
                        ?: return                                                        // No image: exit

                // Mark as uploaded
                successfullyUploaded.add(event.member!!.user.id)

                // Confirmation
                event.author.openPrivateChannel().queue { pc ->
                    pc.sendMessage(
                        MessageCreateBuilder()
                            .addEmbeds(
                                EmbedBuilder()
                                    .withTimestamp()
                                    .color(Color.SUCCESS)
                                    .setTitle(bot.language.translate("feature.be-real.notification.success.title"))
                                    .setDescription(bot.language.translate("feature.be-real.notification.success.desc", streaks[event.member!!.user.id]?.toString() ?: "0", bot.guild.name))
                                    .setFooter(bot.language.translate("feature.be-real.notification.success.footer", bot.guild.name))
                                    .build()
                            )
                            .addComponents(
                                ActionRow.of(
                                    Button.danger(
                                        "agora_dm_delete_all",
                                        bot.language.translate("global.button.dm_delete_all")
                                    ),
                                    Button.primary(
                                        "agora_delete_message",
                                        bot.language.translate("global.button.delete_msg")
                                    )
                                ))
                            .build()
                    ).queue()
                }
            }
        })

        // Register button events
        bot.componentHandler.registerButton("${this.name}_btn_leave") { event ->
            // Check for permission
            if (event.member?.hasPermission(Permission.FEATURE_BE_REAL, bot) != true &&
                event.member?.hasPermission(Permission.FEATURE_BE_REAL_MANAGE, bot) != true) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerButton
            }

            // Remove user
            participants = participants.apply { remove(event.user.id) }

            // Send confirmation
            event.replyEmbeds(
                Embeds.SUCCESS(bot)
                    .setDescription(bot.language.translate("feature.be-real.command.quit.success"))
                    .build()
            ).setEphemeral(true).queue()
        }
        bot.componentHandler.registerButton("${this.name}_btn_join") { event ->
            // Check for permission
            if (event.member?.hasPermission(Permission.FEATURE_BE_REAL, bot) != true &&
                event.member?.hasPermission(Permission.FEATURE_BE_REAL_MANAGE, bot) != true) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerButton
            }

            // return if already a member
            if (participants.contains(event.user.id)) {
                event.replyEmbeds(
                    Embeds.FAILURE(bot)
                        .setDescription(bot.language.translate("feature.be-real.command.join.error.already"))
                        .build()
                ).setEphemeral(true).queue()
                return@registerButton
            }

            // Add user
            participants = participants.apply { add(event.user.id) }

            // Send confirmation
            event.replyEmbeds(
                Embeds.SUCCESS(bot)
                    .setDescription(bot.language.translate("feature.be-real.command.join.success"))
                    .build()
            ).setEphemeral(true).queue()
        }
        bot.componentHandler.registerButton("${this.name}_btn_leaderboard") { event ->
            val leaderboard = leaderboard(event.user)

            event.replyEmbeds(leaderboard).setEphemeral(true).queue()
        }
    }

    val isEnabled: Boolean
        get() = bot.dataHandler.get<Boolean>(this.name, "enabled") ?: false

    var participants: MutableList<String>
        get() = bot.dataHandler.get<MutableList<String>>(this.name, "participants") ?: mutableListOf()
        set(value) = bot.dataHandler.set(this.name, "participants", value)

    var channel: TextChannel?
        get() {
            val channelName = bot.dataHandler.get<String>(this.name, "channel") ?: return null
            return bot.guild.getTextChannelById(channelName)
        }
        set(value) = bot.dataHandler.set(this.name, "channel", value?.id ?: "")

    var streaks: MutableMap<String, Int>
        get() =
            bot.dataHandler.get<MutableMap<String, Double>>(this.name, "streak")
                ?.mapValues { it.value.toInt() }?.toMutableMap()
                ?: mutableMapOf()
        set(value) = bot.dataHandler.set(this.name, "streak", value)

    override fun registerCommands() {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("be-real", bot.language.translate("feature.be-real.command.desc"))
                .addSubcommands(
                    // /be-real join
                    SubcommandData("join", bot.language.translate("feature.be-real.command.join.desc")),

                    // /be-real quit
                    SubcommandData("quit", bot.language.translate("feature.be-real.command.quit.desc")),

                    // /be-real start
                    SubcommandData("start", bot.language.translate("feature.be-real.command.start.desc")),

                    // /be-real end
                    SubcommandData("end", bot.language.translate("feature.be-real.command.end.desc")),

                    // /be-real reload
                    SubcommandData("reload", bot.language.translate("feature.be-real.command.reload.desc")),

                    // /be-real leaderboard
                    SubcommandData("leaderboard", bot.language.translate("feature.be-real.command.leaderboard.desc")),

                    // /be-real buttons
                    SubcommandData("buttons", bot.language.translate("feature.be-real.command.buttons.desc"))
                        .addOption(OptionType.STRING, "quit_label", bot.language.translate("feature.be-real.command.buttons.quit_label.desc"))
                        .addOption(OptionType.STRING, "join_label", bot.language.translate("feature.be-real.command.buttons.join_label.desc"))
                        .addOption(OptionType.STRING, "board_label", bot.language.translate("feature.be-real.command.buttons.board_label.desc"))
                )
        ) { event ->
            // Check for permission
            if (event.subcommandName != "leaderboard" &&
                event.member?.hasPermission(Permission.FEATURE_BE_REAL, bot) != true &&
                event.member?.hasPermission(Permission.FEATURE_BE_REAL_MANAGE, bot) != true) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            if (
                listOf("reload", "start", "end", "buttons").contains(event.subcommandName) &&
                event.member?.hasPermission(Permission.FEATURE_BE_REAL_MANAGE, bot) != true
            ) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            // Return if not enabled
            if (!isEnabled) {
                event.replyEmbeds(
                    Embeds.FAILURE(bot)
                        .setDescription(bot.language.translate("feature.be-real.command.error.disabled"))
                        .build()
                ).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            when (event.subcommandName) {
                "buttons" -> {
                    val quit = event.getOption("quit_label", OptionMapping::getAsString)
                        ?: bot.language.translate("feature.be-real.notification.public.btn.quit")

                    val join = event.getOption("join_label", OptionMapping::getAsString)
                        ?: bot.language.translate("feature.be-real.notification.public.btn.join")

                    val board = event.getOption("board_label", OptionMapping::getAsString)
                        ?: bot.language.translate("feature.be-real.notification.end.btn.leaderboard")

                    // Send buttons
                    event.channel.sendMessage(
                        MessageCreateBuilder()
                            .addComponents(
                                ActionRow.of(
                                    Button.danger("${this.name}_btn_leave", quit),
                                    Button.success("${this.name}_btn_join", join),
                                    Button.primary("${this.name}_btn_leaderboard", board)
                                ))
                            .build()
                    ).queue()

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.buttons.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "leaderboard" -> {
                    val leaderboard = leaderboard(event.user)

                    event.replyEmbeds(leaderboard).setEphemeral(true).queue()
                }

                "reload" -> {
                    val times = reload()
                    val timesList = times.joinToString("\n- ", prefix = "- ") { "${it.hour}:${it.minute}" }

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.reload.success", timesList))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "join" -> {
                    // return if already a member
                    if (participants.contains(event.user.id)) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.be-real.command.join.error.already"))
                                .build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // Add user
                    participants = participants.apply { add(event.user.id) }

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.join.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "quit" -> {
                    // Remove user
                    participants = participants.apply { remove(event.user.id) }

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.quit.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "end" -> {
                    // stop
                    end()

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.stop.success", participants.size.toString()))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "start" -> {
                    if (channel == null) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.be-real.command.start.error.no_channel"))
                                .build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // start
                    start()

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.start.success", participants.size.toString()))
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
        }
    }

    /**
     * Schedules a reload for every midnight to create new random times
     */
    private fun scheduleReload() {
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()

        val delay = Duration.between(now, nextMidnight).toMillis()

        Scheduler.scheduler.schedule({
            reload()
            scheduleReload()
        }, delay, TimeUnit.MILLISECONDS)
    }

    /**
     * Generates new random times the current day
     */
    private fun reload(): List<LocalDateTime> {
        val times = generateTimes()

        // Clear old schedule
        scheduledTasks.forEach { it.cancel(false) }
        scheduledTasks.clear()

        val now = LocalDateTime.now()

        // Schedule start task to run at times
        times.forEach { time ->
            if (time.isBefore(now)) return@forEach

            val delay = Duration.between(now, time).toMillis()

            scheduledTasks.add(Scheduler.scheduler.schedule({
                start()
            }, delay, TimeUnit.MILLISECONDS))
        }

        // Logging
        logger.info("Scheduling next BeReal times for guild '${bot.guild.id}': ${times.joinToString(", ") { "${it.hour}:${it.minute}" }}")

        return times
    }

    /**
     * Ends the currently running BeReal
     */
    private fun end() {
        if (!hasActiveBeReal) return

        // Update streaks
        val failed = participants.filter { !successfullyUploaded.contains(it) }
        streaks = streaks.apply {
            successfullyUploaded.forEach {
                this[it] = this.getOrDefault(it, 0) + 1
            }

            failed.forEach { this.remove(it) }
        }

        // Send end message
        channel?.sendMessage(
            MessageCreateBuilder()
                .addEmbeds(
                    EmbedBuilder()
                        .withTimestamp()
                        .color(Color.PRIMARY)
                        .setTitle(bot.language.translate("feature.be-real.notification.end.title"))
                        .setDescription(bot.language.translate("feature.be-real.notification.end.desc", failed.size.toString(), successfullyUploaded.size.toString()))
                        .setFooter(bot.language.translate("feature.be-real.notification.end.footer"))
                        .build()
                )
                .addComponents(
                    ActionRow.of(
                        Button.primary(
                            "${this.name}_btn_leaderboard",
                            bot.language.translate("feature.be-real.notification.end.btn.leaderboard")
                        )
                    ))
                .build()
        )?.queue()

        // Reset
        hasActiveBeReal = false
        successfullyUploaded.clear()

        // Logging
        logger.info("BeReal has stopped for guild '${bot.guild.id}'")
    }

    /**
     * Generates an embed with a leaderboard
     * @param self The user that requested the leaderboard
     */
    private fun leaderboard(self: User): MessageEmbed {
        val entries = streaks
            .asSequence()
            .sortedByDescending { it.value }
            .mapNotNull { (id, streak) ->
                val member = bot.guild.retrieveMemberById(id).complete() ?: return@mapNotNull null
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
        var i = -1
        val rest = entries.drop(3)
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
     * Starts a BeReal
     */
    private fun start() {
        if (hasActiveBeReal) return

        fun get(x: String, vararg args: String): String = bot.language.translate("feature.be-real.notification.${x}", *args)

        if (channel == null)
            return

        val time = bot.dataHandler.get<Int>(this.name, "time") ?: 5

        // Send public announcement
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
                            "${this.name}_btn_leave",
                            bot.language.translate("feature.be-real.notification.public.btn.quit")
                        ),
                        Button.primary(
                            "${this.name}_btn_join",
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
                        .addComponents(
                            ActionRow.of(
                            Button.danger(
                                "agora_dm_delete_all",
                                bot.language.translate("global.button.dm_delete_all")
                            ),
                            Button.primary(
                                "agora_delete_message",
                                bot.language.translate("global.button.delete_msg")
                            )
                        ))
                        .build()
                ).queue()
            }
        }

        hasActiveBeReal = true

        // schedule end
        Scheduler.scheduler.schedule({
            end()
        }, time.toLong(), TimeUnit.MINUTES)

        // Logging
        logger.info("BeReal has been started for guild '${bot.guild.id}'")
    }

    /**
     * Generates a list of random times for a specific date
     * @param date The date
     * @param defaultAmount The default amount of posts a day (used if not set in bot config)
     * @param defaultStartHour The default starting hour of a day (used if not set in bot config)
     * @param defaultStartMinute The default starting minute of a day (used if not set in bot config)
     * @param defaultEndHour The default ending hour of a day (used if not set in bot config)
     * @param defaultEndMinute The default ending minute of a day (used if not set in bot config)
     */
    private fun generateTimes(
        date: LocalDate = LocalDate.now(),

        // By default, two posts a day
        defaultAmount: Int = 2,

        // Default start time: 7:30 PM
        defaultStartHour: Int = 7,
        defaultStartMinute: Int = 30,

        // Default end time: 11 PM
        defaultEndHour: Int = 23,
        defaultEndMinute: Int = 0
    ): List<LocalDateTime> {
        // Get start time (default: 7:30 PM)
        val start = LocalTime.of(
            bot.dataHandler.get<Int>(this.name, "start.h") ?: defaultStartHour,
            bot.dataHandler.get<Int>(this.name, "start.m") ?: defaultStartMinute
        ).toSecondOfDay()

        // Get end time (default: 11:00 PM)
        val end = LocalTime.of(
            bot.dataHandler.get<Int>(this.name, "end.h") ?: defaultEndHour,
            bot.dataHandler.get<Int>(this.name, "end.m") ?: defaultEndMinute
        ).toSecondOfDay()

        // Get amount of posts per day (default: 2)
        val num = bot.dataHandler.get<Int>(this.name, "amount") ?: defaultAmount

        // Create list
        return (1..num).map {
            val randomSec = Random.nextInt(start, end)
            LocalDateTime.of(date, LocalTime.ofSecondOfDay(randomSec.toLong()))
        }.sorted()
    }
}