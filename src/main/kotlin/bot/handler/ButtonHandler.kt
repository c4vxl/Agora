package de.c4vxl.bot.handler

import de.c4vxl.bot.Bot
import de.c4vxl.utils.LoggerUtils.createLogger
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger

/**
 * Handler class for registering custom button click events on a per-guild basis
 */
class ButtonHandler(
    val bot: Bot,
    val logger: Logger = createLogger<CommandHandler>()
) {
    // List of all registered handlers
    private val handlers: MutableList<Pair<String, (ButtonInteractionEvent) -> Unit>> = mutableListOf()

    init {
        logger.info("Initializing button handler for guild '${bot.guild.id}'")

        // Register listener
        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onButtonInteraction(event: ButtonInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return

                handlers.forEach {
                    if (it.first == event.componentId)
                        it.second.invoke(event)
                }
            }
        })
    }

    /**
     * Register a button handler
     * @param componentId The component id of the buttons the handler should be active for
     * @param handler The handler
     */
    fun register(componentId: String, handler: (ButtonInteractionEvent) -> Unit) {
        handlers.add(Pair(componentId, handler))
    }
}