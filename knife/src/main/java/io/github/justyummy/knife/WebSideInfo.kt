package io.github.justyummy.knife

import android.util.Log
import org.jsoup.Jsoup

object WebSideInfo {

    fun getTitle(link: String): String? {
        var title: String? = null

        try {
            val doc = Jsoup.connect(link).get()
            title = doc.title()
        } catch (e: Exception) {
            Log.i("TheTitleError", e.toString())
        }

        return title
    }
}