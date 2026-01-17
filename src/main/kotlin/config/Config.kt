package de.c4vxl.config

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.c4vxl.utils.ResourceUtils
import java.nio.file.Files
import java.nio.file.Path

object Config {
    const val CONFIG_PATH = "./config.json"

    val config: MutableMap<String, Any>
        get() {
            val path = Path.of(CONFIG_PATH)

            if (!Files.exists(path)) ResourceUtils.saveResource("config.json", CONFIG_PATH)

            return Gson().fromJson(Files.readString(path), object : TypeToken<MutableMap<String, Any>>(){})
        }

    inline fun <reified T> get(key: String): T =
        config[key] as T
}