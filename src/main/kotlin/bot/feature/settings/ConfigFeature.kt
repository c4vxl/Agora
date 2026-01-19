package de.c4vxl.bot.feature.settings

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.data.Database
import de.c4vxl.config.enums.Embeds
import de.c4vxl.utils.ResourceUtils
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.utils.FileUpload

/**
 * Command for exporting/importing guild-specific config data
 */
class ConfigFeature(bot: Bot) : Feature<ConfigFeature>(bot, ConfigFeature::class.java) {
    init {
        registerCommands()
    }

    override fun registerCommands() {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("config", bot.language.translate("feature.config.command.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                    SubcommandData("export", bot.language.translate("feature.config.command.export.desc")),

                    SubcommandData("import", bot.language.translate("feature.config.command.import.desc"))
                        .addOption(OptionType.ATTACHMENT, "file", bot.language.translate("feature.config.command.import.file.desc"), true),

                    SubcommandData("show", bot.language.translate("feature.config.command.show.desc")),
                )
        ) { event ->
            when (event.subcommandName) {
                "show" -> {
                    event.reply(
                        """
                        ```json${"\n"}${Database.file(event.guild!!.idLong).readText()}```
                        """.trimIndent()
                    ).setEphemeral(true).queue()
                }

                "export" -> {
                    Database.makeDirty(event.guild!!.idLong)
                    Database.save(event.guild!!.idLong)
                    event.replyFiles(
                        FileUpload.fromData(Database.file(event.guild!!.idLong))
                    ).setEphemeral(true).queue()
                }

                "import" -> {
                    val attachment = event.getOption("file", OptionMapping::getAsAttachment) ?: return@registerSlashCommand

                    if (attachment.fileExtension != "db") {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.config.command.import.error.invalid"))
                                .build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // Download to config
                    ResourceUtils.downloadFile(attachment.url, Database.file(event.guild!!.idLong))

                    // Reload config to memory
                    Database.reload(event.guild!!.idLong)

                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.config.command.import.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
        }
    }
}