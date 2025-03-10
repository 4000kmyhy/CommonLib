package com.lib.bass.utils

import android.util.Log
import androidx.annotation.FloatRange
import com.un4seen.bass.BASS
import com.un4seen.bass.BASSFLAC
import com.un4seen.bass.BASS_FX
import com.un4seen.bass.BASSenc
import com.un4seen.bass.BASSenc_FLAC
import com.un4seen.bass.BASSenc_MP3
import com.un4seen.bass.BASSenc_OGG
import java.io.File
import java.nio.ByteBuffer

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/27 9:55
 **/
object BassUtils {

    //播放状态
    const val STATE_IDLE = -1
    const val STATE_LOADING = 0
    const val STATE_PREPARED = 1
    const val STATE_PLAYING = 2
    const val STATE_PAUSED = 3
    const val STATE_STOP = 4
    const val STATE_COMPLETED = 5
    const val STATE_ERROR = 6

    //格式类型
    const val FORMAT_WAV = "wav"
    const val FORMAT_MP3 = "mp3"
    const val FORMAT_OGG = "ogg"
    const val FORMAT_FLAC = "flac"//无损压缩

    private const val TAG = "BassUtils"

    private const val device = -1
    private const val freq = 44100
    private const val flags = 0

    private var isInitBass = false
    private var isInitRecord = false

    fun initBass(): Boolean {
        if (!isInitBass) {
            isInitBass = BASS.BASS_Init(device, freq, flags)
//            val version = BASS.BASS_GetVersion()
//            val fxVersion = BASS_FX.BASS_FX_GetVersion()
        }
        return isInitBass
    }

    fun freeBass() {
        if (isInitBass) {
            if (BASS.BASS_Free()) {
                isInitBass = false
            }
        }
    }

    fun initRecord() {
        if (!isInitRecord) {
            isInitRecord = BASS.BASS_RecordInit(device)
        }
    }

    fun freeRecord() {
        if (isInitRecord) {
            if (BASS.BASS_RecordFree()) {
                isInitRecord = false
            }
        }
    }

    /**
     * 实时频谱fft数据
     */
    fun getFftData(handle: Int): ByteBuffer {
        val byteBuffer = ByteBuffer.allocate(512 * 4)
        BASS.BASS_ChannelGetData(handle, byteBuffer, BASS.BASS_DATA_FFT1024)
        return byteBuffer
    }

    fun seekTo(handle: Int, position: Int) {
        if (handle == 0) return
        val currentPosition = getCurrentPosition(handle)
        val duration = getDuration(handle)
        var second = position / 1000.0
        second = Math.max(second, 0.0)
        second = Math.min(second, duration / 1000.0 - 0.1) //保留0.1秒
        val result = BASS.BASS_ChannelSetPosition(
            handle,
            BASS.BASS_ChannelSeconds2Bytes(handle, second),
            BASS.BASS_POS_BYTE
        )
        Log.d(TAG, "seekTo: result=$result")
        if (!result) { //跳转了无效位置，返回当前进度
            BASS.BASS_ChannelSetPosition(
                handle,
                BASS.BASS_ChannelSeconds2Bytes(
                    handle,
                    currentPosition / 1000.0
                ),
                BASS.BASS_POS_BYTE
            )
        }
    }

    fun isPlaying(handle: Int): Boolean {
        if (handle == 0) return false
        return BASS.BASS_ChannelIsActive(handle) == BASS.BASS_ACTIVE_PLAYING
    }

    fun play(handle: Int, restart: Boolean = false) {
        if (handle == 0) return
        BASS.BASS_ChannelPlay(handle, restart)
    }

    fun pause(handle: Int) {
        if (handle == 0) return
        BASS.BASS_ChannelPause(handle)
    }

    fun stop(handle: Int) {
        if (handle == 0) return
        BASS.BASS_ChannelStop(handle)
        BASS.BASS_ChannelSetPosition(handle, 0, BASS.BASS_POS_BYTE)//播放进度归0
    }


    fun release(handle: Int) {
        if (handle == 0) return
        BASS.BASS_StreamFree(handle)
    }

    fun getCurrentSecond(handle: Int): Double {
        if (handle == 0) return 0.0
        return BASS.BASS_ChannelBytes2Seconds(
            handle,
            BASS.BASS_ChannelGetPosition(handle, BASS.BASS_POS_BYTE)
        )
    }

    fun getTotalSecond(handle: Int): Double {
        if (handle == 0) return 0.0
        return BASS.BASS_ChannelBytes2Seconds(
            handle,
            BASS.BASS_ChannelGetLength(handle, BASS.BASS_POS_BYTE)
        )
    }

    fun getCurrentPosition(handle: Int): Int {
        if (handle == 0) return 0
        val second = BASS.BASS_ChannelBytes2Seconds(
            handle,
            BASS.BASS_ChannelGetPosition(handle, BASS.BASS_POS_BYTE)
        )
//        return Math.round(second * 1000).toInt()
        return Math.floor(second * 1000).toInt()//向下取整
    }

