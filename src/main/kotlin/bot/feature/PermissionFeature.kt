package de.c4vxl.bot.feature

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enum.Color
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
        bot.commandHandler.registerSlashCommand(
            Commands.slash("permissions", bot.language.translate("feature.perms.command.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                    // /permissions list
                    SubcommandData("list", bot.language.translate("feature.perms.command.list.desc")),

                    // /permissions set <role> [perms]
                    SubcommandData("set", bot.language.translate("feature.perms.command.set.desc"))
                        .addOption(OptionType.ROLE, "role", bot.language.translate("feature.perms.command.set.role.desc"), true)
                        .apply {
                            de.c4vxl.enum.Permission.entries.forEach { permission ->
                                this.addOption(
                                    OptionType.BOOLEAN,
                                    permission.name.lowercase(Locale.getDefault()),
                                    bot.language.translate("feature.perms.command.set.perm.desc", permission.name)
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
                            .addField("- " + de.c4vxl.enum.Permission.entries
                                .joinToString("\n- ") {
                                    it.name.lowercase(Locale.getDefault()) },
                                "", false)
                            .withTimestamp()
                            .color(Color.PRIMARY)
                            .build()
                    ).setEphemeral(true).queue()
                }

                "set" -> {
                    val role = event.getOption("role")!!.asRole

                    val permissions = event.getOptionsByType(OptionType.BOOLEAN)

                    permissions.forEach { opt ->
                        de.c4vxl.enum.Permission.fromName(opt.name)?.let {
                            bot.permissionHandler.set(role, it, opt.asBoolean)
                        }
                    }

                    if (permissions.isEmpty())
                        event.replyEmbeds(
                            EmbedBuilder()
                                .setTitle(bot.language.translate("global.title.failure"))
                                .setDescription(bot.language.translate("feature.perms.command.set.failure.desc"))
                                .withTimestamp()
                                .color(Color.DANGER)
                                .build()
                        ).setEphemeral(true).queue()

                    else
                        event.replyEmbeds(
                            EmbedBuilder()
                                .setTitle(bot.language.translate("global.title.success"))
                                .setDescription(bot.language.translate("feature.perms.command.set.success.desc", role.asMention))
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
                                .withTimestamp()
                                .color(Color.SUCCESS)
                                .build()
                        ).setEphemeral(true).queue()
                }
            }
        }
    }
}