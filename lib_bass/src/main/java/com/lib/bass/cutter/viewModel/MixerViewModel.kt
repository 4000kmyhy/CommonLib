package com.lib.bass.cutter.viewModel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lib.bass.cutter.entity.MergerMusic
import com.lib.bass.cutter.MixerSaver
import com.lib.bass.utils.BassUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * desc:
 **
 * user: xujj
 * time: 2024/4/9 14:07
 **/
class MixerViewModel(application: Application) : BaseCutterViewModel(application) {

    var mergerMusic1: MergerMusic? = null
    var mergerMusic2: MergerMusic? = null

    private var playJob: Job? = null

    var totalDuration = 0
        private set
    private var minDuration = 0 //最小时长

    val shortestAudioModel = MutableLiveData<Boolean>()

    val gainModel1 = MutableLiveData<Float>()
    val gainModel2 = MutableLiveData<Float>()

    init {
        shortestAudioModel.value = false
    }

    private fun reset() {
        mergerMusic1?.release()
        mergerMusic2?.release()
        setCurrentState(BassUtils.STATE_IDLE)

        totalDuration = 0
        currentPositionModel.postValue(0)
    }

    fun loadMergerMusics(music1: MergerMusic?, music2: MergerMusic?) {
        reset()
        mergerMusic1 = music1
        mergerMusic2 = music2

        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.IO) {
            mergerMusic1?.let {
                it.setDataSource()
                gainModel1.postValue(it.gainValue)
            }
            mergerMusic2?.let {
                it.setDataSource()
                gainModel2.postValue(it.gainValue)
            }

            resetStartPosition()
            setSync()
            setCurrentState(BassUtils.STATE_PREPARED, true)//准备完成

            computeList(0)
        }
    }

    fun resetStartPosition() {
        mergerMusic1?.let {
            it.startPositionInList = 0
            it.endPositionInList = it.getActualDuration()
        }
        mergerMusic2?.let {
            it.startPositionInList = 0
            it.endPositionInList = it.getActualDuration()
        }
    }

    fun computeList(position: Int) {
        totalDuration = if (isShortestAudio()) {
            Math.min(
                mergerMusic1?.endPositionInList ?: 0,
                mergerMusic2?.endPositionInList ?: 0,
            )
        } else {
            Math.max(
                mergerMusic1?.endPositionInList ?: 0,
                mergerMusic2?.endPositionInList ?: 0,
            )
        }
        minDuration = if (totalDuration > 60 * 1000) {//超过一分钟
            5000
        } else if (totalDuration > 10 * 1000) {//超过10秒
            1000
        } else if (totalDuration > 1000) {//超过1秒
            300
        } else {
            0
        }

        seekTo(position)
    }

    private fun setSync() {
        if (mergerMusic1 == null || mergerMusic2 == null) return
        mergerMusic1?.let {
            it.setSync {
                if (it.endPositionInList < mergerMusic2!!.endPositionInList) {//1先结束
                    if (it.endPositionInList >= mergerMusic2!!.startPositionInList) {//2还没开始
                        mergerMusic2?.play()
                        updatePosition()
                    }
                } else {
                    pause()
                    seekTo(0)
                }
            }
        }
        mergerMusic2?.let {
            it.setSync {
                if (it.endPositionInList < mergerMusic1!!.endPositionInList) {//1先结束
                    if (it.endPositionInList >= mergerMusic1!!.startPositionInList) {//2还没开始
                        mergerMusic1?.play()
                        updatePosition()
                    }
                } else {
                    pause()
                    seekTo(0)
                }
            }
        }
    }

    fun isPlaying(): Boolean {
        return mergerMusic1?.isPlaying() == true ||
                mergerMusic2?.isPlaying() == true
    }

    fun getCurrentPosition(): Int {
        if (mergerMusic1 != null && mergerMusic2 != null) {
            val currentPosition1 = mergerMusic1!!.getCurrentPosition()
            val currentPosition2 = mergerMusic2!!.getCurrentPosition()
            return if (mergerMusic1!!.endPositionInList > mergerMusic2!!.endPositionInList) {
                if (mergerMusic1!!.startPositionInList == 0 || currentPosition1 > 0) {//先开始，或者已播放
                    mergerMusic1!!.startPositionInList + currentPosition1 - mergerMusic1!!.startPosition
                } else {
                    currentPosition2 - mergerMusic2!!.startPosition
                }
            } else {
                if (mergerMusic2!!.startPositionInList == 0 || currentPosition2 > 0) {//先开始，或者已播放
                    mergerMusic2!!.startPositionInList + currentPosition2 - mergerMusic2!!.startPosition
                } else {
                    currentPosition1 - mergerMusic1!!.startPosition
                }
            }
        }
        return 0
    }

    fun play() {
        requestAudioFocus()
        val currentPosition = getCurrentPosition()
        mergerMusic1?.let {
            if (currentPosition >= it.startPositionInList &&
                currentPosition < it.endPositionInList
            ) {
                it.seekTo(currentPosition - it.startPositionInList + it.startPosition)
                it.play()
            }
        }
        mergerMusic2?.let {
            if (currentPosition >= it.startPositionInList &&
                currentPosition < it.endPositionInList
            ) {
                it.seekTo(currentPosition - it.startPositionInList + it.startPosition)
                it.play()
            }
        }
        setCurrentState(BassUtils.STATE_PLAYING, true)
        updatePosition()
    }

    fun pause() {
        mergerMusic1?.pause()
        mergerMusic2?.pause()
        setCurrentState(BassUtils.STATE_PAUSED)
    }

    fun seekTo(position: Int) {
        val isPlaying = isPlaying()
        if (isPlaying) {
            mergerMusic1?.pause()
            mergerMusic2?.pause()
        }

        mergerMusic1?.let {
            if (position >= it.startPositionInList && position < it.endPositionInList) {
                it.seekTo(position - it.startPositionInList + it.startPosition)
                if (isPlaying) {
                    it.play()
                }
            } else {
                it.seekTo(0)
            }
        }
        mergerMusic2?.let {
            if (position >= it.startPositionInList && position < it.endPositionInList) {
                it.seekTo(position - it.startPositionInList + it.startPosition)
                if (isPlaying) {
                    it.play()
                }
            } else {
                it.seekTo(0)
            }
        }

        updatePosition()
    }

    private fun updatePosition() {
        val currentPosition = getCurrentPosition()
        currentPositionModel.postValue(currentPosition)

        if (isPlaying()) {
            mergerMusic1?.let {
                if (it.isPlaying()) {
                    if (currentPosition >= it.endPositionInList) {
                        it.stop()
                    }
                } else {
                    if (currentPosition >= it.startPositionInList &&
                        currentPosition < it.endPositionInList
                    ) {
                        it.seekTo(currentPosition - it.startPositionInList + it.startPosition)
                        it.play()
                    }
                }
            }
            mergerMusic2?.let {
                if (it.isPlaying()) {
                    if (currentPosition >= it.endPositionInList) {
                        it.stop()
                    }
                } else {
                    if (currentPosition >= it.startPositionInList &&
                        currentPosition < it.endPositionInList
                    ) {
                        it.seekTo(currentPosition - it.startPositionInList + it.startPosition)
                        it.play()
                    }
                }
            }

            if (currentPosition >= totalDuration) {
                pause()
                seekTo(0)
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

    fun setShortestAudio(isShortestAudio: Boolean) {
        if (shortestAudioModel.value != isShortestAudio) {
            //1.记录当前进度
            val currentPosition = getCurrentPosition()
            //2.重置开始位置
            if (isShortestAudio) {
                resetStartPosition()
            }
            //3.更新model（重置开始位置之后，计算总时长之前）
            shortestAudioModel.value = isShortestAudio
            //4.计算总时长、跳转到记录位置
            computeList(currentPosition)
        }
    }

    private fun isShortestAudio() = shortestAudioModel.value ?: false

    fun playOrPause() {
        if (isPlaying()) {
            pause()
        } else {
            play()
        }
    }

    fun playStart() {
        seekTo(0)
        play()
    }

    fun playEnd() {
        seekTo(totalDuration - minDuration)
        play()
    }

    fun setGain(selected: Int, gain: Float) {
        var value = gain
        value = Math.max(value, 0f)
        value = Math.min(value, 5f)
        if (selected == 0) {
            gainModel1.value = value
            mergerMusic1?.setGain(gain)
        } else {
            gainModel2.value = value
            mergerMusic2?.setGain(gain)
        }
    }

    fun getGain(selected: Int): Float {
        val gain = if (selected == 0) gainModel1.value else gainModel2.value
        return gain ?: 1f
    }

    override fun onAudioFocusLoss() {
        pause()
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
        mergerMusic1?.release()
        mergerMusic2?.release()
    }

    override fun saveFile(targetPath: String, format: String) {
        val saver = MixerSaver.Builder()
            .setMergerMusics(mergerMusic1, mergerMusic2)
            .setTargetPath(targetPath)
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