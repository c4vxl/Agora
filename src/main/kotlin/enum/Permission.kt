package de.c4vxl.enum

import java.util.*

enum class Permission(name: String) {
    ALL("all")


    ;

    companion object {
        fun fromName(name: String): Permission? =
            Permission.entries.find { it.name.lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault()) }
    }
}