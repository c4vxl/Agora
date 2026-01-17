package de.c4vxl.bot.handler

import de.c4vxl.bot.Bot
import de.c4vxl.enums.Permission
import de.c4vxl.utils.LoggerUtils.createLogger
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import org.slf4j.Logger

/**
 * Custom permission handler that ties feature-specific permissions to guild-roles
 */
class PermissionHandler(
    val bot: Bot,
    val logger: Logger = createLogger<PermissionHandler>(),
    val dataKey: String = "permissions"
) {
    init {
        logger.info("Initializing permission handler for guild '${bot.guild.id}'")

    }

    /**
     * Returns a list of all permissions a certain role has
     * @param role The role to check
     */
    fun get(role: Role): MutableList<Permission> {
        val perms = bot.dataHandler.get<MutableList<String>>(dataKey, role.id)
            ?: mutableListOf()

        return perms.mapNotNull { Permission.valueOf(it) }.toMutableList()
    }

    /**
     * Set if a specific role has access to a permission
     * @param role The role to change the permission for
     * @param permission The permission to modify
     * @param value The new state of the permission (`true` if permission should be granted)
     */
    fun set(role: Role, permission: Permission, value: Boolean) {
        if (role.guild.id != bot.guild.id) return

        val hasPerm = has(role, permission)

        // Exit early if role already has desired state
        if (hasPerm == value) return

        // Change state
        val perms = get(role);
        if (value)
            perms.add(permission)
        else
            perms.remove(permission)

        // Update config
        bot.dataHandler.set(dataKey, role.id, perms.map { it.name })
    }

    /**
     * Checks if a role has a permission
     * @param role The role to check
     * @param permission The permission to check for
     */
    fun has(role: Role, permission: Permission): Boolean {
        val perms = get(role)
        return perms.contains(permission) || perms.contains(Permission.ALL)
    }

    /**
     * Checks if a member has a role that grants him a certain permission
     * @param member The member to check
     * @param permission The permission to check for
     */
    fun has(member: Member, permission: Permission): Boolean {
        if (has(bot.guild.publicRole, permission)) return true
        return member.roles.find { has(it, permission) } != null
    }
}