package com.yhy.api_lyric.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * desc:
 **
 * user: xujj
 * time: 2024/8/8 11:47
 **/
object OkHttpUtils {

    private const val TAG = "OkHttpUtils"

    interface OnGetCallback {
        suspend fun onSucceed(response: String)
        suspend fun onFailed()
    }

    interface OnDownloadCallback {
        suspend fun onProgressUpdated(progress: Int)
        suspend fun onSucceed(path: String)
        suspend fun onFailed()
    }

    private val mOkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8000, TimeUnit.MILLISECONDS)
            .readTimeout(8000, TimeUnit.MILLISECONDS)
            .writeTimeout(8000, TimeUnit.MILLISECONDS)
            .build()
    }

    suspend fun get(
        url: String,
        name: String? = null,
        value: String? = null,
        callback: OnGetCallback? = null
    ) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "get: url=$url")
            try {
                val request = Request.Builder().url(url).apply {
                    if (!name.isNullOrEmpty() && !value.isNullOrEmpty()) {
                        addHeader(name, value)
                    }
                }.build()
                val response = mOkHttpClient.newCall(request).execute()
                if (response.isSuccessful && response.body != null) {
                    val string = response.body!!.string()
                    Log.d(TAG, "get: response=$string")
                    callback?.onSucceed(string)
                } else {
                    callback?.onFailed()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                Log.d(TAG, "get: error=$e")
            }
        }
    }
}