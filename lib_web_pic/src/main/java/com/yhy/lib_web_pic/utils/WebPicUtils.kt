package com.yhy.lib_web_pic.utils

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.yhy.lib_web_pic.service.KuwoApiService.Companion.getKuwoData
import com.yhy.lib_web_pic.service.QQApiService.Companion.getQQData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/27 9:48
 **/

private const val TAG = "WebPicUtils"

private fun getPicUrlSP(context: Context, key: String, defValue: String): String {
    val sp = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
    return sp.getString(key, defValue) ?: defValue
}

private fun setPicUrlSP(context: Context, key: String, value: String) {
    val sp = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
    sp.edit().putString(key, value).apply()
}

enum class WebPicType {
    Kuwo,
    QQ,
    Bing,
    Fail
}

object WebPicUtils {

    private fun getPicUrlKey(songId: Long) = "pic_url_$songId"

    fun getPicUrl(context: Context, songId: Long, albumId: Long): String {
        return getPicUrlSP(context, getPicUrlKey(songId), getLocalPicUrl(albumId))
    }

    fun setPicUrl(context: Context, songId: Long, picUrl: String) {
        setPicUrlSP(context, getPicUrlKey(songId), picUrl)
    }

    fun updatePicUrl(
        context: Context,
        songId: Long,
        albumId: Long,
        songName: String,
        artistName: String,
        priority: Array<WebPicType> = arrayOf(
            WebPicType.Kuwo,
            WebPicType.QQ,
            WebPicType.Bing
        ),
        callback: (Pair<String, WebPicType>) -> Unit
    ) {
        val currentUrl = getPicUrl(context, songId, albumId)
        Glide.with(context.applicationContext)
            .load(currentUrl)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    callback(Pair(currentUrl, WebPicType.Fail))
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    CoroutineScope(Dispatchers.IO).launch {
                        val webPic = getWebPic(songName, artistName, priority)
                        if (webPic.second != WebPicType.Fail) {
                            setPicUrl(context, songId, webPic.first)
                        }
                        callback(webPic)
                    }
                }
            })
    }

    /**
     * 根据优先级依次获取在线图片
     */
    suspend fun getWebPic(
        songName: String,
        artistName: String,
        priority: Array<WebPicType> = arrayOf(
            WebPicType.Kuwo,
            WebPicType.QQ,
            WebPicType.Bing
        )
    ): Pair<String, WebPicType> {
        priority.forEach {
            when (it) {
                WebPicType.Kuwo -> {
                    val url = getKuwoPicUrl(songName, artistName)
                    if (url.isNotEmpty()) {
                        return Pair(url, it)
                    }
                }

                WebPicType.QQ -> {
                    val url = getQQPicUrl(songName, artistName)
                    if (url.isNotEmpty()) {
                        return Pair(url, it)
                    }
                }

                WebPicType.Bing -> {
                    val url = getBingPicUrl(songName, artistName)
                    if (url.isNotEmpty()) {
                        return Pair(url, it)
                    }
                }

                WebPicType.Fail -> Pair("", it)
            }
        }
        return Pair("", WebPicType.Fail)
    }

    fun getLocalPicUrl(albumId: Long): String {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        ).toString()
    }

    suspend fun getKuwoPicUrl(name: String, artist: String): String {
        return withContext(Dispatchers.IO) {
            val kuwoData = getKuwoData("$name-$artist")
            if (kuwoData != null) {
                return@withContext kuwoData.getPicUrl(name, artist)
            }
            return@withContext ""
        }
    }

    suspend fun getQQPicUrl(name: String, artist: String): String {
        return withContext(Dispatchers.IO) {
            val qqData = getQQData("$name-$artist")
            if (qqData != null) {
                return@withContext qqData.getPicUrl(name, artist)
            }
            return@withContext ""
        }
    }

    suspend fun getBingPicUrl(title: String, artist: String): String {
        return withContext(Dispatchers.IO) {
            var coverUrl = ""
            try {
                val bingUrl =
                    "https://cn.bing.com/images/search?first=1&tsc=ImageBasicHover&q=$title-$artist"
                val cookies = Jsoup.connect(bingUrl).execute().cookies()
                val document = Jsoup.connect(bingUrl)
                    .userAgent("Mozilla/5.0 (jsoup)")
                    .cookies(cookies)
                    .timeout(8000).get()
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

    suspend fun getBingPicUrls(q: String, n: Int = 20): List<String> {
        return withContext(Dispatchers.IO) {
            val urls = ArrayList<String>()
            try {
                val bingUrl = "https://cn.bing.com/images/search?first=1&tsc=ImageBasicHover&q=$q"
                val cookies = Jsoup.connect(bingUrl).execute().cookies()
                val document = Jsoup.connect(bingUrl)
                    .userAgent("Mozilla/5.0 (jsoup)")
                    .cookies(cookies)
                    .timeout(8000).get()
                val tBody = document.getElementsByClass("dgControl_list")
                val img = tBody.select("a")
                img.forEachIndexed { index, element ->
                    val m = element.attr("m")
                    try {
                        val jsonObject = JSONObject(m)
                        val murl = jsonObject.getString("murl")
                        urls.add(murl)
                        if (urls.size >= n) return@withContext urls
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext urls
        }
    }

    private var launch2Gallery: ActivityResultLauncher<Intent>? = null
    private var urlCallback: (String) -> Unit = {}

    fun onCreate(activity: ComponentActivity) {
        launch2Gallery = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                val url = it.data?.dataString
                if (url != null) {
                    urlCallback.invoke(url)
                }
            }
        }.also {
            activity.lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        it.unregister()
                    }
                }
            })
        }
    }

    fun openGallery(callback: (String) -> Unit) {
        urlCallback = callback
        launch2Gallery?.launch(
            Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        )
    }
}