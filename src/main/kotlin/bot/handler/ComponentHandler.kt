package de.c4vxl.bot.handler

import de.c4vxl.bot.Bot
import de.c4vxl.utils.LoggerUtils.createLogger
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.Logger

/**
 * Handler class for registering custom button click events on a per-guild basis
 */
class ComponentHandler(
    val bot: Bot,
    val logger: Logger = createLogger<ComponentHandler>()
) {
    // List of all registered handlers
    private val buttonHandlers: MutableList<Pair<String, (ButtonInteractionEvent) -> Unit>> = mutableListOf()
    private val modalHandlers: MutableList<Pair<String, (ModalInteractionEvent) -> Unit>> = mutableListOf()

    init {
        logger.info("Initializing button handler for guild '${bot.guild.id}'")

        // Register listener
        bot.jda.addEventListener(object : ListenerAdapter() {
            // Handle buttons
            override fun onButtonInteraction(event: ButtonInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return

                buttonHandlers.forEach {
                    if (it.first == event.componentId)
                        it.second.invoke(event)
                }
            }

            // Handle modals
            override fun onModalInteraction(event: ModalInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return

                modalHandlers.forEach {
                    if (it.first == event.modalId)
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
    fun registerButton(componentId: String, handler: (ButtonInteractionEvent) -> Unit) {
        buttonHandlers.add(Pair(componentId, handler))
    }

    /**
     * Register a modal handler
     * @param componentId The component id of the modals the handler should be active for
     * @param handler The handler
     */
    fun registerModal(componentId: String, handler: (ModalInteractionEvent) -> Unit) {
        modalHandlers.add(Pair(componentId, handler))
    }
}