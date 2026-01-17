package de.c4vxl.enums

import java.util.*

enum class Permission(name: String) {
    ALL("all"),
    FEATURE_EMBED("feature_embed"),
    FEATURE_CHANNELS_VOICE("feature_create_voice"),
    FEATURE_CHANNELS_TEXT("feature_create_text"),
    FEATURE_CHANNELS_UNLIMITED("feature_create_unlimited"),

    ;

    companion object {
        fun fromName(name: String): Permission? =
            Permission.entries.find { it.name.lowercase(Locale.getDefault()) == name.lowercase(Locale.getDefault()) }
    }
}