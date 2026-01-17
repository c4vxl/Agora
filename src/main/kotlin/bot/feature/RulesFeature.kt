package de.c4vxl.bot.feature

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.enums.Color
import de.c4vxl.enums.Embeds
import de.c4vxl.enums.Permission
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

/**
 * Feature for prompting new users with the guild-rules
 */
class RulesFeature(bot: Bot) : Feature<RulesFeature>(bot, RulesFeature::class.java) {
    init {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("rules", bot.language.translate("feature.rules.command.desc"))
                .addSubcommands(
                    SubcommandData("set", bot.language.translate("feature.rules.command.set.desc"))
                        .addOption(OptionType.CHANNEL, "channel", bot.language.translate("feature.rules.command.set.channel.desc"), true)
                        .addOption(OptionType.ROLE, "accepted_role", bot.language.translate("feature.rules.command.set.role.desc"), true)
                        .addOption(OptionType.BOOLEAN, "timestamp", bot.language.translate("feature.rules.command.set.timestamp.desc"))
                        .addOption(OptionType.BOOLEAN, "required", bot.language.translate("feature.rules.command.set.required.desc"))
                        .apply {
                            listOf("title", "image", "description", "footer", "color", "fields").forEach {
                                this.addOption(OptionType.STRING, it, bot.language.translate("feature.rules.command.set.${it}.desc"))
                            }
                        },

                    SubcommandData("remove", bot.language.translate("feature.rules.command.remove.desc"))
                )
        ) { event ->
            // Ensure user has permission
            if (event.member?.hasPermission(Permission.FEATURE_RULES_MOD, bot) != true) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            when (event.subcommandName) {
                "set" -> {
                    // Get config
                    val channel = event.getOption("channel", OptionMapping::getAsChannel) ?: return@registerSlashCommand
                    val role = event.getOption("accepted_role", OptionMapping::getAsRole) ?: return@registerSlashCommand
                    val timestamp = event.getOption("timestamp", OptionMapping::getAsBoolean) ?: true
                    val required = event.getOption("required", OptionMapping::getAsBoolean) ?: true
                    val title = event.getOption("title", OptionMapping::getAsString)
                    val image = event.getOption("image", OptionMapping::getAsString)
                    val description = event.getOption("description", OptionMapping::getAsString)
                    val footer = event.getOption("footer", OptionMapping::getAsString)
                    val color = event.getOption("color", OptionMapping::getAsString)?.removePrefix("#")?.toIntOrNull(16) ?: Color.PRIMARY.asInt
                    val fields = event.getOption("fields", OptionMapping::getAsString)

                    // Exit if empty
                    if (mutableListOf(title, image, description, footer, fields)
                        .filterNotNull().isEmpty()) {
                        event.replyEmbeds(
                            Embeds.FAILURE(bot)
                                .setDescription(bot.language.translate("feature.rules.command.error.empty"))
                                .build()
                        ).setEphemeral(true).queue()
                        return@registerSlashCommand
                    }

                    // Save config
                    bot.dataHandler.set(this.name, "channel", channel.id)
                    bot.dataHandler.set(this.name, "role", role.id)
                    bot.dataHandler.set(this.name, "required", required)
                    bot.dataHandler.set(this.name, "timestamp", timestamp)
                    bot.dataHandler.set(this.name, "color", color)
                    title?.let { bot.dataHandler.set(this.name, "title", it) }
                    image?.let { bot.dataHandler.set(this.name, "image", it) }
                    description?.let { bot.dataHandler.set(this.name, "description", it) }
                    footer?.let { bot.dataHandler.set(this.name, "footer", it) }
                    fields?.let { bot.dataHandler.set(this.name, "fields", it) }

                    // Setup rules
                    setup()

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.rules.command.set.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "remove" -> {
                    remove()

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.rules.command.remove.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
        }
    }

    /**
     * Removes the rules system from this guild
     */
    fun remove() {
        if (get<Boolean>("required") != true)
            return

        val role = bot.guild.getRoleById(get<String>("role") ?: return) ?: return

        val channels = bot.guild.channels.filter {
            it.permissionContainer
                .upsertPermissionOverride(role)
                .allowedPermissions
                .contains(net.dv8tion.jda.api.Permission.VIEW_CHANNEL)
        }

        channels.forEach {
            // Allow visibility
            it.permissionContainer
                .upsertPermissionOverride(bot.guild.publicRole)
                .grant(net.dv8tion.jda.api.Permission.VIEW_CHANNEL)
                .queue()
        }
    }

    /**
     * Sets up the rules system
     */
    fun setup() {
        val channel = bot.guild.getTextChannelById(get<String>("channel") ?: return) ?: return
        val role = bot.guild.getRoleById(get<String>("role") ?: return) ?: return
        val embed = getEmbed()

        // Send embed
        channel.sendMessage(
            MessageCreateBuilder()
                .addEmbeds(embed)
                .build()
        ).queue()

        // Don't modify permissions if rules are not required
        if (get<Boolean>("required") != true)
            return

        val visibleChannels = bot.guild.channels.filterNot {
            it.permissionContainer
                .upsertPermissionOverride(bot.guild.publicRole)
                .deniedPermissions
                .contains(net.dv8tion.jda.api.Permission.VIEW_CHANNEL)
        }

        // Update permissions
        visibleChannels.forEach {
            // Disallow visibility
            it.permissionContainer
                .upsertPermissionOverride(bot.guild.publicRole)
                .deny(net.dv8tion.jda.api.Permission.VIEW_CHANNEL)
                .queue()

            // Allow for everyone with accepted role
            it.permissionContainer
                .upsertPermissionOverride(role)
                .grant(net.dv8tion.jda.api.Permission.VIEW_CHANNEL)
                .queue()
        }

        // Allow view for rules channel
        channel.permissionContainer
            .upsertPermissionOverride(bot.guild.publicRole)
            .grant(net.dv8tion.jda.api.Permission.VIEW_CHANNEL)
            .queue()
    }

    private inline fun <reified R> get(x: String): R? = bot.dataHandler.get(this.name, x)

    /**
     * Returns the rules embed from the configuration
     */
    fun getEmbed() =
        EmbedFeature.createEmbed(
            get("description"), get("title"),
            null, null, null, null,
            get("footer"), null, get("image"), null, null,
            get("fields"), get("color") ?: Color.PRIMARY.asInt, get("timestamp") ?: true
        )
}