package com.yhy.commonlib.voice

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lib.bass.BassRecorder
import com.lib.bass.OnSaveRecordCallback
import com.lib.bass.utils.BassUtils.STATE_IDLE
import com.lib.bass.utils.BassUtils.initBass
import com.lib.bass.utils.BassUtils.initRecord
import com.un4seen.bass.BASS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.log10

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/20 10:16
 **/
class RecordVoiceViewModel(
    application: Application
) : AndroidViewModel(application) {

    var recordState by mutableIntStateOf(STATE_IDLE)
        private set
    var currentPosition by mutableIntStateOf(0)
        private set
    var recordWaveData by mutableStateOf(listOf<Int>())
        private set

    private val mRecorder = BassRecorder()
    private var updatePositionJob: Job? = null

    private val mAudioManager by lazy {
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val recordFile = File(
        getApplication<Application>().cacheDir.path,
        "my_record.mp3"
    )

    init {
        initBass()
        initRecord()

        mRecorder.setOnRecorderListener { recorder, state, playStateChanged ->
            recordState = state
            if (playStateChanged) {
                updatePosition()
            }
        }
    }

    fun isRecordIdle() = recordState == STATE_IDLE

    fun startRecord() {
        mRecorder.startRecord {
            requestAudioFocus()
        }
    }

    fun saveRecord(
        onStart: () -> Unit,
        onSaved: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            recordFile.parentFile?.mkdirs()
            recordFile.delete()
            mRecorder.saveRecord(
                recordFile.path,
                callback = object : OnSaveRecordCallback {
                    override fun onSaveStart() {
                        onStart()
                    }

                    override fun onProgressUpdated(progress: Int) {
                    }

                    override fun onSaveCompletion() {
                        onSaved(recordFile.path)
                        currentPosition = 0
                        recordWaveData = listOf()
                    }
                })
        }
    }

    fun pause() {
        mRecorder.pauseRecord()
    }

    fun release() {
        mRecorder.releaseRecord()
        currentPosition = 0
        recordWaveData = listOf()
    }

    private fun updatePosition() {
        updatePositionJob?.cancel()
        updatePositionJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                currentPosition = mRecorder.getRecordPosition()
                if (mRecorder.isRecording()) {
                    val level = floatArrayOf(0f)
                    BASS.BASS_ChannelGetLevelEx(
                        mRecorder.recordChan,
                        level,
                        0.1f,
                        BASS.BASS_LEVEL_MONO
                    )
                    if (level[0] > 0) {
                        level[0] = (1 + 0.5 * log10(level[0].toDouble())).toFloat()
                    }
                    recordWaveData = recordWaveData + (level[0].coerceIn(0f, 1f) * 100).toInt()

                    delay(100)
                } else {
                    break
                }
            }
        }
    }

    private fun requestAudioFocus() {
        mAudioManager.requestAudioFocus(
            onAudioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }

    private fun abandonAudioFocus() {
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener)
    }

    private val onAudioFocusChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
            ) {
                pause()
            }
        }

    override fun onCleared() {
        super.onCleared()
        abandonAudioFocus()
        release()
    }
}