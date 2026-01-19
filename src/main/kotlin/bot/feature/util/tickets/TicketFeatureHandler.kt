package de.c4vxl.bot.feature.util.tickets

import de.c4vxl.bot.Bot
import de.c4vxl.config.enums.Color
import de.c4vxl.utils.ChannelUtils.deleteIfEmpty
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.PermissionUtils.rolesWithPermission
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.util.*

/**
 * Class for handling the data of the TicketFeature
 * @see de.c4vxl.bot.feature.tickets.TicketFeature
 */
class TicketFeatureHandler(val feature: TicketFeature) {
    private val bot: Bot = feature.bot

    /**
     * Returns the category where tickets will be created
     * @param type The type (open or saved)
     */
    private fun getCategory(type: String = "open"): Category {
        val name: String = bot.dataHandler.get<String>(feature.name, "${type}_category") // get from data store
            ?: bot.language.translate("feature.tickets.default_name.category.${type}") // default

        // find category
        return bot.guild.getCategoriesByName(name, false)
            .firstOrNull() ?:

        // or create it
        bot.guild.createCategory(name)
            .addPermissionOverride(bot.guild.publicRole, mutableListOf(), mutableListOf(Permission.VIEW_CHANNEL))
            .apply {
                bot.rolesWithPermission(
                    de.c4vxl.config.enums.Permission.FEATURE_TICKETS_VIEW,
                    de.c4vxl.config.enums.Permission.FEATURE_TICKETS_MANAGE
                ).forEach {
                    this.addRolePermissionOverride(it.idLong, mutableListOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), mutableListOf())
                }
            }
            .complete()
    }

    private fun getEmbedField(id: String, vararg args: String): String =
        bot.language.translate("feature.tickets.embed.${id}", *args)

    /**
     * Opens a ticket
     * @param title The title of the ticket
     * @param description A description of the issue
     * @param owner The owner of the ticket
     */
    fun open(title: String, description: String, owner: User): TextChannel {
        return getCategory("open")
            .createTextChannel(bot.language.translate("feature.tickets.default_name.channel", owner.name))

            // Hide channel from other members
            .addPermissionOverride(bot.guild.publicRole, mutableListOf(), mutableListOf(Permission.VIEW_CHANNEL))

            // Give owner access
            .addMemberPermissionOverride(owner.idLong, mutableListOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), mutableListOf())

            // Give roles with view permissions access
            .apply {
                bot.rolesWithPermission(
                    de.c4vxl.config.enums.Permission.FEATURE_TICKETS_VIEW,
                    de.c4vxl.config.enums.Permission.FEATURE_TICKETS_MANAGE
                ).forEach {
                    this.addRolePermissionOverride(it.idLong, mutableListOf(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), mutableListOf())
                }
            }

            .complete().apply {
                // Send start message
                this.sendMessage(
                    MessageCreateBuilder()
                        .addEmbeds(
                            EmbedBuilder()
                                .color(Color.PRIMARY)
                                .setTitle(getEmbedField("title", this.id))
                                .setDescription(getEmbedField("desc", owner.asMention))
                                .addField(getEmbedField("fields.name"), title, false)
                                .addField(getEmbedField("fields.desc"), description.ifEmpty { "`None`" }, false)
                                .addField(getEmbedField("fields.by"), owner.asMention, true)
                                .addField(getEmbedField("fields.time"), Date().toInstant().toString(), true)
                                .setThumbnail(owner.avatarUrl)
                                .build()
                        )
                        .addComponents(
                            ActionRow.of(
                                Button.danger("${feature.name}_button_delete", bot.language.translate("feature.tickets.embed.btn.delete")),
                                Button.primary("${feature.name}_button_save", bot.language.translate("feature.tickets.embed.btn.save"))
                            )
                        )
                        .build()
                ).queue {
                    // Pin message
                    this.pinMessageById(it.id).queue()
                }
            }
    }

    /**
     * Deletes a ticket
     * @param ticket The ticket channel
     */
    fun delete(ticket: TextChannel): Boolean {
        val category = ticket.parentCategory
        val openCategory = getCategory("open")
        val savedCategory = getCategory("saved")
        if (!(openCategory.id == category?.id || savedCategory.id == category?.id))
            return false

        // Delete ticket
        ticket.delete().queue {
            // Delete parent category if empty
            openCategory.deleteIfEmpty()?.queue()
            savedCategory.deleteIfEmpty()?.queue()
        }

        return true
    }

    /**
     * Saves a ticket
     * @param ticket The ticket channel
     */
    fun save(ticket: TextChannel) {
        // Hide ticket from op
        ticket.permissionContainer
            .memberPermissionOverrides
            .forEach { it.manager.deny(Permission.VIEW_CHANNEL).queue() }

        // Move
        val category = ticket.parentCategory
        ticket.manager
            .setParent(getCategory("saved"))
            .queue {
                // Delete open category if empty
                category?.deleteIfEmpty()?.queue()
            }

        // Send embed
        ticket.sendMessage(
            MessageCreateBuilder()
                .addEmbeds(
                    EmbedBuilder()
                        .color(Color.SUCCESS)
                        .setTitle(getEmbedField("saved.title"))
                        .setDescription(getEmbedField("saved.desc"))
                        .build()
                )
                .addComponents(
                    ActionRow.of(
                        Button.danger("${feature.name}_button_delete", bot.language.translate("feature.tickets.embed.btn.delete")),
                        Button.primary("${feature.name}_button_reopen", bot.language.translate("feature.tickets.embed.btn.reopen"))
                    )
                )
                .build()
        ).queue()
    }

    /**
     * Opens a saved ticket
     * @param ticket The ticket channel
     */
    fun reopen(ticket: TextChannel) {
        // Show ticket to op again
        ticket.permissionContainer
            .memberPermissionOverrides
            .forEach { it.manager.grant(Permission.VIEW_CHANNEL).queue() }

        // Move
        ticket.manager
            .setParent(getCategory("open"))
            .queue {
                // Delete open category if empty
                getCategory("saved").deleteIfEmpty()?.queue()
            }
    }
}