package de.c4vxl.bot.feature.settings

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.language.Language
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
 * The command for configuring the guild-language
 */
class LanguageFeature(bot: Bot) : Feature<LanguageFeature>(bot, LanguageFeature::class.java) {
    init {
        registerCommands()
    }

    override fun registerCommands() {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("language", bot.language.translate("feature.lang.command.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                    // /language list
                    SubcommandData("list", bot.language.translate("feature.lang.command.list.desc")),

                    // /language set <lang>
                    SubcommandData("set", bot.language.translate("feature.lang.command.set.desc"))
                        .apply {
                            Language.available.forEach {
                                this.addOption(
                                    OptionType.BOOLEAN,
                                    it.lowercase(Locale.getDefault()),
                                    bot.language.translate("feature.lang.command.set.e.desc", it)
                                )
                            }
                        },
                )
        ) { event ->
            when (event.subcommandName) {
                "list" -> {
                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle(bot.language.translate("feature.lang.command.list.embed.title"))
                            .setDescription("- " + Language.available
                                .joinToString("\n- ") {
                                    it.lowercase(Locale.getDefault()) })
                            .withTimestamp()
                            .color(Color.PRIMARY)
                            .build()
                    ).setEphemeral(true).queue()
                }

                "set" -> {
                    val language: String? = event.options
                        .firstOrNull() { it.type == OptionType.BOOLEAN && it.asBoolean }
                        ?.name

                    if (language == null) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.lang.command.set.failure"))
                                .build()
                        ).setEphemeral(true).queue()

                        return@registerSlashCommand
                    }

                    bot.dataHandler.set("metadata", "lang", language)
                    bot.language = Language.fromName(language)

                    // Reload commands
                    bot.reloadCommands()

                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.lang.command.set.success", language))
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
        }
    }
}