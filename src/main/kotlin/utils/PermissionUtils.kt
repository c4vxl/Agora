package de.c4vxl.utils

import de.c4vxl.bot.Bot
import de.c4vxl.enums.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role

object PermissionUtils {
    /**
     * Checks if a certain member has a given permission
     * @param permission The permission to check for
     * @param bot The bot responsible for handling the permissions
     */
    fun Member.hasPermission(permission: Permission, bot: Bot): Boolean =
        bot.permissionHandler.has(this, permission)

    /**
     * Checks if a certain role has a given permission
     * @param permission The permission to check for
     * @param bot The bot responsible for handling the permissions
     */
    fun Role.hasPermission(permission: Permission, bot: Bot): Boolean =
        bot.permissionHandler.has(this, permission)

    /**
     * Checks if a member as a certain role
     * @param role The role to check for
     */
    fun Member.hasRole(role: Role): Boolean =
        this.roles.find { it.id == role.id } != null

    /**
     * Returns a list of all roles in a guild with a certain permission
     * @param permissions The permissions to look for
     * @param needAll If set to true, all permissions passed are needed to return the role
     */
    fun Bot.rolesWithPermission(vararg permissions: Permission, needAll: Boolean = false): MutableList<Role> =
        this.guild.roles.filter { role ->
            if (needAll) permissions.all { role.hasPermission(it, this) }
            else permissions.any { role.hasPermission(it, this) }
        }.toMutableList()
}