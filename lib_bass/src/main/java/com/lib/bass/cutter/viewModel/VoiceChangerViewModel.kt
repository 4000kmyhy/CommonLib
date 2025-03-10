package com.lib.bass.cutter.viewModel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lib.bass.effect.GainEffect
import com.lib.bass.effect.VoiceEffectManager
import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import com.un4seen.bass.BASSenc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2024/4/22 17:53
 **/
class VoiceChangerViewModel(application: Application) : BaseCutterViewModel(application) {

    private var mPath: String? = null

    private var playJob: Job? = null
    private var playHandle = 0

    var duration = 0
        private set

    private var gainEffect: GainEffect? = null
    val gainModel = MutableLiveData<Float>()
    val tempoModel = MutableLiveData<Float>()
    val pitchModel = MutableLiveData<Float>()
    val freqModel = MutableLiveData<Float>()

    private var effectId = -1
    private val voiceEffectManager = VoiceEffectManager()

    private fun reset() {
        BassUtils.release(playHandle)
        setCurrentState(BassUtils.STATE_IDLE)
        duration = 0

        gainModel.value = 1f
        tempoModel.value = 0f
        pitchModel.value = 0f
        freqModel.value = 1f
        effectId = -1
        voiceEffectManager.reset()
    }

    fun loadFromPath(path: String) {
        reset()
        mPath = path

        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.IO) {
            val playChan = BassUtils.streamCreateFile(path)
            playHandle = BASS_FX.BASS_FX_TempoCreate(
                playChan,
                BASS.BASS_SAMPLE_LOOP or BASS_FX.BASS_FX_FREESOURCE
            )

            if (playHandle != 0) {
                BASS.BASS_ChannelFlags(playHandle, 0, BASS.BASS_SAMPLE_LOOP)//不循环
                BASS.BASS_ChannelSetSync(
                    playHandle,
                    BASS.BASS_SYNC_END,
                    0,
                    { handle, channel, data, user ->
                        setCurrentState(BassUtils.STATE_COMPLETED)//播放完成
                        seekTo(0)
                    },
                    0
                )
                duration = BassUtils.getDuration(playHandle)

                //音量放大
                gainEffect = GainEffect(playHandle)
                gainEffect?.setGain(getGain())
                //变音器
                voiceEffectManager.setEffectId(effectId, playHandle)

                setCurrentState(BassUtils.STATE_PREPARED, true)//准备完成
            } else {
                setCurrentState(BassUtils.STATE_ERROR, true)//播放失败
                BassUtils.logError("setDataSource")
            }
        }
    }

    fun isPlaying() = BassUtils.isPlaying(playHandle)

    fun getCurrentPosition() = BassUtils.getCurrentPosition(playHandle)

    fun play() {
        requestAudioFocus()
        BassUtils.play(playHandle)
        setCurrentState(BassUtils.STATE_PLAYING)
        updatePosition()
    }

    fun pause() {
        BassUtils.pause(playHandle)
        setCurrentState(BassUtils.STATE_PAUSED)
    }

    fun seekTo(position: Int) {
        BassUtils.seekTo(playHandle, position)
        updatePosition()
    }

    private fun updatePosition() {
        val currentPosition = getCurrentPosition()
        currentPositionModel.postValue(currentPosition)

        mHandler.removeCallbacks(updatePosition)
        mHandler.postDelayed(updatePosition, 100)
    }

    private val updatePosition = Runnable {
        if (isPlaying()) {
            updatePosition()
        }
    }

    fun playOrPause() {
        if (isPlaying()) {
            pause()
        } else {
            play()
        }
    }

    fun setGain(gain: Float) {
        var value = gain
        value = Math.max(value, 0f)
        value = Math.min(value, 5f)
        gainEffect?.setGain(value)
        gainModel.value = value
    }

    fun getGain(): Float {
        return gainModel.value ?: 1f
    }

    fun setEffectId(effectId: Int) {
        this.effectId = effectId
        voiceEffectManager.setEffectId(effectId, playHandle)
    }

    fun setTempo(tempo: Float) {
        var value = tempo
        value = Math.max(value, -50f)
        value = Math.min(value, 100f)
        tempoModel.value = value
        voiceEffectManager.setTempo(playHandle, value)
    }

    fun getTempo(): Float {
        return tempoModel.value ?: 0f
    }

    fun setPitch(pitch: Float) {
        var value = pitch
        value = Math.max(value, -10f)
        value = Math.min(value, 10f)
        pitchModel.value = value
        voiceEffectManager.setPitch(playHandle, value)
    }

    fun getPitch(): Float {
        return pitchModel.value ?: 0f
    }

    fun setFreq(freq: Float) {
        var value = freq
        value = Math.max(value, 0.5f)
        value = Math.min(value, 1.5f)
        freqModel.value = value
        voiceEffectManager.setFreq(playHandle, value)
    }

    fun getFreq(): Float {
        return freqModel.value ?: 1f
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
        BassUtils.release(playHandle)
    }

    override fun onAudioFocusLoss() {
        pause()
    }

    override fun saveFile(targetPath: String, format: String) {
        if (mPath == null) return

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            saveProgressModel.value = 0
            saveStateModel.value = false
            withContext(Dispatchers.IO) {
                val chan = BassUtils.streamCreateFile(mPath!!)
                val handle = BASS_FX.BASS_FX_TempoCreate(
                    chan,
                    BASS.BASS_SAMPLE_FLOAT or BASS.BASS_STREAM_DECODE
                )
                val voiceEffectManager = VoiceEffectManager()
                voiceEffectManager.setCustomValue(getTempo(), getPitch(), getFreq())
                voiceEffectManager.setEffectId(effectId, handle)

                val encoder = BassUtils.getEncodeChannel(handle, targetPath, format)
                if (encoder == 0) {
                    BassUtils.logError("Encode")
                    return@withContext
                }

                val speed = BASS_FX.BASS_FX_TempoGetRateRatio(handle)

                val totalLength = BASS.BASS_ChannelGetLength(handle, BASS.BASS_POS_BYTE)
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
                    sum += (length * speed).toInt()
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