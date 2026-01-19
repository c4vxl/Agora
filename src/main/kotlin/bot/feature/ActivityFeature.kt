package de.c4vxl.bot.feature

import de.c4vxl.bot.Bot
import de.c4vxl.bot.feature.type.Feature
import de.c4vxl.config.enums.Color
import de.c4vxl.config.enums.Embeds
import de.c4vxl.config.enums.Permission
import de.c4vxl.utils.EmbedUtils.color
import de.c4vxl.utils.EmbedUtils.withTimestamp
import de.c4vxl.utils.PermissionUtils.hasPermission
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

/**
 * Feature for managing activities
 */
class ActivityFeature(bot: Bot) : Feature<ActivityFeature>(bot, ActivityFeature::class.java) {
    override fun registerCommands() {
        bot.commandHandler.registerSlashCommand(
            Commands.slash("activities", bot.language.translate("feature.activities.command.desc"))
                .addSubcommands(
                    // /activities add [name]
                    SubcommandData("add", bot.language.translate("feature.activities.command.add.desc"))
                        .addOption(OptionType.STRING, "name", bot.language.translate("feature.activities.command.add.name.desc"), true),

                    // /activities remove [name]
                    SubcommandData("remove", bot.language.translate("feature.activities.command.remove.desc"))
                        .addOption(OptionType.STRING, "name", bot.language.translate("feature.activities.command.remove.name.desc"), true),

                    // /activities interest [name]
                    SubcommandData("interest", bot.language.translate("feature.activities.command.interest.desc"))
                        .addOption(OptionType.STRING, "name", bot.language.translate("feature.activities.command.interest.name.desc"), true),

                    // /activities ignore [name]
                    SubcommandData("ignore", bot.language.translate("feature.activities.command.ignore.desc"))
                        .addOption(OptionType.STRING, "name", bot.language.translate("feature.activities.command.ignore.name.desc"), true),

                    // /activities start <name>
                    SubcommandData("start", bot.language.translate("feature.activities.command.start.desc"))
                        .addOption(OptionType.STRING, "name", bot.language.translate("feature.activities.command.start.name.desc"), true)
                        .addOption(OptionType.STRING, "message", bot.language.translate("feature.activities.command.start.message.desc")),

                    // /activities list-members [name]
                    SubcommandData("list-members", bot.language.translate("feature.activities.command.list_members.desc"))
                        .addOption(OptionType.STRING, "name", bot.language.translate("feature.activities.command.list_members.name.desc"), true),

                    // /activities list
                    SubcommandData("list", bot.language.translate("feature.activities.command.list.desc"))
                        .addOption(OptionType.USER, "user", bot.language.translate("feature.activities.command.list.user.desc")),

                    // /activities buttons
                    SubcommandData("buttons", bot.language.translate("feature.activities.command.buttons.desc"))
                )
        ) { event ->
            // Check for permission
            if (event.member?.hasPermission(Permission.FEATURE_ACTIVITIES, bot) != true &&
                event.member?.hasPermission(Permission.FEATURE_ACTIVITIES_MANAGE, bot) != true) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            if (
                listOf("add", "remove", "list-members").contains(event.subcommandName) &&
                event.member?.hasPermission(Permission.FEATURE_ACTIVITIES_MANAGE, bot) != true
            ) {
                event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                return@registerSlashCommand
            }

            when (event.subcommandName) {
                "add" -> {
                    val name: String = event.getOption("name", OptionMapping::getAsString) ?: return@registerSlashCommand

                    activities = activities.apply { putIfAbsent(name, mutableListOf()) }

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.activities.command.add.success", name))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "remove" -> {
                    val name: String = event.getOption("name", OptionMapping::getAsString) ?: return@registerSlashCommand

                    activities = activities.apply { remove(name) }

                    // Send confirmation
                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.activities.command.remove.success", name))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "list-members" -> {
                    val name: String = event.getOption("name", OptionMapping::getAsString) ?: return@registerSlashCommand

                    val members = activities.getOrDefault(name, mutableListOf())
                        .mapNotNull { bot.guild.getMemberById(it) }

                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle(bot.language.translate("feature.activities.command.list_members.embed.title", name))
                            .apply {
                                if (members.isEmpty())
                                    this.setDescription(bot.language.translate("feature.activities.command.list_members.embed.empty.desc", name))
                                else
                                    this.setDescription("- " + members.joinToString("\n- ") { it.user.name })
                            }
                            .withTimestamp()
                            .color(Color.PRIMARY)
                            .build()
                    ).setEphemeral(true).queue()
                }

                "interest" -> {
                    val name: String = event.getOption("name", OptionMapping::getAsString) ?: return@registerSlashCommand

                    interest(name, event.user)

                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.activities.command.interest.success", name))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "ignore" -> {
                    val name: String = event.getOption("name", OptionMapping::getAsString) ?: return@registerSlashCommand

                    ignore(name, event.user)

                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.activities.command.ignore.success", name))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "start" -> {
                    val name: String = event.getOption("name", OptionMapping::getAsString) ?: return@registerSlashCommand
                    val message: String? = event.getOption("message", OptionMapping::getAsString)

                    val numUsers = start(name, event.user, message)

                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.activities.command.start.success", name, numUsers.toString()))
                            .build()
                    ).setEphemeral(true).queue()
                }

                "list" -> {
                    val target = event.getOption("user", OptionMapping::getAsUser)

                    if (target != null) {
                        event.replyEmbeds(
                            EmbedBuilder()
                                .setTitle(bot.language.translate("feature.activities.command.list.user.embed.title", target.name))
                                .apply {
                                    val items = activities.filter { it.value.contains(target.id) }

                                    if (items.isEmpty())
                                        this.setDescription(bot.language.translate("feature.activities.command.list.user.embed.empty.desc", target.asMention))
                                    else this.setDescription("- " + items.keys.joinToString("\n- "))
                                }
                                .withTimestamp()
                                .color(Color.PRIMARY)
                                .build()
                        ).setEphemeral(true).queue()

                        return@registerSlashCommand
                    }

                    event.replyEmbeds(
                        EmbedBuilder()
                            .setTitle(bot.language.translate("feature.activities.command.list.embed.title"))
                            .apply {
                                if (activities.isEmpty())
                                    this.setDescription(bot.language.translate("feature.activities.command.list.embed.empty.desc"))
                                else this.setDescription("- " + activities.keys.joinToString("\n- "))
                            }
                            .withTimestamp()
                            .color(Color.PRIMARY)
                            .build()
                    ).setEphemeral(true).queue()
                }

                "buttons" -> {
                    // Send buttons
                    event.channel.sendMessage(
                        MessageCreateBuilder()
                            .setComponents(
                                ActionRow.of(
                                    createSelectMenu()
                                )
                            )
                            .build()
                    ).queue()

                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.activities.command.buttons.success"))
                            .build()
                    ).setEphemeral(true).queue()
                }
            }
        }
    }

    init {
        registerCommands()

        bot.componentHandler.registerModal("${this@ActivityFeature.name}_custom") { event ->
            val activity = event.getValue("${this@ActivityFeature.name}_custom_name")!!.asString

            event.reply("")
                .addComponents(createComponents(activity, event.user))
                .setEphemeral(true)
                .queue()
        }

        bot.jda.addEventListener(object : ListenerAdapter() {
            override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return
                if (event.componentId != "${this@ActivityFeature.name}_select") return
                if (event.isAcknowledged) return

                // Check for permission
                if (event.member?.hasPermission(Permission.FEATURE_ACTIVITIES, bot) != true &&
                    event.member?.hasPermission(Permission.FEATURE_ACTIVITIES_MANAGE, bot) != true) {
                    event.replyEmbeds(Embeds.INSUFFICIENT_PERMS(bot)).setEphemeral(true).queue()
                    return
                }

                event.message.editMessageComponents(
                    ActionRow.of(createSelectMenu())
                ).queue()

                val activity = event.values[0]

                if (activity == "custom") {
                    event.replyModal(
                        Modal.create(
                            "${this@ActivityFeature.name}_custom",
                            bot.language.translate("feature.activities.modal.custom.title")
                        )
                            .addComponents(
                                Label.of(
                                    bot.language.translate("feature.activities.modal.custom.name"),
                                    TextInput.of("${this@ActivityFeature.name}_custom_name", TextInputStyle.SHORT)
                                )
                            )
                            .build()
                    ).queue()
                    return
                }

                event.reply("")
                    .addComponents(createComponents(activity.removePrefix("a_"), event.user))
                    .setEphemeral(true)
                    .queue()
            }

            override fun onButtonInteraction(event: ButtonInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return
                if (!event.componentId.startsWith("${this@ActivityFeature.name}_select_")) return

                val name = event.componentId
                    .removePrefix("${this@ActivityFeature.name}_select_")

                if (name.startsWith("interested")) {
                    val activity = name.removePrefix("interested_")

                    interest(activity, event.user)

                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.activities.command.interest.success", activity))
                            .build()
                    ).setEphemeral(true).queue()
                }

                else if (name.startsWith("uninterested")) {
                    val activity = name.removePrefix("uninterested_")

                    ignore(activity, event.user)

                    event.replyEmbeds(
                        Embeds.SUCCESS(bot)
                            .setDescription(bot.language.translate("feature.activities.command.ignore.success", activity))
                            .build()
                    ).setEphemeral(true).queue()
                }

                else if (name.startsWith("start")) {
                    val activity = name.removePrefix("start_")

                    event.replyModal(
                        Modal.create(
                            "${this@ActivityFeature.name}_start_${activity}",
                            bot.language.translate("feature.activities.modal.start.title")
                        )
                            .addComponents(
                                Label.of(
                                    bot.language.translate("feature.activities.modal.start.message"),
                                    bot.language.translate("feature.activities.modal.start.message.desc"),
                                    TextInput.of("${this@ActivityFeature.name}_start_msg", TextInputStyle.PARAGRAPH)
                                )
                            )
                            .build()
                    ).queue()
                }

                event.message.delete().queue()
            }

            override fun onModalInteraction(event: ModalInteractionEvent) {
                if (event.guild?.id != bot.guild.id) return
                val prefix = "${this@ActivityFeature.name}_start_"
                if (!event.modalId.startsWith(prefix)) return

                val activity = event.modalId.removePrefix(prefix)
                val message = event.getValue("${this@ActivityFeature.name}_start_msg")!!.asString

                val numUsers = start(activity, event.user, message)

                event.replyEmbeds(
                    Embeds.SUCCESS(bot)
                        .setDescription(bot.language.translate("feature.activities.command.start.success", activity, numUsers.toString()))
                        .build()
                ).setEphemeral(true).queue()
            }
        })
    }

    private fun createSelectMenu() =
        StringSelectMenu.create("${this.name}_select")
            .apply { activities.keys.forEach { this.addOption(it, "a_$it") } }
            .addOption(bot.language.translate("feature.activities.command.buttons.select.custom"), "custom")
            .build()

    private fun createComponents(activity: String, user: User) =
        ActionRow.of(
            if (activities.getOrDefault(activity, mutableListOf()).contains(user.id))
                Button.danger(
                    "${this.name}_select_uninterested_${activity}",
                    bot.language.translate("feature.activities.command.buttons.uninterested")
                )
            else Button.success(
                "${this.name}_select_interested_${activity}",
                    bot.language.translate("feature.activities.command.buttons.interested")
                ),
            Button.primary(
                "${this.name}_select_start_${activity}",
                bot.language.translate("feature.activities.command.buttons.start")
            )
        )

    private var activities: MutableMap<String, MutableList<String>>
        get() = bot.dataHandler.get(this.name, "interests") ?: mutableMapOf()
        set(value) = bot.dataHandler.set(this.name, "interests", value)

    /**
     * Start an activity
     * @param activity The activity to start
     * @param owner The user who started the activity
     * @param message An optional message
     */
    private fun start(activity: String, owner: User, message: String?): Int {
        val interested = activities[activity] ?: return 0
        val users = interested.mapNotNull { bot.guild.getMemberById(it) }

        fun get(x: String, vararg args: String): String =
            bot.language.translate("feature.activities.notification.$x", *args)

        users.forEach {
            it.user.openPrivateChannel().queue { pc ->
                pc.sendMessage(
                    MessageCreateBuilder()
                        .addEmbeds(
                            EmbedBuilder()
                                .color(Color.PRIMARY)
                                .withTimestamp()
                                .setTitle(get("title"))
                                .setDescription(get("desc", owner.asMention, bot.guild.name))
                                .setFooter(get("footer", bot.guild.name))
                                .setAuthor(owner.name, null, owner.avatarUrl)
                                .addField(get("fields.guild"), "`${bot.guild.name}`", true)
                                .addField(get("fields.activity"), "`${activity}`", true)
                                .addField(get("fields.host"), owner.asMention, true)
                                .addField(get("fields.message"), message ?: get("fields.message.empty"), false)
                                .build()
                        )
                        .addComponents(ActionRow.of(
                            Button.danger(
                                "agora_dm_delete_all",
                                bot.language.translate("global.button.dm_delete_all")
                            ),
                            Button.primary(
                                "agora_delete_message",
                                bot.language.translate("feature.activities.notification.delete")
                            )
                        ))
                        .build()
                ).queue()
            }
        }

        return users.size
    }

    /**
     * Set a user to be interested in an activity
     * @param activity The activity
     * @param user The user
     */
    private fun interest(activity: String, user: User) {
        activities = activities.apply {
            this[activity] = this.getOrDefault(activity, mutableListOf()).apply { add(user.id) }
        }
    }

    /**
     * Set a user to be interested in an activity
     * @param activity The activity
     * @param user The user
     */
    private fun ignore(activity: String, user: User) {
        activities = activities.apply {
            this[activity] = this.getOrDefault(activity, mutableListOf()).apply { remove(user.id) }
        }
    }
}