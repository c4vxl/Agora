package de.c4vxl.language

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.c4vxl.utils.ResourceUtils

/**
 * The data object wrapping a translation file
 */
data class Language(
    val name: String,
    val translations: MutableMap<String, String> = mutableMapOf()
) {
    companion object {
        const val DEFAULT: String = "english"

        val available: MutableList<String>
            get() =
                ResourceUtils.readResource("langs.lines")
                    .split("\n")
                    .toMutableList()

        fun fromName(name: String): Language {
            val content = ResourceUtils.readResource("langs/${name}.json")
            val translations = Gson().fromJson(content, object : TypeToken<MutableMap<String, String>>(){})

            return Language(name, translations)
        }
    }

    /**
     * Returns the translation of a key
     * @param key The key to the translation
     */
    fun translate(key: String, vararg args: String): String {
        var translation: String = translations.getOrDefault(key, key)

        args.forEachIndexed { i, s ->
            translation = translation.replace("$$i", s)
        }

        return translation
    }
}