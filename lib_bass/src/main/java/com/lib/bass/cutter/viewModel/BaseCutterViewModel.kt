package com.lib.bass.cutter.viewModel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.lib.bass.utils.BassUtils
import kotlinx.coroutines.Job
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2024/4/8 10:50
 **/
abstract class BaseCutterViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BaseCutterViewModel"
    }

    val playStateModel = MutableLiveData<Pair<Int, Boolean>>()
    val currentPositionModel = MutableLiveData<Int>()

    private val mAudioManager by lazy {
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    protected val mHandler = Handler(Looper.getMainLooper())

    init {
        BassUtils.initBass()
        setCurrentState(BassUtils.STATE_IDLE)
    }

    protected fun setCurrentState(state: Int, force: Boolean = false) {
        //播放状态改变
        val playStateChanged =
            (playStateModel.value?.first == BassUtils.STATE_PLAYING) != (state == BassUtils.STATE_PLAYING) || force
        Log.d(TAG, "setCurrentState: $state $playStateChanged")
        playStateModel.postValue(Pair(state, playStateChanged))
    }

    fun requestAudioFocus() {
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
                onAudioFocusLoss()
            }
        }

    protected abstract fun onAudioFocusLoss()

    protected var saveJob: Job? = null

    val saveProgressModel = MutableLiveData<Int>()
    val saveStateModel = MutableLiveData<Boolean>()

    abstract fun saveFile(targetPath: String, format: String)

    fun cancelSave(targetPath: String?) {
        saveJob?.cancel()
        if (targetPath != null) {
            File(targetPath).delete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        abandonAudioFocus()
        mHandler.removeCallbacksAndMessages(null)
        saveJob?.cancel()
    }
}