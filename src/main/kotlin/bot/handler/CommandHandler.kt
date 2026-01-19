package de.c4vxl.bot.handler

import de.c4vxl.bot.Bot
import de.c4vxl.utils.LoggerUtils.createLogger
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.slf4j.Logger

/**
 * Class for handling guild-specific commands
 */
class CommandHandler(
    val bot: Bot,
    val logger: Logger = createLogger<CommandHandler>()
) {
    init {
        logger.info("Initializing command handler for guild '${bot.guild.id}'")
    }

    // Registered commands
    private val slashCommands: MutableMap<SlashCommandData, (SlashCommandInteractionEvent) -> Unit> = mutableMapOf()

    /**
     * Unregisters all commands
     */
    fun unregisterAll() {
        slashCommands.clear()
    }

    /**
     * Register a slash command to the guild
     * @param command The command to register
     * @param handler The handler to handle the command when run by a user
     */
    fun registerSlashCommand(command: SlashCommandData, handler: (SlashCommandInteractionEvent) -> Unit) {
        slashCommands[command] = handler
    }

    /**
     * Pushes the commands to the guild
     */
    fun update() {
        logger.info("Pushing commands to '${bot.guild.id}'")

        bot.guild.updateCommands()
            .addCommands(slashCommands.keys)
            .queue()
    }

    /**
     * Initializes command handlers
     */
    fun initHandlers() {
        bot.jda.addEventListener(object : ListenerAdapter() {
            // Handle slash commands
            override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return

                // Invoke handler
                slashCommands[slashCommands.keys.find { it.name == event.name }]
                    ?.invoke(event)
            }
        })
    }
}