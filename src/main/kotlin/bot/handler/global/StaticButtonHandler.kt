package bot.handler.global

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

/**
 * Global handler for common button actions
 */
object StaticButtonHandler {
    fun init(jda: JDA) {
        jda.addEventListener(object : ListenerAdapter() {
            override fun onButtonInteraction(event: ButtonInteractionEvent) {
                when (event.componentId) {
                    "agora_delete_message" -> {
                        if (event.message.isFromGuild)
                            event.message.delete().queue()
                        else
                            event.user.openPrivateChannel().queue {
                                it.deleteMessageById(event.messageId).queue()
                            }
                    }

                    "agora_dm_delete_all" -> {
                        event.user.openPrivateChannel().queue {
                            it.history.retrievePast(100).queue { msgs ->
                                msgs
                                    .filter { m -> m.author.id == jda.selfUser.id }
                                    .forEach { m -> m.delete().queue() }
                            }
                        }
                    }
                }
            }
        })
    }
}