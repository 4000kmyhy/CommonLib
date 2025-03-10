package com.lib.bass.cutter.viewModel

import android.app.Application
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lib.bass.cutter.CutterSaver
import com.lib.bass.effect.FadeEffect
import com.lib.bass.effect.GainEffect
import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class CutterViewModel(application: Application) : BaseCutterViewModel(application) {

    private var mPath: String? = null

    private var playJob: Job? = null
    private var playHandle = 0

    private var fadeEffect: FadeEffect? = null
    private var gainEffect: GainEffect? = null

    var duration = 0
        private set
    var minDuration = 0 //最小时长
        private set

    val trimModel = MutableLiveData<Boolean>() //true裁剪、false剪切
    val startPositionModel = MutableLiveData<Int>()
    val endPositionModel = MutableLiveData<Int>()

    val gainModel = MutableLiveData<Float>()
    val tempoModel = MutableLiveData<Float>()
    val masterTempoModel = MutableLiveData<Boolean>()
    val fadeInModel = MutableLiveData<Int>()
    val fadeOutModel = MutableLiveData<Int>()

    //标记，符合条件时只执行一次
    private var doSeekToEnd = true //裁剪中间时，可跳转到结束时间
    private var doFadeOut = true //可执行淡出

    private var waveformHandle = 0

    val waveformModel = MutableLiveData<Pair<Float, MutableList<Int>>>()

    fun reset() {
        BassUtils.release(playHandle)
        setCurrentState(BassUtils.STATE_IDLE)

        gainModel.value = 1f
        tempoModel.value = 0f
        masterTempoModel.value = true
        fadeInModel.value = 0
        fadeOutModel.value = 0

        duration = 0
        setStartPosition(0)
        setEndPosition(0)

        BassUtils.release(waveformHandle)
        waveformModel.value = Pair(0f, ArrayList())
    }

    fun loadFromPath(path: String) {
        reset()
        mPath = path

        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.IO) {
            val result1 = async { setDataSource(path) }
            val result2 = async { loadWaveformData(path) }
        }
    }

    private suspend fun setDataSource(path: String) {
        setCurrentState(BassUtils.STATE_LOADING)//加载中

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
                    if (isTrim()) {
                        seekTo(getStartPosition())
                    } else {
                        seekTo(0)
                    }
                },
                0
            )

            //淡入淡出
            fadeEffect = FadeEffect(playHandle)
            //音量放大
            gainEffect = GainEffect(playHandle)
            gainEffect?.setGain(getGain())
            //变速
            BassUtils.setTempo(playHandle, getTempo())
            //变调
            if (isMasterTempo()) {
                BassUtils.setPitch(playHandle, 0f)
            } else {
                BassUtils.setPitch(playHandle, getTempo() / 10f)
            }

            duration = BassUtils.getDuration(playHandle)
            minDuration = if (duration > 60 * 1000) {//超过一分钟
                5000
            } else if (duration > 10 * 1000) {//超过10秒
                1000
            } else if (duration > 1000) {//超过1秒
                300
            } else {
                0
            }
            withContext(Dispatchers.Main) {
                setStartPosition((duration * 0.2f).toInt())
                setEndPosition((duration * 0.8f).toInt())
            }

            setCurrentState(BassUtils.STATE_PREPARED, true)//准备完成
        } else {
            setCurrentState(BassUtils.STATE_ERROR, true)//播放失败
            BassUtils.logError("setDataSource")
        }
    }

    fun isPlaying() = BassUtils.isPlaying(playHandle)

    fun getCurrentPosition() = BassUtils.getCurrentPosition(playHandle)

    fun play() {
        requestAudioFocus()
        BassUtils.play(playHandle)
        setCurrentState(BassUtils.STATE_PLAYING)
        doFadeOut = true
        doSeekToEnd = true
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
        if (playHandle == 0) return

        val currentPosition = getCurrentPosition()
        currentPositionModel.postValue(currentPosition)

        val speed = BassUtils.getSpeed(getTempo())

        if (isTrim()) {
            if (currentPosition >= getEndPosition()) {
                pause()
                seekTo(getStartPosition())
            }
            //淡出
            if (currentPosition >= getEndPosition() - getFadeOutTime() * speed &&
                currentPosition < getEndPosition()
            ) {
                if (doFadeOut) {
                    doFadeOut = false
                    fadeEffect?.fadeOut()
                }
            }
        } else {
            if (currentPosition >= getStartPosition() && currentPosition < getEndPosition()) {
                if (doSeekToEnd) {
                    doSeekToEnd = false
                    if (getEndPosition() == duration) {//死循环
                        pause()
                        if (getStartPosition() != 0) {
                            seekTo(0)
                        }
                    } else {
                        seekTo(getEndPosition())
                    }
                }
            }
            //淡出
            if (currentPosition >= duration - getFadeOutTime() * speed &&
                currentPosition < duration
            ) {
                if (doFadeOut) {
                    doFadeOut = false
                    fadeEffect?.fadeOut()
                }
            }
        }
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
            var position = getCurrentPosition()
            if (isTrim()) {
                if (position < getStartPosition() || position >= getEndPosition()) {
                    position = getStartPosition()
                }
            } else {
                if (position >= getStartPosition() && position < getEndPosition()) {
                    position = 0
                }
            }
            seekToPlay(position)
        }
    }

    fun seekToPlay(position: Int) {
        //1.如果是播放状态，先暂停
        if (isPlaying()) {
            pause()
        }
        //2.重置音效
        fadeEffect?.reset()
        //3.跳转进度
//        seekTo(position)
        BassUtils.seekTo(playHandle, position)
        //4.淡入
        if (isTrim()) {
            if (position == getStartPosition()) {//从start开始，淡入
                fadeEffect?.fadeIn()
            }
        } else {
            if (getStartPosition() == 0) {//start为0，都淡入
                fadeEffect?.fadeIn()
            }
        }
        //5.播放
        play()
    }

    @MainThread
    fun setTrim(isTrim: Boolean) {
        if (trimModel.value != isTrim) {
            pause()
            trimModel.value = isTrim
        }
    }

    fun isTrim(): Boolean {
        return trimModel.value ?: true
    }

    @MainThread
    fun setStartPosition(position: Int) {
        startPositionModel.value = position
    }

    fun getStartPosition(): Int {
        return startPositionModel.value ?: 0
    }

    @MainThread
    fun setEndPosition(position: Int) {
        endPositionModel.value = position
    }

    fun getEndPosition(): Int {
        return endPositionModel.value ?: 0
    }

    fun getTotalTime(): Int {
        return if (isTrim()) {
            getEndPosition() - getStartPosition()
        } else {
            duration - (getEndPosition() - getStartPosition())
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

    fun setTempo(tempo: Float) {
        var value = tempo
        value = Math.max(value, -50f)
        value = Math.min(value, 100f)
        BassUtils.setTempo(playHandle, value)
        if (isMasterTempo()) {
            BassUtils.setPitch(playHandle, 0f)
        } else {
            BassUtils.setPitch(playHandle, value / 10f)
        }
        tempoModel.value = value
    }

    fun getTempo(): Float {
        return tempoModel.value ?: 0f
    }

    fun setMasterTempo(isMasterTempo: Boolean) {
        if (isMasterTempo) {
            BassUtils.setPitch(playHandle, 0f)
        } else {
            BassUtils.setPitch(playHandle, getTempo() / 10f)
        }
        masterTempoModel.value = isMasterTempo
    }

    fun isMasterTempo(): Boolean {
        return masterTempoModel.value ?: true
    }

    fun setFadeInTime(time: Int) {
        var value = time
        value = Math.max(value, 0)
        value = Math.min(value, 10000)
        fadeEffect?.fadeInTime = value
        fadeInModel.value = value
    }

    fun getFadeInTime(): Int {
        return fadeInModel.value ?: 0
    }

    fun setFadeOutTime(time: Int) {
        var value = time
        value = Math.max(value, 0)
        value = Math.min(value, 10000)
        fadeEffect?.fadeOutTime = value
        fadeOutModel.value = value
    }

    fun getFadeOutTime(): Int {
        return fadeOutModel.value ?: 0
    }

    private fun loadWaveformData(path: String) {
        waveformHandle = BassUtils.streamCreateFile(path)
        val length = BASS.BASS_ChannelGetLength(waveformHandle, BASS.BASS_POS_BYTE)
        if (length == 0L) return
//        val second = BASS.BASS_ChannelBytes2Seconds(waveformHandle, length)
//        val count = (second * 10).toInt() //一秒100条数据
//        if (count == 0) return
        val count = 1000
        val waveformData = ArrayList<Int>()
        val buffer = ByteBuffer.allocate((length / count).toInt())
        while (true) {
            val res = BASS.BASS_ChannelGetData(waveformHandle, buffer, buffer.limit())
            if (res == -1) break
//            val tempDataArray = byteArray2Sampler(buffer.array(), buffer.limit())
//            if (tempDataArray != null && waveformData.size < count) {//不超过totalSize
//                waveformData.add(tempDataArray)
//                waveformModel.postValue(Pair(count, waveformData))
//            }
            val level = floatArrayOf(0f)
            BASS.BASS_ChannelGetLevelEx(
                waveformHandle,
                level,
                0.01f,
                BASS.BASS_LEVEL_MONO or BASS.BASS_LEVEL_RMS
            )
            if (level[0] > 0) {
                level[0] =
                    (1 + 0.5 * Math.log10(level[0].toDouble())).toFloat() // convert to dB (40dB range)
                if (level[0] < 0) level[0] = 0f
            }
            waveformData.add((level[0] * 100).toInt())

            val position = BASS.BASS_ChannelGetPosition(waveformHandle, BASS.BASS_POS_BYTE)
            val progress = 1f * position / length
            waveformModel.postValue(Pair(progress, waveformData))
        }
        //已解码完，释放当前解码句柄
        BASS.BASS_StreamFree(waveformHandle)
        waveformHandle = 0
    }

    override fun onAudioFocusLoss() {
        pause()
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
        BassUtils.release(playHandle)
        BassUtils.release(waveformHandle)
    }

    override fun saveFile(targetPath: String, format: String) {
        val saver = CutterSaver.Builder()
            .setSourcePath(mPath)
            .setTargetPath(targetPath)
            .setTrim(isTrim())
            .setDuration(duration, getStartPosition(), getEndPosition())
            .setFxValue(getGain(), getTempo(), isMasterTempo())
            .setFadeTime(getFadeInTime(), getFadeOutTime())
            .setFormat(format)
            .create()

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            saveProgressModel.value = 0
            saveStateModel.value = false
            saver.save(object : OnCutterSaveCallback {
                override fun onProgressUpdated(progress: Int) {
                    saveProgressModel.value = progress
                }

                override fun onCompletion() {
                    saveStateModel.value = true
                }
            })
        }
    }
}