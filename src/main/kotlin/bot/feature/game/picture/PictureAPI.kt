package de.c4vxl.bot.feature.game.picture

import com.google.gson.Gson
import de.c4vxl.utils.PictureUtils.fetchImage
import net.dv8tion.jda.api.utils.FileUpload
import java.net.URL

/**
 * Implementations for various picture APIs
 */
class PictureAPI(val feature: PictureFeature) {
    /**
     * Fetches a random cat picture
     * @param queries Optional queries to narrow down search
     */
    fun cat(vararg queries: String): FileUpload {
        val baseURL = "https://cataas.com/cat"
        return fetchImage(buildString {
            append(baseURL)
            
            // Add queries
            if (queries.isNotEmpty())
                append("/${queries.joinToString(",")}")
        })
    }

    /**
     * Fetches a random dog picture
     */
    fun dog(): FileUpload? {
        val url = URL("https://api.thedogapi.com/v1/images/search?size=med&mime_types=jpg&format=json")

        val content = Gson().fromJson<List<Map<String, String>>>(
            url.readText(),
            List::class.java
        )

        val image = content.firstOrNull()?.get("url") ?: return null
        return fetchImage(image)
    }
}