package bot.feature.management.inactivity

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.Feature
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

/**
 * A feature that tracks inactivity and automatically kicks inactive members after n days
 */
class InactivityKickFeature(bot: Bot) : Feature<InactivityKickFeature>(bot, InactivityKickFeature::class.java) {
    val handler = InactivityKickFeatureHandler(this)

    init {
        registerCommands()

        handler.scheduleIncrements()

        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onMessageReceived(event: MessageReceivedEvent) {
                // Wrong guild
                if (!event.isFromGuild || event.guild.id != bot.guild.id) return

                // Reset inactivity
                handler.setInactivity(event.author, null)
            }

            override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
                // Wrong guild
                if (event.guild.id != bot.guild.id) return

                // Reset inactivity
                handler.setInactivity(event.member.user, null)
            }

            override fun onGenericCommandInteraction(event: GenericCommandInteractionEvent) {
                if (!event.isFromGuild || event.guild?.id != bot.guild.id) return

                // Reset inactivity
                handler.setInactivity(event.user, null)
            }

            override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
                if (event.guild.id != bot.guild.id) return

                // Clear entry
                handler.setInactivity(event.user, null)
            }
        })
    }

    override fun registerCommands() {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("inactivity", bot.language.translate("feature.inactivity.command.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))

                .addSubcommands(
                    SubcommandData("list", bot.language.translate("feature.inactivity.command.show.desc"))
                        .addOption(OptionType.USER, "member", bot.language.translate("feature.inactivity.command.show.member.desc")),

                    SubcommandData("settings", bot.language.translate("feature.inactivity.command.settings.desc"))
                        .addOption(OptionType.BOOLEAN, "enabled", bot.language.translate("feature.inactivity.command.settings.enabled.desc"))
                        .addOption(OptionType.BOOLEAN, "add-rejoin-link", bot.language.translate("feature.inactivity.command.settings.rejoin.desc"))
                        .addOption(OptionType.INTEGER, "kick-after", bot.language.translate("feature.inactivity.command.settings.kick_after.desc"))
                )
        ) { event ->
            when (event.subcommandName) {
                "settings" -> {
                    val enabled = event.getOption("enabled", OptionMapping::getAsBoolean)
                    val addRejoinLink = event.getOption("add-rejoin-link", OptionMapping::getAsBoolean)
                    val kickAfter = event.getOption("kick-after", OptionMapping::getAsInt)

                    enabled?.let { handler.isEnabled = it }
                    kickAfter?.let { handler.kickAfter = it }
                    addRejoinLink?.let { handler.rejoinLink = it }

                    event.replyEmbeds(Embeds.SUCCESS(bot)
                        .setDescription(bot.language.translate("feature.inactivity.command.settings.success"))
                        .build()).setEphemeral(true).queue()
                }

                "list" -> {
                    val member = event.getOption("member", OptionMapping::getAsUser)

                    event.deferReply().setEphemeral(true).queue()

                    if (member == null)
                        sendOverview(event)
                    else
                        sendSpecific(member, event)
                }
            }

            // Reload schedule
            handler.scheduleIncrements()
        }
    }

    /**
     * Sends the inactivity time of a specific user
     * @param user The user
     * @param event The command event
     */
    private fun sendSpecific(user: User, event: SlashCommandInteractionEvent) {
        event.hook.sendMessageEmbeds(EmbedBuilder()
            .color(Color.PRIMARY)
            .withTimestamp()
            .setTitle(bot.language.translate("feature.inactivity.embed.specific.title"))
            .setDescription(bot.language.translate("feature.inactivity.embed.specific.desc", user.asMention, handler.getInactivity(user).toString()))
            .build()).queue()
    }

    /**
     * Sends an overview of different users with their inactivity time
     * @param event The command event
     */
    private fun sendOverview(event: SlashCommandInteractionEvent) {
        bot.guild.loadMembers().onSuccess { members ->
            val table = members
                .asSequence()
                .filterNot { it.user.isBot || it.user.isSystem }
                .map { member -> Pair(member, handler.getInactivity(member.user)) }
                .sortedByDescending { it.second }
                .take(100)
                .joinToString("\n") {
                    bot.language.translate("feature.inactivity.embed.overview.desc.table", it.first.asMention, it.second.toString())
                }

            event.hook.sendMessageEmbeds(EmbedBuilder()
                .color(Color.PRIMARY)
                .withTimestamp()
                .setTitle(bot.language.translate("feature.inactivity.embed.overview.title"))
                .setDescription(bot.language.translate("feature.inactivity.embed.overview.desc", table))
                .build()).queue()
        }
    }
}