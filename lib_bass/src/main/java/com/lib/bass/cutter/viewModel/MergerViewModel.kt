package com.lib.bass.cutter.viewModel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lib.bass.cutter.entity.MergerMusic
import com.lib.bass.cutter.MergerSaver
import com.lib.bass.utils.BassUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * desc:
 **
 * user: xujj
 * time: 2024/4/1 14:51
 **/
class MergerViewModel(application: Application) : BaseCutterViewModel(application) {

    val mergerList = ArrayList<MergerMusic>()

    private var playJob: Job? = null

    var totalDuration = 0
        private set

    private var currentIndex = 0
        set(value) {
            field = value
            currentIndexModel.postValue(value)
        }
    val currentIndexModel = MutableLiveData<Int>()

    var adjustTime = 0//todo 只对保存有效

    fun loadMergerList(list: ArrayList<MergerMusic>) {
        reset()
        mergerList.addAll(list)

        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.IO) {
            for (mergerMusic in mergerList) {
                mergerMusic.setDataSource()
            }
            computeList()
        }
    }

    private fun reset() {
        for (mergerMusic in mergerList) {
            mergerMusic.release()
        }
        mergerList.clear()
        setCurrentState(BassUtils.STATE_IDLE)

        totalDuration = 0
        currentIndex = 0
        currentPositionModel.postValue(0)
    }

    fun updateMergerList(list: ArrayList<MergerMusic>) {
        stop()

        mergerList.clear()
        mergerList.addAll(list)

        totalDuration = 0
        currentIndex = 0
        currentPositionModel.postValue(0)

        computeList()
    }

    fun computeList() {
        if (mergerList.isEmpty()) return

        for (i in mergerList.indices) {
            if (i == 0) {
                mergerList[i].startPositionInList = 0
            } else {
                mergerList[i].startPositionInList = mergerList[i - 1].endPositionInList
            }
            mergerList[i].endPositionInList =
                mergerList[i].startPositionInList + mergerList[i].getActualDuration()
        }
        totalDuration = mergerList[mergerList.size - 1].endPositionInList

        setSync()

        setCurrentState(BassUtils.STATE_PREPARED, true)//准备完成

        seekTo(0)
    }

    private fun setSync() {
        for (i in mergerList.indices) {
            mergerList[i].setSync {
                playIndex(i + 1)
            }
        }
    }

    fun isPlaying(): Boolean {
        for (mergerMusic in mergerList) {
            if (mergerMusic.isPlaying()) {
                return true
            }
        }
        return false
    }

    fun getCurrentPosition(): Int {
        if (currentIndex in mergerList.indices) {
            val mergerMusic = mergerList[currentIndex]
            return mergerMusic.getCurrentPosition() - mergerMusic.startPosition + mergerMusic.startPositionInList
        }
        return 0
    }

    fun play() {
        requestAudioFocus()
        if (currentIndex in mergerList.indices) {
            val mergerMusic = mergerList[currentIndex]
            mergerMusic.play()
        }
        setCurrentState(BassUtils.STATE_PLAYING, true)
        updatePosition()
    }

    fun pause() {
        for (mergerMusic in mergerList) {
            mergerMusic.pause()
        }
        setCurrentState(BassUtils.STATE_PAUSED)
    }

    fun stop() {
        for (mergerMusic in mergerList) {
            mergerMusic.stop()
        }
        setCurrentState(BassUtils.STATE_STOP)
    }

    fun seekTo(position: Int) {
        val isPlaying = isPlaying()
        if (position >= totalDuration) {//跳转到末尾
            if (isPlaying) {
                pause()
            }

            currentIndex = mergerList.size - 1
            val mergerMusic = mergerList[currentIndex]
            mergerMusic.seekTo(position - mergerMusic.startPositionInList + mergerMusic.startPosition)
            if (isPlaying) {
                play()
            }
        } else {
            if (isPlaying) {
                for (mergerMusic in mergerList) {
                    mergerMusic.pause()
                }
            }

            for (i in mergerList.indices) {
                val mergerMusic = mergerList[i]
                if (position >= mergerMusic.startPositionInList &&
                    position < mergerMusic.endPositionInList
                ) {
                    currentIndex = i
                    mergerMusic.seekTo(
                        position - mergerMusic.startPositionInList + mergerMusic.startPosition
                    )
                    if (isPlaying) {
                        mergerMusic.play()
                    }
                }
            }

            updatePosition()
        }
    }

    private fun updatePosition() {
        val currentPosition = getCurrentPosition()
        currentPositionModel.postValue(currentPosition)

        if (currentIndex in mergerList.indices) {
            val mergerMusic = mergerList[currentIndex]
            if (currentPosition >= mergerMusic.endPositionInList) {
                playNext(true)
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

    fun playNext(isNext: Boolean) {
        if (isNext) {
            playIndex(currentIndex + 1)
        } else {
            playIndex(currentIndex - 1)
        }
    }

    fun playIndex(index: Int) {
        if (index in mergerList.indices) {
            seekTo(mergerList[index].startPositionInList)
            play()
        } else if (index >= mergerList.size) {
            pause()
            seekTo(0)
            setCurrentState(BassUtils.STATE_COMPLETED)//播放完成
        }
    }

    fun playOrPause() {
        if (isPlaying()) {
            pause()
        } else {
            play()
        }
    }

    override fun onAudioFocusLoss() {
        pause()
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
        for (mergerMusic in mergerList) {
            mergerMusic.release()
        }
    }

    override fun saveFile(targetPath: String, format: String) {
        val saver = MergerSaver.Builder()
            .setMergerList(mergerList)
            .setTargetPath(targetPath)
            .setAdjustTime(adjustTime)
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