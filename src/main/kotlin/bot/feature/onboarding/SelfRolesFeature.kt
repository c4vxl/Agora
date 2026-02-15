package de.c4vxl.bot.feature.onboarding

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.Feature
import de.c4vxl.config.enums.Embeds
import de.c4vxl.config.enums.Permission
import de.c4vxl.utils.PermissionUtils.hasPermission
import de.c4vxl.utils.PermissionUtils.hasRole
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.util.*

/**
 * Allows server-moderators to create modals for users to choose specific roles that should apply to them
 */
class SelfRolesFeature(bot: Bot) : Feature<SelfRolesFeature>(bot, SelfRolesFeature::class.java) {
    init {
        registerCommands()

        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onMessageDelete(event: MessageDeleteEvent) {
                if (event.guild.id != bot.guild.id) return

                val uuid = messages[event.messageId] ?: return

                // Remove modal
                modals = modals.apply { this.remove(uuid) }

                // Remove message
                messages = messages.apply { this.remove(event.messageId) }
            }

            override fun onButtonInteraction(event: ButtonInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return

                // Show modal
                if (event.componentId.startsWith("${this@SelfRolesFeature.name}_button_")) {
                    val uuid = event.componentId.removePrefix("${this@SelfRolesFeature.name}_button_")
                    val roles = modals[uuid] ?: return
                    event.reply(createModal(event.member ?: return, roles)).setEphemeral(true).queue()
                }

                // Modal click
                if (event.componentId.startsWith("${this@SelfRolesFeature.name}_toggle_")) {
                    val roleId = event.componentId.removePrefix("${this@SelfRolesFeature.name}_toggle_")
                    val role = bot.guild.getRoleById(roleId) ?: return

                    val member = event.member ?: return

                    if (member.hasRole(role)) {
                        bot.guild.removeRoleFromMember(event.user, role).queue {
                            // Send confirmation
                            event.replyEmbeds(
                                Embeds.SUCCESS(bot)
                                    .setDescription(bot.language.translate("feature.self-roles.remove.success", role.asMention))
                                    .build()
                            ).setEphemeral(true).queue()
                        }
                    } else {
                        bot.guild.addRoleToMember(member.user, role).queue {
                            // Send confirmation
                            event.replyEmbeds(
                                Embeds.SUCCESS(bot)
                                    .setDescription(bot.language.translate("feature.self-roles.grant.success", role.asMention))
                                    .build()
                            ).setEphemeral(true).queue()
                        }
                    }

                    event.message.delete().queue()
                }
            }
        })
    }

    // Holds the ids of modals with the adjustable roles
    private var modals: MutableMap<String, MutableList<Role>>
        get() {
            val ids = bot.dataHandler.get<MutableMap<String, MutableList<String>>>(this.name, "modals") ?: mutableMapOf()
            return ids.mapValues { it.value.mapNotNull { id -> bot.guild.getRoleById(id) }.toMutableList() }.toMutableMap()
        }
        set(value) {
            val ids = value.mapValues { it.value.map { role -> role.id } }
            bot.dataHandler.set<SelfRolesFeature>("modals", ids)
        }

    // Holds the messages to be able to properly remove the modals from db later
    private var messages: MutableMap<String, String>
        get() = bot.dataHandler.get<MutableMap<String, String>>(this.name, "msgs") ?: mutableMapOf()
        set(value) = bot.dataHandler.set<SelfRolesFeature>("msgs", value)

    override fun registerCommands() {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("self-roles", bot.language.translate("feature.self-roles.command.desc"))
                .addSubcommands(
                    SubcommandData("paste", bot.language.translate("feature.self-roles.paste.command.desc"))
                        .addOption(OptionType.STRING, "roles", bot.language.translate("feature.self-roles.paste.command.desc"), true)
                        .addOption(OptionType.STRING, "label", bot.language.translate("feature.self-roles.label.command.desc"))
                )
        ) { event ->
            // Ensure user has permission
            if (event.member?.hasPermission(Permission.FEATURE_SELF_ROLES_MANAGE, bot) != true) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            val rolesStr = event.getOption("roles", OptionMapping::getAsString) ?: return@registerSlashCommand
            val label = event.getOption("label", OptionMapping::getAsString)
                ?: bot.language.translate("feature.self-roles.button.default.label")

            // Get roles list
            val roles: List<Role> = rolesStr.replace(" ", "").split(">")
                .filter { it.startsWith("<@&") }
                .map { it.removePrefix("<@&") }
                .mapNotNull { bot.guild.getRoleById(it) }

            // Return if no roles passed
            if (roles.isEmpty()) {
                event.replyEmbeds(
                    Embeds.FAILURE(bot)
                        .setDescription(bot.language.translate("feature.self-roles.command.set.error.no_roles"))
                        .build()
                ).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            // Create modal id
            val uuid = UUID.randomUUID().toString()

            // Store roles
            modals = modals.apply {
                this[uuid] = roles.toMutableList()
            }

            // Send message
            event.channel.sendMessage(
                MessageCreateBuilder()
                    .addComponents(ActionRow.of(Button.primary("${this.name}_button_${uuid}", label)))
                    .build()
            ).queue {
                messages = messages.apply { this[it.id] = uuid }
            }

            // Send confirmation
            event.replyEmbeds(
                Embeds.SUCCESS(bot)
                    .setDescription(bot.language.translate("feature.self-roles.paste.command.success",
                        roles.joinToString("\n\\- ") { it.asMention }))
                    .build()
            ).setEphemeral(true).queue()
        }
    }

    @Suppress("LABEL_NAME_CLASH")
    private fun createModal(user: Member, roles: List<Role>): MessageCreateData {
        return MessageCreateBuilder()
            .addComponents(
                roles.chunked(5).map {
                    ActionRow.of(
                        it.map { role ->
                            val hasRole = user.hasRole(role)

                            return@map if (!hasRole) Button.primary("${this.name}_toggle_${role.id}", role.name)
                            else Button.danger("${this.name}_toggle_${role.id}", role.name)
                        }
                    )
                }
            )
            .build()
    }
}