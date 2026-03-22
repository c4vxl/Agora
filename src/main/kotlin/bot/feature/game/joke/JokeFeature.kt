package de.c4vxl.bot.feature.game.joke

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.Feature
import de.c4vxl.bot.feature.game.joke.api.Joke
import de.c4vxl.bot.feature.game.joke.api.JokesAPI
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.utils.EmbedUtils.color
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands

/**
 * A feature for generating jokes
 */
class JokeFeature(bot: Bot) : Feature<JokeFeature>(bot, JokeFeature::class.java) {
    init {
        registerCommands()

        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onButtonInteraction(event: ButtonInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return

                if (event.componentId.startsWith("${name}_delete_")) {
                    val allowed = event.componentId.endsWith(event.user.id)

                    if (!allowed) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.joke.reply.embed.delete_btn.error"))
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
        bot.commandHandler.registerSlashCommand(
            Commands.slash("joke", bot.language.translate("feature.joke.command.desc"))
                .addOption(OptionType.STRING, "category", bot.language.translate("feature.joke.command.category.desc"))
                .addOption(OptionType.BOOLEAN, "ephemeral", bot.language.translate("feature.joke.command.ephemeral.desc"))
        ) { event ->
            val ephemeral = event.getOption("ephemeral", OptionMapping::getAsBoolean) ?: false

            // Defer reply
            event.deferReply(ephemeral).queue()

            // Fetch joke
            val joke = JokesAPI.random()

            // Reply
            reply(joke, ephemeral, event)
        }
    }

    /**
     * Sends the reply to the command
     * @param joke The joke
     * @param event The command event
     */
    private fun reply(joke: Joke?, ephemeral: Boolean, event: SlashCommandInteractionEvent) {
        // Failed to fetch joke
        if (joke == null) {
            event.hook.sendMessageEmbeds(Embeds.FAILURE(bot)
                .setDescription(bot.language.translate("feature.joke.reply.embed.failure.desc"))
                .build())
                .setEphemeral(true).queue()
            return
        }

        // Send reply
        event.hook.sendMessageEmbeds(EmbedBuilder()
            .setTitle(bot.language.translate("feature.joke.reply.embed.title"))
            .setDescription(bot.language.translate("feature.joke.reply.embed.desc", joke.fullJoke))
            .setAuthor(event.user.name, null, event.user.avatarUrl)
            .color(Color.PRIMARY)
            .build())
            .addComponents(
                ActionRow.of(
                Button.danger("${name}_delete_${event.user.id}", bot.language.translate("feature.joke.reply.embed.delete_btn.label"))
            ))
            .setEphemeral(ephemeral)
            .queue()
    }
}