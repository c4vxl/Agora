package de.c4vxl.bot.feature

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enum.Color
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
                            .addField("- " + Language.available
                                .joinToString("\n- ") {
                                    it.lowercase(Locale.getDefault()) },
                                "", false)
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
                            EmbedBuilder()
                                .setTitle(bot.language.translate("global.title.failure"))
                                .setDescription(bot.language.translate("feature.lang.command.set.failure.desc"))
                                .withTimestamp()
                                .color(Color.DANGER)
                                .build()
                        ).setEphemeral(true).queue()

                        return@registerSlashCommand
                    }

                    bot.dataHandler.set("metadata", "lang", language)
                    bot.language = Language.fromName(language)

                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle(bot.language.translate("global.title.success"))
                            .setDescription(bot.language.translate("feature.lang.command.set.success.desc", language))
                            .withTimestamp()
                            .color(Color.SUCCESS)
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
        }
    }
}