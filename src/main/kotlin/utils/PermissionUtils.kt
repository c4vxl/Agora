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
}