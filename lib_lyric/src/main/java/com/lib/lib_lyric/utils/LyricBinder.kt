package com.lib.lib_lyric.utils

import android.content.Context
import android.content.Intent
import com.lib.lib_lyric.entity.LyricBean
import com.yhy.api_lyric.utils.NetLyricBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * desc:
 **
 * user: xujj
 * time: 2024/10/23 10:41
 **/
class LyricBinder {

    companion object {

        @Volatile
        private var instance: LyricBinder? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: LyricBinder().also { instance = it }
        }
    }

    private val bindScope: CoroutineScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = SupervisorJob() + Dispatchers.Main
    }
    private var bindJob: Job? = null

    var lyricList: List<LyricBean>? = null
    private var lastMusicId: Long = -1

    fun bindLyric(
        context: Context,
        id: Long,
        title: String,
        artist: String,
        force: Boolean
    ) {
        if (lastMusicId != id || force) {
            lastMusicId = id

            lyricList = null
            context.sendBroadcast(Intent(getLoadingLyricAction(context)))

            bindJob?.cancel()
            bindJob = bindScope.launch(Dispatchers.IO) {
                val file = getInternalLrcFile(context, id)
                if (file.exists()) {
                    //1.内部存在已写入的歌词文件
                    lyricList = LyricUtils.parseLyricByFile(file)
                    context.sendBroadcast(Intent(getUpdateLyricAction(context)))
                } else {
                    //2.查找本地同名歌词文件
                    val lrcPath = LyricUtils.findLyricPath(context, title, artist)
                    if (!lrcPath.isNullOrEmpty()) {
                        LyricUtils.writeLrcFileByPath(context, lrcPath, id)//写入内部
                        lyricList = LyricUtils.parseLyricByFile(file)
                        context.sendBroadcast(Intent(getUpdateLyricAction(context)))
                    } else {
                        NetLyricBinder.getLyric(
                            title,
                            artist,
                            object : NetLyricBinder.OnNetLyricCallback {
                                override fun onSucceed(lyric: String) {
                                    LyricUtils.writeLrcFileByString(context, lyric, id)//写入内部
                                    lyricList = LyricUtils.parseLyricByFile(file)
                                    context.sendBroadcast(Intent(getUpdateLyricAction(context)))
                                }

                                override fun onFailed() {
                                    lyricList = null
                                    context.sendBroadcast(Intent(getUpdateLyricAction(context)))
                                }
                            })
                    }
                }
            }
        }
    }

    fun clearLyric(context: Context) {
        lastMusicId = -1
        lyricList = null
        context.sendBroadcast(Intent(getUpdateLyricAction(context)))
    }

    fun getCurrentLyric(time: Long): String {
        return LyricUtils.getCurrentLyric(lyricList, time)
    }
}