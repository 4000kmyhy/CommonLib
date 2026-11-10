package com.lib.bass.utils

import com.un4seen.bass.BASS
import com.un4seen.bass.BASSenc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2026/1/29 15:39
 **/
object BassConverter {

    interface OnConvertCallback {
        fun onProgressUpdated(progress: Int)
        suspend fun onSucceed(path: String)
        suspend fun onFailed()
    }

    suspend fun convertFile(
        sourceFile: File,
        targetFile: File,
        callback: OnConvertCallback,
        delayTimeMillis: Long = 10
    ) {
        withContext(Dispatchers.IO) {
            try {
                val handle = BASS.BASS_StreamCreateFile(
                    sourceFile.path, 0, 0,
                    BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN
                )
                convertChannel(handle, targetFile, callback, delayTimeMillis)
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onFailed()
            }
        }
    }

    suspend fun convertUrl(
        url: String,
        targetFile: File,
        callback: OnConvertCallback,
        delayTimeMillis: Long = 0
    ) {
        withContext(Dispatchers.IO) {
            try {
                val handle = BASS.BASS_StreamCreateURL(
                    url,
                    0,
                    BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN,
                    null,
                    null
                )
                convertChannel(handle, targetFile, callback, delayTimeMillis)
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onFailed()
            }
        }
    }

    suspend fun convertChannel(
        handle: Int, // BASS_STREAM_DECODE
        targetFile: File,
        callback: OnConvertCallback,
        delayTimeMillis: Long = 0
    ) {
        val encoder = BassUtils.getEncodeChannel(
            handle,
            targetFile.path,
            targetFile.extension
        )
        if (encoder == 0) {
            BassUtils.logError("Encode")
            callback.onFailed()
            return
        }

        val totalLength = BASS.BASS_ChannelGetLength(handle, BASS.BASS_POS_BYTE)
        if (totalLength == 0L) {
            callback.onFailed()
            return
        }

        var sum = 0
        var length = 0
        var progress = 0
        val buffer = ByteBuffer.allocate(1024)
        while (BASS.BASS_ChannelGetData(
                handle,
                buffer,
                buffer.limit()
            ).also { length = it } > 0
        ) {
            sum += length
            val progress2 = (100f * sum / totalLength)
                .roundToInt()
                .coerceAtMost(100)
            if (progress != progress2) {
                progress = progress2
                callback.onProgressUpdated(progress)
                delay(delayTimeMillis)
            }
        }

        //释放、结束编码
        BASS.BASS_StreamFree(handle)
        BASSenc.BASS_Encode_Stop(encoder)

        if (sum >= totalLength) {//下载完成
            callback.onSucceed(targetFile.path)
        } else {//cancel
            targetFile.delete()
            callback.onFailed()
        }
    }
}