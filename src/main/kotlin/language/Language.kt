package de.c4vxl.language

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.c4vxl.config.Config
import de.c4vxl.utils.ResourceUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * The data object wrapping a translation file
 */
data class Language(
    val name: String,
    val translations: MutableMap<String, String> = mutableMapOf()
) {
    companion object {
        const val DEFAULT: String = "english"

        val available: List<String>
            get() = Files.list(Path.of(Config.get<String>("lang_dir"))).map { it.name.split(".").first() }.toList()


        /**
         * Loads a language from a name
         * @param name The name of the language file
         */
        fun fromName(name: String): Language {
            val content = Files.readString(
                Path.of(Config.get<String>("lang_dir"))
                    .resolve("$name.json")
            )
            val translations = Gson().fromJson(content, object : TypeToken<MutableMap<String, String>>(){})

            return Language(name, translations)
        }

        /**
         * Loads the default languages
         */
        fun loadLanguages() {
            val langs = ResourceUtils.readResource("langs.lines")
                .split("\n")
                .toMutableList()

            val langsPath = Path.of(Config.get<String>("lang_dir"))

            // Create dir
            Files.createDirectories(langsPath)

            // Copy default languages
            langs.forEach {
                val filePath = langsPath.resolve("$it.json")
                if (!Files.exists(filePath))
                    ResourceUtils.saveResource("langs/$it.json", filePath.toString(), false)
            }
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