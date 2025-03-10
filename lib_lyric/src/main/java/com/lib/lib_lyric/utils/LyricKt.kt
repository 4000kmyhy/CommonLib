package com.lib.lib_lyric.utils

import android.content.Context
import android.util.Log
import com.lib.lib_lyric.entity.LyricBean
import com.yhy.api_lyric.utils.NetLyricBinder
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2024/10/25 15:37
 **/

fun getLoadingLyricAction(context: Context): String {
    return context.packageName + ".LOADING_LYRIC"
}

fun getUpdateLyricAction(context: Context): String {
    return context.packageName + ".UPDATE_LYRIC"
}

const val LRC_EXTENSION = ".lrc"

fun getLrcRootDir(context: Context) = context.cacheDir.path + "/lrc"

fun getInternalLrcFile(context: Context, musicId: Long) =
    File(getLrcRootDir(context), "$musicId$LRC_EXTENSION")

object LyricKt {

    @JvmStatic
    fun setLyricList(lyricList: MutableList<LyricBean>?) {
        LyricBinder.getInstance().lyricList = lyricList
    }

    @JvmStatic
    fun getLyricList(): List<LyricBean>? {
        return LyricBinder.getInstance().lyricList
    }

    @JvmStatic
    fun bindLyric(
        context: Context,
        id: Long,
        title: String,
        artist: String,
        force: Boolean
    ) {
        LyricBinder.getInstance().bindLyric(context, id, title, artist, force)
    }

    @JvmStatic
    fun clearLyric(context: Context) {
        LyricBinder.getInstance().clearLyric(context)
    }

    @JvmStatic
    fun getCurrentLyric(time: Long): String {
        return LyricBinder.getInstance().getCurrentLyric(time)
    }

    @JvmStatic
    fun getLocalLrcFiles(context: Context, name: String = ""): List<File> {
        return LyricUtils.getLrcFiles(context, name)
    }

    @JvmStatic
    suspend fun getOnlineLrcResults(
        name: String,
        callback: (List<Triple<String, String, String>>?) -> Unit
    ) {
        NetLyricBinder.getLyricResults(name, callback)
    }

    @JvmStatic
    suspend fun getOnlineLyric(songMid: String, callback: (String) -> Unit) {
        NetLyricBinder.getLyric(songMid, object : NetLyricBinder.OnNetLyricCallback {
            override fun onSucceed(lyric: String) {
                callback(lyric)
            }

            override fun onFailed() {
                callback("")
            }
        })
    }

    @JvmStatic
    fun writeLrcFileByPath(
        context: Context,
        lrcPath: String,
        musicId: Long,
        callback: (Boolean) -> Unit
    ) {
        val result = LyricUtils.writeLrcFileByPath(context, lrcPath, musicId)
        callback(result)
    }

    @JvmStatic
    fun writeLrcFileByString(
        context: Context,
        lyric: String,
        musicId: Long,
        callback: (Boolean) -> Unit
    ) {
        val result = LyricUtils.writeLrcFileByString(context, lyric, musicId)
        Log.d("xxx", "writeLrcFileByString: " + result + " " + lyric)
        callback(result)
    }

    @JvmStatic
    fun initDesktop(callback: (Boolean) -> Unit) {
        DesktopLyricUtils.init(callback)
    }

    @JvmStatic
    fun showDesktopLyric(context: Context, isLock: Boolean) {
        DesktopLyricUtils.getInstance().show(context, isLock)
    }

    @JvmStatic
    fun hideDesktopLyric() {
        DesktopLyricUtils.getInstance().hide()
    }

    @JvmStatic
    fun unlockDesktopLyric() {
        DesktopLyricUtils.getInstance().unlock()
    }

    @JvmStatic
    fun bindService(service: DesktopLyricInterface) {
        DesktopLyricUtils.getInstance().mService = service
    }
}