package de.c4vxl.bot.feature.tickets

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Embeds
import de.c4vxl.enums.Permission
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.modals.Modal

/**
 * Feature for opening and managing tickets to server staff
 */
class TicketFeature(bot: Bot) : Feature<TicketFeature>(bot, TicketFeature::class.java) {
    val handler: TicketFeatureHandler = TicketFeatureHandler(this)

    init {
        registerCommands()

        bot.componentHandler.registerButton("${this@TicketFeature.name}_create_ticket") { event ->
            if (event.member?.hasPermission(Permission.FEATURE_TICKETS_OPEN, bot) != true) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerButton
            }

            event.replyModal(Modal.create(
                "${this@TicketFeature.name}_create",
                bot.language.translate("feature.tickets.modal.title")
            )
                .addComponents(
                    Label.of(
                        bot.language.translate("feature.tickets.modal.name"),
                        TextInput.create("${this@TicketFeature.name}_create_name", TextInputStyle.SHORT).build()
                    ),
                    Label.of(
                        bot.language.translate("feature.tickets.modal.desc"),
                        bot.language.translate("feature.tickets.modal.desc.desc"),
                        TextInput.create("${this@TicketFeature.name}_create_desc", TextInputStyle.PARAGRAPH).build()
                    )
                )
                .build()
            ).queue()
        }

        bot.componentHandler.registerModal("${this@TicketFeature.name}_create") { event ->
            val name = event.getValue("${this@TicketFeature.name}_create_name")!!.asString
            val description = event.getValue("${this@TicketFeature.name}_create_desc")!!.asString

            val ticket = handler.open(name, description, event.user)

            event.replyEmbeds(
                Embeds.SUCCESS(bot)
                    .setTitle(bot.language.translate("feature.tickets.success.opened.title"))
                    .setDescription(bot.language.translate("feature.tickets.success.opened.desc", ticket.asMention))
                    .build()
            ).setEphemeral(true).queue()
        }

        bot.componentHandler.registerButton("${name}_button_delete") { event ->
            if (!checkManagePerms(event)) return@registerButton

            handler.delete(event.channel.asTextChannel())
        }

        bot.componentHandler.registerButton("${name}_button_save") { event ->
            if (!checkManagePerms(event)) return@registerButton

            // Save
            handler.save(event.channel.asTextChannel())

            // Send confirmation
            event.replyEmbeds(
                Embeds.SUCCESS(bot)
                    .setDescription(bot.language.translate("feature.tickets.success.saved"))
                    .build()
            ).setEphemeral(true).queue()
        }

        bot.componentHandler.registerButton("${name}_button_reopen") { event ->
            if (!checkManagePerms(event)) return@registerButton

            // Save
            handler.reopen(event.channel.asTextChannel())

            // Delete message
            event.message.delete().queue()

            // Send confirmation
            event.replyEmbeds(
                Embeds.SUCCESS(bot)
                    .setDescription(bot.language.translate("feature.tickets.success.reopen"))
                    .build()
            ).setEphemeral(true).queue()
        }
    }

    override fun registerCommands() {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("ticket", "Play ping-pong with the bot!")
                .addSubcommands(
                    // /ticket open <name> [desc]
                    SubcommandData("open", bot.language.translate("feature.tickets.command.open.desc"))
                        .addOption(OptionType.STRING, "name", bot.language.translate("feature.tickets.command.open.name.desc"), true)
                        .addOption(OptionType.STRING, "description", bot.language.translate("feature.tickets.command.open.description.desc"), true),

                    // /ticket close [ticket]
                    SubcommandData("close", bot.language.translate("feature.tickets.command.close.desc"))
                        .addOption(OptionType.CHANNEL, "ticket", bot.language.translate("feature.tickets.command.close.ticket.desc")),

                    // /ticket button <label>
                    SubcommandData("button", bot.language.translate("feature.tickets.command.buttons.desc"))
                        .addOption(OptionType.STRING, "label", bot.language.translate("feature.channel.command.buttons.label.desc"), true)
                )

        ) { event ->
            when (event.subcommandName) {
                "button" -> {
                    // Check for permissions
                    if (event.member?.hasPermission(Permission.FEATURE_TICKETS_MANAGE, bot) != true) {
                        event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // Send buttons
                    event.channel.sendMessageComponents(
                        ActionRow.of(mutableListOf<ActionRowChildComponent?>().apply {
                            this.add(Button.primary("${this@TicketFeature.name}_create_ticket", event.getOption("label", OptionMapping::getAsString)!!))
                        })
                    ).queue()

                    // Send feedback
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.tickets.command.success.buttons"))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "open" -> {
                    // Check for permission
                    if (event.member?.hasPermission(Permission.FEATURE_TICKETS_OPEN, bot) != true) {
                        event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    val name = event.getOption("name", OptionMapping::getAsString) ?: return@registerSlashCommand
                    val description = event.getOption("description", OptionMapping::getAsString) ?: return@registerSlashCommand

                    val ticket = handler.open(name, description, event.user)

                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setTitle(bot.language.translate("feature.tickets.success.opened.title"))
                            .setDescription(bot.language.translate("feature.tickets.success.opened.desc", ticket.asMention))
                            .build()
                    ).queue()
                }

                "close" -> {
                    // Check for permissions
                    if (event.member?.hasPermission(Permission.FEATURE_TICKETS_MANAGE, bot) != true) {
                        event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    val ticket: GuildChannel = event.getOption("ticket", OptionMapping::getAsChannel)
                        ?: event.guildChannel

                    if (ticket.type != ChannelType.TEXT) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.tickets.command.error.not_a_ticket"))
                                .build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // delete ticket
                    if (!handler.delete(bot.guild.getTextChannelById(ticket.id) ?: return@registerSlashCommand)) {
                        event.replyEmbeds(Embeds.FAILURE(bot)
                            .setDescription(bot.language.translate("feature.tickets.command.error.not_a_ticket"))
                            .build()).setEphemeral(true).queue()
                    }

                    // Send confirmation
                    if (event.channel.id != ticket.id)
                        event.replyEmbeds(Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.tickets.command.success.delete"))
                            .build()).setEphemeral(true).queue()
                }
            }
        }
    }

    private fun checkManagePerms(event: ButtonInteractionEvent): Boolean {
        if (event.member?.hasPermission(Permission.FEATURE_TICKETS_MANAGE, bot) != true) {
            event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
            return false
        }
        return true
    }
}