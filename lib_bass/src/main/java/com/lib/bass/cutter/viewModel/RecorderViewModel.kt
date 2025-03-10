package com.lib.bass.cutter.viewModel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import com.un4seen.bass.BASSenc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2024/4/18 17:32
 **/
class RecorderViewModel(application: Application) : BaseCutterViewModel(application) {

    private var recordChan = 0
    private var playChan = 0

    val recordStateModel = MutableLiveData<Int>()
    val levelModel = MutableLiveData<Float>()

    init {
        BassUtils.initRecord()
        recordStateModel.value = BassUtils.STATE_IDLE
    }

    fun startRecord() {
        if (recordChan == 0) {
            createRecord()
        }
        if (isRecording()) {
            pauseRecord()
        } else {
            startRecordInternal()
        }
    }

    private fun createRecord() {
        if (playChan != 0) {
            BASS.BASS_StreamFree(playChan)
        }
        if (recordChan != 0) {
            BASS.BASS_StreamFree(recordChan)
        }

        playChan = BASS.BASS_StreamCreate(
            44100, 2,
            BASS.BASS_STREAM_DECODE,
            BASS.STREAMPROC_PUSH,
            null
        )
//        playHandle = BASS_FX.BASS_FX_TempoCreate(
//            playChan,
//            BASS.BASS_SAMPLE_LOOP or BASS_FX.BASS_FX_FREESOURCE
//        )//todo

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
    }

    fun isInitRecord() = recordChan != 0

    fun isRecording(): Boolean {
        return BassUtils.isPlaying(recordChan)
    }

    fun getCurrentPosition(): Int {
        return BassUtils.getCurrentPosition(recordChan)
    }

    private fun startRecordInternal() {
        requestAudioFocus()
        BassUtils.play(recordChan)
        recordStateModel.postValue(BassUtils.STATE_PLAYING)
        updatePosition()
    }

    fun pauseRecord() {
        BassUtils.pause(recordChan)
        recordStateModel.postValue(BassUtils.STATE_PAUSED)
    }

    fun stopRecord() {
        BASS.BASS_ChannelStop(recordChan)
        recordChan = 0
        recordStateModel.postValue(BassUtils.STATE_IDLE)
    }

    private fun updatePosition() {
        mHandler.removeCallbacks(recordRunnable)
        mHandler.postDelayed(recordRunnable, 100)
    }

    private val recordRunnable = object : Runnable {
        override fun run() {
            if (isRecording()) {
                currentPositionModel.postValue(getCurrentPosition())
                val level = floatArrayOf(0f)
                BASS.BASS_ChannelGetLevelEx(recordChan, level, 0.1f, BASS.BASS_LEVEL_MONO)
                if (level[0] > 0) {
                    level[0] =
                        (1 + 0.5 * Math.log10(level[0].toDouble())).toFloat() // convert to dB (40dB range)
                    if (level[0] < 0) level[0] = 0f
                }
                levelModel.postValue(level[0] * 100)
                mHandler.postDelayed(this, 100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        BassUtils.release(playChan)
        BassUtils.release(recordChan)
        BassUtils.freeRecord()
    }

    override fun onAudioFocusLoss() {
        pauseRecord()
    }

    override fun saveFile(targetPath: String, format: String) {
        //只能通过录音进度来获取长度
        val totalLength = BASS.BASS_ChannelGetPosition(recordChan, BASS.BASS_POS_BYTE)

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            saveProgressModel.value = 0
            saveStateModel.value = false
            withContext(Dispatchers.IO) {
                val handle = BASS_FX.BASS_FX_TempoCreate(
                    playChan,
                    BASS.BASS_SAMPLE_FLOAT or BASS.BASS_STREAM_DECODE
                )

                val encoder = BassUtils.getEncodeChannel(handle, targetPath, format)
                if (encoder == 0) {
                    BassUtils.logError("Encode")
                    return@withContext
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
                    var progress2 = (sum * 1.0f / totalLength * 100).roundToInt()
                    progress2 = Math.min(progress2, 100)

                    if (progress != progress2) {
                        progress = progress2
                        withContext(Dispatchers.Main) {
                            saveProgressModel.value = progress
                        }
                    }
                }

                //释放、结束编码
                BASS.BASS_StreamFree(handle)
                BASSenc.BASS_Encode_Stop(encoder)

                withContext(Dispatchers.Main) {
                    saveStateModel.value = true
                }
            }
        }
    }
}