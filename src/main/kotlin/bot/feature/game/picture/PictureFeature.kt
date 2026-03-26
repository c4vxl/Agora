package de.c4vxl.bot.feature.game.picture

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.Feature
import de.c4vxl.bot.feature.game.picture.api.PictureFeatureAPIResponse
import de.c4vxl.config.enums.Embeds
import de.c4vxl.config.enums.Permission
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

/**
 * Feature for fetching random pictures
 */
class PictureFeature(bot: Bot) : Feature<PictureFeature>(bot, PictureFeature::class.java) {
    val handler = PictureFeatureHandler(this)

    init {
        registerCommands()

        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onButtonInteraction(event: ButtonInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return

                if (event.componentId.startsWith("${name}_delete_")) {
                    val allowed = event.componentId.endsWith(event.user.id) || event.componentId.endsWith("null")

                    if (!allowed) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.picture.embed.reply.delete_btn.error"))
                                .build()
                        ).setEphemeral(true).queue()
                        return
                    }

                    event.channel.deleteMessageById(event.messageId).queue()
                }
            }
        })
    }

    override fun registerCommands() {
        fun commandDesc(name: String) =
            bot.language.translate(
                "feature.picture.command.type.desc",
                bot.language.translate("feature.picture.type.$name").lowercase()
            )

        fun queryOption(subcommandData: SubcommandData): SubcommandData =
            subcommandData.apply { addOption(OptionType.STRING, "query", bot.language.translate("feature.picture.command.type.query.desc")) }

        bot.commandHandler.registerSlashCommand(
            Commands.slash("picture", bot.language.translate("feature.picture.command.desc"))
                .addSubcommands(
                    queryOption(SubcommandData("cat", commandDesc("cat"))),
                    SubcommandData("dog", commandDesc("dog")),
                    SubcommandData("fish", commandDesc("fish")),

                    queryOption(SubcommandData("search", bot.language.translate("feature.picture.command.unsplash.desc")))
                )
        ) { event ->
            val queries = event.getOption("query", OptionMapping::getAsString)
                ?.replace("; ", ";")
                ?.split(";")
                ?.toTypedArray()
                ?: arrayOf()

            // Check for permission
            if (event.member?.hasPermission(Permission.FEATURE_PICTURE_USE, bot) != true) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            when (event.subcommandName) {
                "cat" -> {
                    event.deferReply().queue()
                    sendReply(
                        handler.publicAPIs.cat(),
                        event, event.user
                    )
                }

                "dog" -> {
                    event.deferReply().queue()
                    sendReply(
                        handler.publicAPIs.dog(),
                        event, event.user
                    )
                }

                "fish" -> handleUnsplashRequest(event, "fish")
                "search" -> handleUnsplashRequest(event, *queries)
            }
        }

        bot.commandHandler.registerSlashCommand(
            Commands.slash("picture-of-the-day", bot.language.translate("feature.picture_of_the_day.command.desc"))
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(net.dv8tion.jda.api.Permission.ADMINISTRATOR))
                .addSubcommands(
                    SubcommandData("trigger", bot.language.translate("feature.picture_of_the_day.command.trigger.desc"))
                )
        ) { event ->
            when (event.subcommandName) {
                "trigger" -> {
                    // Trigger
                    val success = handler.sendPicOfTheDay()

                    if (!success) event.replyEmbeds(
                        Embeds.FAILURE(bot)
                            .setDescription(bot.language.translate("feature.picture_of_the_day.command.trigger.failure"))
                            .build()
                    ).setEphemeral(true).queue()
                    else
                        event.replyEmbeds(
                            Embeds.SUCCESS(bot)
                                .setDescription(bot.language.translate("feature.picture_of_the_day.command.trigger.success"))
                                .build()
                        ).setEphemeral(true).queue()
                }
            }
        }
    }

    /**
     * Handles a command using unsplash
     * @param event The command event
     * @param queries The queries to unsplash
     */
    private fun handleUnsplashRequest(event: SlashCommandInteractionEvent, vararg queries: String) {
        // Check for permission
        if (event.member?.hasPermission(Permission.FEATURE_PICTURE_UNSPLASH, bot) != true) {
            event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
            return
        }

        // Track uses
        val uses = handler.unsplashUses[event.user.id] ?: 0
        handler.unsplashUses[event.user.id] = uses + 1

        // Too many uses
        if (uses > handler.unsplashMaxUsesPerHour && event.member?.hasPermission(Permission.UNSPLASH_UNLIMITED, bot) != true) {
            event.replyEmbeds(Embeds.FAILURE(bot)
                .setDescription(bot.language.translate("feature.picture.embed.unsplash.failure.uses_exceeded", handler.unsplashMaxUsesPerHour.toString()))
                .build()
            ).setEphemeral(true).queue()
            return
        }

        event.deferReply().queue()

        sendReply(
            handler.unsplashAPI.random(*queries),
            event, event.user
        )
    }

    /**
     * Sends an embed with the image
     * @param response The response of the API
     * @param event The command event to reply to
     * @param user The user that requested the image
     */
    private fun sendReply(response: PictureFeatureAPIResponse, event: SlashCommandInteractionEvent, user: User? = null) {
        event.hook.sendMessageEmbeds(response.embed)
            .addComponents(ActionRow.of(
                Button.danger("${name}_delete_${user?.id}", bot.language.translate("feature.picture.embed.reply.delete_btn.label"))
            ))
            .apply {
                response.file?.let { addFiles(it) }
            }
            .setEphemeral(response.ephemeral)
            .queue()
    }
}