    fun getDuration(handle: Int): Int {
        if (handle == 0) return 0
        val second = BASS.BASS_ChannelBytes2Seconds(
            handle,
            BASS.BASS_ChannelGetLength(handle, BASS.BASS_POS_BYTE)
        )
//        return Math.round(second * 1000).toInt()
        return Math.floor(second * 1000).toInt()//向下取整
    }

    fun setVolume(handle: Int, volume: Float) {
        setAttribute(handle, BASS.BASS_ATTRIB_VOL, volume)
    }

    /**
     * [-50,100]:0.5倍~2倍
     */
    fun setTempo(
        handle: Int,
        @FloatRange(from = -50.0, to = 100.0) tempo: Float
    ) {
        setAttribute(handle, BASS_FX.BASS_ATTRIB_TEMPO, tempo)
    }

    fun setPitch(
        handle: Int,
        @FloatRange(from = -10.0, to = 10.0) pitch: Float
    ) {
        setAttribute(handle, BASS_FX.BASS_ATTRIB_TEMPO_PITCH, pitch)
    }

    fun setFreq(handle: Int, freq: Float) {
        setAttribute(handle, BASS_FX.BASS_ATTRIB_TEMPO_FREQ, freq)
    }

    /**
     * 左右声道：-1：左，0：中间，1：右
     */
    fun setPan(handle: Int, @FloatRange(from = -1.0, to = 1.0) pan: Float) {
        setAttribute(handle, BASS.BASS_ATTRIB_PAN, pan)
    }

    fun setAttribute(handle: Int, attr: Int, value: Float) {
        if (handle == 0) return
        BASS.BASS_ChannelSetAttribute(handle, attr, value)
    }

    fun getAttribute(handle: Int, attr: Int): Float {
        if (handle == 0) return 0f
        val fValue = BASS.FloatValue()
        BASS.BASS_ChannelGetAttribute(handle, attr, fValue)
        return fValue.value
    }

    /**
     * 循环
     */
    fun setLooping(handle: Int, isLoop: Boolean) {
        if (handle == 0) return
        if (isLoop) {
            BASS.BASS_ChannelFlags(
                handle,
                BASS.BASS_SAMPLE_LOOP,
                BASS.BASS_SAMPLE_LOOP
            )
        } else {
            BASS.BASS_ChannelFlags(handle, 0, BASS.BASS_SAMPLE_LOOP)
        }
    }

    /**
     * 回放
     */
    fun setReverse(reverseChan: Int, isReverse: Boolean) {
        val reverseValue =
            getAttribute(reverseChan, BASS_FX.BASS_ATTRIB_REVERSE_DIR)
        if (isReverse) {
            if (reverseValue > 0) {
                setAttribute(
                    reverseChan,
                    BASS_FX.BASS_ATTRIB_REVERSE_DIR,
                    BASS_FX.BASS_FX_RVS_REVERSE.toFloat()
                )
            }
        } else {
            if (reverseValue < 0) {
                setAttribute(
                    reverseChan,
                    BASS_FX.BASS_ATTRIB_REVERSE_DIR,
                    BASS_FX.BASS_FX_RVS_FORWARD.toFloat()
                )
            }
        }
    }

    fun streamCreateFile(path: String): Int {
        val extension = File(path).extension
        return if (extension.lowercase() == FORMAT_FLAC) {
            BASSFLAC.BASS_FLAC_StreamCreateFile(
                path, 0, 0,
                BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN
            )
        } else {
            BASS.BASS_StreamCreateFile(
                path, 0, 0,
                BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN
            )
        }
    }

    fun getEncodeChannel(handle: Int, outputPath: String, format: String): Int {
        return if (format.lowercase() == FORMAT_WAV) {
            BASSenc.BASS_Encode_Start(
                handle,
                outputPath,
                BASSenc.BASS_ENCODE_PCM,
                null,
                null
            )
        } else if (format.lowercase() == FORMAT_MP3) {
            BASSenc_MP3.BASS_Encode_MP3_StartFile(handle, null, 0, outputPath)
        } else if (format.lowercase() == FORMAT_OGG) {
            BASSenc_OGG.BASS_Encode_OGG_StartFile(handle, null, 0, outputPath)
        } else if (format.lowercase() == FORMAT_FLAC) {
            BASSenc_FLAC.BASS_Encode_FLAC_StartFile(handle, null, 0, outputPath)
        } else {
            0
        }
    }

    fun getCutChannel(path: String, startPosition: Int, endPosition: Int): Int {
        val chan = streamCreateFile(path)
        //1.先设置结束时间
        BASS.BASS_ChannelSetPosition(
            chan,
            BASS.BASS_ChannelSeconds2Bytes(chan, endPosition / 1000.0),
            BASS.BASS_POS_END
        )
        //2.再跳转到开始时间
        BASS.BASS_ChannelSetPosition(
            chan,
            BASS.BASS_ChannelSeconds2Bytes(chan, startPosition / 1000.0),
            BASS.BASS_POS_BYTE
        )
        return chan
    }

    fun getSpeed(tempo: Float): Float {
        return (tempo + 100) / 100f
    }

    fun logError(msg: String = "") {
        val code = BASS.BASS_ErrorGetCode()
        if (code == BASS.BASS_OK) {
            Log.d(TAG, "logError($msg): ok")
        } else {
            Log.d(TAG, "logError($msg): $code")
        }
    }
}