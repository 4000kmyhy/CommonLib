package com.lib.bass

import com.lib.bass.utils.BassUtils.FORMAT_MP3
import com.lib.bass.utils.BassUtils.STATE_IDLE
import com.lib.bass.utils.BassUtils.STATE_PAUSED
import com.lib.bass.utils.BassUtils.STATE_PLAYING
import com.lib.bass.utils.BassUtils.STATE_PREPARED
import com.lib.bass.utils.BassUtils.STATE_STOP
import com.lib.bass.utils.BassUtils.getCurrentPosition
import com.lib.bass.utils.BassUtils.getEncodeChannel
import com.lib.bass.utils.BassUtils.isPlaying
import com.lib.bass.utils.BassUtils.logError
import com.lib.bass.utils.BassUtils.pause
import com.lib.bass.utils.BassUtils.play
import com.lib.bass.utils.BassUtils.release
import com.lib.bass.utils.BassUtils.stop
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import com.un4seen.bass.BASSenc
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2026/4/23 10:39
 **/
class BassRecorder {

    companion object {
        private const val TAG = "BassRecorder"
    }

    var currentState = STATE_IDLE
        private set
    var recordChan = 0
        private set
    var playChan = 0
        private set

    private var recorderListener: IRecorderListener? = null

    fun setOnRecorderListener(listener: IRecorderListener) {
        recorderListener = listener
    }

    init {
        setCurrentState(STATE_IDLE)
    }

    private fun setCurrentState(state: Int) {
        val hasChanged = currentState != state
        //播放状态改变
        val playStateChanged = (currentState == STATE_PLAYING) != (state == STATE_PLAYING)
        currentState = state
        if (hasChanged) {
            recorderListener?.onRecordStateChanged(this, state, playStateChanged)
        }
    }

    fun startRecord(onPlay: () -> Unit = {}) {
        if (currentState == STATE_IDLE) {
            createRecord()
        }
        if (isRecording()) {
            pauseRecord()
        } else {
            onPlay()
            playRecord()
        }
    }

    fun createRecord() {
//        releaseRecord()
        playChan = BASS.BASS_StreamCreate(
            44100, 2,
            BASS.BASS_STREAM_DECODE,
            BASS.STREAMPROC_PUSH,
            null
        )
        recordChan = BASS.BASS_RecordStart(
            44100, 2, BASS.BASS_RECORD_PAUSE,
            object : BASS.RECORDPROC {
                override fun RECORDPROC(
                    handle: Int,
                    buffer: ByteBuffer?,
                    length: Int,
                    user: Any?
                ): Boolean {
                    if (playChan != 0 && buffer != null) {
                        BASS.BASS_StreamPutData(playChan, buffer, buffer.limit())
                    }
                    return true
                }
            }, 0
        )
        setCurrentState(STATE_PREPARED)
    }

    fun isRecording(): Boolean {
        if (recordChan == 0) false
        return isPlaying(recordChan)
    }

    fun getRecordPosition(): Int {
        if (recordChan == 0) return 0
        return getCurrentPosition(recordChan).coerceAtLeast(0)
    }

    fun playRecord() {
        if (recordChan == 0) return
        play(recordChan)
        setCurrentState(STATE_PLAYING)
    }

    fun pauseRecord() {
        if (recordChan == 0) return
        pause(recordChan)
        setCurrentState(STATE_PAUSED)
    }

    fun stopRecord() {
        if (recordChan == 0) return
        stop(recordChan)
        setCurrentState(STATE_STOP)
    }

    fun releaseRecord() {
        if (playChan != 0) {
            release(playChan)
            playChan = 0
        }
        if (recordChan != 0) {
            release(recordChan)
            recordChan = 0
        }
        setCurrentState(STATE_IDLE)
    }

    fun saveRecord(
        targetPath: String,
        format: String = FORMAT_MP3,
        callback: OnSaveRecordCallback
    ) {
        if (recordChan == 0 || playChan == 0) return

        val totalLength = BASS.BASS_ChannelGetPosition(recordChan, BASS.BASS_POS_BYTE)
        if (totalLength == 0L) {
            return
        }
        stopRecord() //获取长度后停止录音

        callback.onSaveStart()

        val handle = BASS_FX.BASS_FX_TempoCreate(
            playChan,
            BASS.BASS_SAMPLE_FLOAT or BASS.BASS_STREAM_DECODE
        )

        val encoder = getEncodeChannel(handle, targetPath, format)
        if (encoder == 0) {
            logError("Encode")
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
            val progress2 = (sum * 1.0f / totalLength * 100).roundToInt().coerceIn(0, 100)
            if (progress != progress2) {
                progress = progress2
                callback.onProgressUpdated(progress)
            }
        }

        //释放、结束编码
        BASS.BASS_StreamFree(handle)
        BASSenc.BASS_Encode_Stop(encoder)
        //释放录音
        releaseRecord()

        callback.onSaveCompletion()
    }
}

fun interface IRecorderListener {
    fun onRecordStateChanged(recorder: BassRecorder, state: Int, playStateChanged: Boolean)
}

interface OnSaveRecordCallback {
    fun onSaveStart()
    fun onProgressUpdated(progress: Int)
    fun onSaveCompletion()
}