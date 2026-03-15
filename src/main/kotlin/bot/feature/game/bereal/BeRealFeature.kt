package de.c4vxl.bot.feature.game.bereal

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.Feature
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.config.enums.Permission
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

/**
 * A game feature prompting users to post a picture of what they're doing at the moment at random times a day
 */
class BeRealFeature(bot: Bot) : Feature<BeRealFeature>(bot, BeRealFeature::class.java) {
    val handler: BeRealFeatureHandler = BeRealFeatureHandler(this)

    init {
        registerCommands()

        // Generate new random times every midnight
        handler.scheduleDailyReload()

        // Initial reload to get new times as soon as the feature gets registered
        handler.reload()

        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onMessageReceived(event: MessageReceivedEvent) {
                // Wrong guild
                if (!event.isFromGuild || event.guild.id != bot.guild.id) return

                // No BeReal active
                if (!handler.hasActiveBeReal) return

                // Author is bot
                if (event.author.isBot) return

                // Feature disabled
                if (!handler.isEnabled) return

                // Wrong channel
                if (event.channel.id != handler.channel?.id) return

                // Not participant in BeReal feature
                if (!handler.participants.contains(event.author.id)) return

                // Has already uploaded
                if (handler.successfullyUploaded.contains(event.author.id)) return

                // Get attachment
                val image =
                    event.message.attachments.find { it.isImage }                        // Get image
                        ?: event.message.embeds.find { it.image != null }?.image         // Get image from embeds (common on phones)
                        ?: event.message.embeds.find { it.thumbnail != null }?.thumbnail // Get thumbnail from embeds (as fallback)
                        ?: return                                                        // No image: exit

                // Mark as uploaded
                handler.successfullyUploaded.add(event.member!!.user.id)

                // Confirmation
                event.author.openPrivateChannel().queue { pc ->
                    pc.sendMessage(
                        MessageCreateBuilder()
                            .addEmbeds(
                                EmbedBuilder()
                                    .withTimestamp()
                                    .color(Color.SUCCESS)
                                    .setTitle(bot.language.translate("feature.be-real.notification.success.title"))
                                    .setDescription(bot.language.translate("feature.be-real.notification.success.desc", ((handler.streaks[event.member!!.user.id]?.toString() ?: "0").toInt() + 1).toString(), bot.guild.name))
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

            override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
                if (event.guild.id != bot.guild.id) return

                handler.participants = handler.participants.apply {
                    remove(event.user.id)
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
            handler.participants = handler.participants.apply { remove(event.user.id) }

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
            if (handler.participants.contains(event.user.id)) {
                event.replyEmbeds(
                    Embeds.FAILURE(bot)
                        .setDescription(bot.language.translate("feature.be-real.command.join.error.already"))
                        .build()
                ).setEphemeral(true).queue()
                return@registerButton
            }

            // Add user
            handler.participants = handler.participants.apply { add(event.user.id) }

            // Send confirmation
            event.replyEmbeds(
                Embeds.SUCCESS(bot)
                    .setDescription(bot.language.translate("feature.be-real.command.join.success"))
                    .build()
            ).setEphemeral(true).queue()
        }
        bot.componentHandler.registerButton("${this.name}_btn_leaderboard") { event ->
            val leaderboard = handler.leaderboard(event.user)

            event.replyEmbeds(leaderboard).setEphemeral(true).queue()
        }
    }

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
            if (!handler.isEnabled) {
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
                    val leaderboard = handler.leaderboard(event.user)

                    event.replyEmbeds(leaderboard).setEphemeral(true).queue()
                }

                "reload" -> {
                    val times = handler.reload()
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
                    if (handler.participants.contains(event.user.id)) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.be-real.command.join.error.already"))
                                .build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // Add user
                    handler.participants = handler.participants.apply { add(event.user.id) }

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.join.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "quit" -> {
                    // Remove user
                    handler.participants = handler.participants.apply { remove(event.user.id) }

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.quit.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "end" -> {
                    // stop
                    handler.end()

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.stop.success", handler.participants.size.toString()))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "start" -> {
                    if (handler.channel == null) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.be-real.command.start.error.no_channel"))
                                .build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // start
                    handler.start()

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.be-real.command.start.success", handler.participants.size.toString()))
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
        }
    }
}