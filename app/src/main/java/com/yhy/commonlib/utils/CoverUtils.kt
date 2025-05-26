package com.yhy.commonlib.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/25 14:37
 **/
object CoverUtils {

    suspend fun getCoverUrlByBing(title: String, artist: String): String {
        return withContext(Dispatchers.IO) {
            var coverUrl = ""
            try {
                val bingUrl = "https://cn.bing.com/images/search?first=1&tsc=ImageBasicHover&q="
                val query = "$bingUrl$title-$artist"
                val cookies = Jsoup.connect(query).execute().cookies()
                val document = Jsoup.connect(query)
                    .userAgent("Mozilla/5.0 (jsoup)").cookies(cookies)
                    .timeout(15000).get()
                val tBody = document.getElementsByClass("dgControl_list")
                val img = tBody.select("a")
                if (img.size > 0) {
                    val m = img[0].attr("m")
                    try {
                        val jsonObject = JSONObject(m)
                        coverUrl = jsonObject.getString("murl")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext coverUrl
        }
    }
}