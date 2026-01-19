package de.c4vxl.bot.feature.settings

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Color
import de.c4vxl.enums.Embeds
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import java.util.*

/**
 * The command for configuring the permission system
 */
class PermissionFeature(bot: Bot) : Feature<PermissionFeature>(bot, PermissionFeature::class.java) {
    init {
        registerCommands()
    }

    override fun registerCommands() {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("permissions", bot.language.translate("feature.perms.command.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                    // /permissions list
                    SubcommandData("list", bot.language.translate("feature.perms.command.list.desc")),

                    // /permissions show <role>
                    SubcommandData("show", bot.language.translate("feature.perms.command.show.desc"))
                        .addOption(OptionType.ROLE, "role", bot.language.translate("feature.perms.command.show.role.desc"), true),

                    // /permissions set <role> [perms]
                    SubcommandData("set", bot.language.translate("feature.perms.command.set.desc"))
                        .addOption(OptionType.ROLE, "role", bot.language.translate("feature.perms.command.set.role.desc"), true)
                        .apply {
                            de.c4vxl.enums.Permission.entries.forEach { permission ->
                                this.addOption(
                                    OptionType.BOOLEAN,
                                    permission.name.lowercase(Locale.getDefault()),
                                    bot.language.translate("perms.${permission.name.lowercase(Locale.getDefault())}.desc")
                                )
                            }
                        }
                )
        ) { event ->
            when (event.subcommandName) {
                "list" -> {
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle(bot.language.translate("feature.perms.command.list.embed.title"))
                            .setDescription(
                                "- " + de.c4vxl.enums.Permission.entries
                                    .joinToString("\n- ") {
                                        it.name.lowercase(Locale.getDefault())
                                    })
                            .withTimestamp()
                            .color(Color.PRIMARY)
                            .build()
                    ).setEphemeral(true).queue()
                }

                "show" -> {
                    val role = event.getOption("role")!!.asRole
                    val permissions = bot.permissionHandler.get(role)

                    val embed = EmbedBuilder()
                        .color(Color.PRIMARY)
                        .withTimestamp()
                        .setTitle(bot.language.translate("feature.perms.command.show.embed.title", role.asMention))

                    if (permissions.isEmpty())
                        embed.setDescription(
                            bot.language.translate(
                                "feature.perms.command.show.embed.empty",
                                role.name
                            )
                        )
                    else {
                        embed.setDescription(
                            bot.language.translate(
                                "feature.perms.command.show.embed.desc",
                                role.asMention
                            )
                        )
                        embed.addField(
                            "- " + permissions
                                .joinToString("\n- ") {
                                    it.name.lowercase(Locale.getDefault())
                                },
                            "", false
                        )
                    }

                    event.replyEmbeds(embed.build()).setEphemeral(true).queue()
                }

                "set" -> {
                    val role = event.getOption("role")!!.asRole

                    val permissions = event.getOptionsByType(OptionType.BOOLEAN)

                    permissions.forEach { opt ->
                        de.c4vxl.enums.Permission.valueOf(opt.name.uppercase(Locale.getDefault())).let {
                            bot.permissionHandler.set(role, it, opt.asBoolean)
                        }
                    }

                    if (permissions.isEmpty())
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.perms.command.set.failure"))
                                .build()
                        ).setEphemeral(true).queue()
                    else
                        event.replyEmbeds(
                            Embeds.SUCCESS(bot)
                                .setDescription(
                                    bot.language.translate(
                                        "feature.perms.command.set.success",
                                        role.asMention
                                    )
                                )
                                .addField(
                                    bot.language.translate("feature.perms.command.set.success.r1"),
                                    bot.language.translate("feature.perms.command.set.success.r2"),
                                    false
                                )
                                .apply {
                                    permissions.forEach {
                                        this.addField(it.name, it.asBoolean.toString(), false)
                                    }
                                }
                                .build()
                        ).setEphemeral(true).queue()
                }
            }
        }
    }
}