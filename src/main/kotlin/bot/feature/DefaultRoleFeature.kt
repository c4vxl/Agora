package de.c4vxl.bot.feature

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Color
import de.c4vxl.enums.Embeds
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import java.util.*

/**
 * A simple feature that allows server admins to configure default roles for newly joined members
 */
class DefaultRoleFeature(bot: Bot) : Feature<DefaultRoleFeature>(bot, DefaultRoleFeature::class.java) {
    var registry: MutableList<Role>
        get() {
            val ids = bot.dataHandler.get<MutableList<String>>(this.name, "list") ?: mutableListOf()
            return ids.mapNotNull { bot.guild.getRoleById(it) }.toMutableList()
        }
        set(value) {
            bot.dataHandler.set(this.name, "list", value.map { it.id }.toMutableList())
        }

    init {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("default-roles", bot.language.translate("feature.default_roles.command.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                    // /default-roles list
                    SubcommandData("list", bot.language.translate("feature.default_roles.command.list.desc")),

                    // /default-roles add
                    SubcommandData("add", bot.language.translate("feature.default_roles.command.add.desc"))
                        .addOption(OptionType.ROLE, "role", bot.language.translate("feature.default_roles.command.add.role.desc"), true),

                    // /default-roles remove
                    SubcommandData("remove", bot.language.translate("feature.default_roles.command.remove.desc"))
                        .addOption(OptionType.ROLE, "role", bot.language.translate("feature.default_roles.command.remove.role.desc"), true),
                )
        ) { event ->
            when (event.subcommandName) {
                "list" -> {
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle(bot.language.translate("feature.default_roles.command.list.embed.title"))
                            .apply {
                                if (registry.isEmpty())
                                    this.setDescription(bot.language.translate("feature.default_roles.command.list.embed.empty.desc"))
                                else this.addField("- " + registry.joinToString("\n- ") { it.name },
                                    "", false)
                            }
                            .withTimestamp()
                            .color(Color.PRIMARY)
                            .build()
                    ).setEphemeral(true).queue()
                }

                "add" -> {
                    val role = event.getOption("role", OptionMapping::getAsRole) ?: return@registerSlashCommand

                    if (role.id != bot.guild.id)
                        registry = registry.apply { add(role) }

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.default_roles.command.add.success", role.asMention))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "remove" -> {
                    val role = event.getOption("role", OptionMapping::getAsRole) ?: return@registerSlashCommand

                    registry = registry.apply { remove(role) }

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.default_roles.command.remove.success", role.asMention))
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
        }

        // Register handler
        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
                if (event.guild.id != bot.guild.id) return

                registry.forEach { bot.guild.addRoleToMember(event.user, it) }
            }
        })
    }
}