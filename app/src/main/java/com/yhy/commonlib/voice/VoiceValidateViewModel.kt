package com.yhy.commonlib.voice

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lib.bass.BassPlayer
import com.lib.bass.utils.BassUtils.STATE_COMPLETED
import com.lib.bass.utils.BassUtils.STATE_PREPARED
import com.lib.bass.utils.BassUtils.initBass
import com.lib.bass.utils.BassUtils.loadWaveformData
import com.yhy.commonlib.api.KieRepository
import com.yhy.commonlib.api.UploadRepository
import com.yhy.commonlib.entity.ValidateInfo
import com.yhy.commonlib.utils.FileUtils
import com.yhy.commonlib.utils.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/15 11:20
 **/
class VoiceValidateViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        const val MIN_DURATION = 10 * 1000L //10s
        const val WAVE_TOTAL_SIZE = 40
    }

    var audioFile by mutableStateOf<File?>(null)
        private set
    val playStateFlow = MutableStateFlow(false)
    val currentPositionFlow = MutableStateFlow(0)
    val durationFlow = MutableStateFlow(0)
    val startPositionFlow = MutableStateFlow(0)
    val endPositionFlow = MutableStateFlow(0)
    val waveformDataFlow = MutableStateFlow(listOf<Int>())
    val languageFlow = MutableStateFlow(Pair("zh", "中文"))//todo en

    private val mPlayer = BassPlayer()
    private var playJob: Job? = null
    private var updatePositionJob: Job? = null

    private var uploadUrl: String? = null

    private val mAudioManager by lazy {
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val repository: KieRepository = KieRepository.getInstance()

    init {
        initBass()

        mPlayer.setOnPlayerListener { player, state, playStateChanged ->
            when (state) {
                STATE_PREPARED -> {
                    durationFlow.value = mPlayer.getDuration()
                    startPositionFlow.value = 0
                    endPositionFlow.value = mPlayer.getDuration()
                }

                STATE_COMPLETED -> {
                    seekTo(startPositionFlow.value)
                    play()
                }
            }
            if (playStateChanged) {
                playStateFlow.value = player.isPlaying()
                updatePosition()
            }
        }
    }

    fun loadUri(uri: Uri) {
        reset()
        val fileName = uri.path?.substringAfterLast('/') ?: "audio_file.mp3"
        val file = File(getApplication<Application>().cacheDir.path, fileName)
        val result = FileUtils.saveUri2File(getApplication(), uri, file)
        Log.d("xxx", "loadUri: $file")
        if (result) {
            audioFile?.delete() //删除旧文件
            audioFile = file
            uploadUrl = null
            setDataSource(file.path)
        }
    }

    fun loadFile(path: String) {
        reset()
        audioFile?.let { //删除旧文件
            if (it.path != path) {
                it.delete()
            }
        }
        audioFile = File(path)
        uploadUrl = null
        setDataSource(path)
    }

    private fun setDataSource(path: String) {
        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.IO) {
            async {
                mPlayer.setDataSource(path)
            }
            async {
                loadWaveformData(
                    path,
                    WAVE_TOTAL_SIZE,
                    onUpdated = {
                        waveformDataFlow.value = it.toList()
                    }
                )
            }
        }
    }

    private fun reset() {
        mPlayer.reset()
        playStateFlow.value = false
        currentPositionFlow.value = 0
        durationFlow.value = 0
        startPositionFlow.value = 0
        endPositionFlow.value = 0
        waveformDataFlow.value = listOf()
    }

    fun playOrPause() {
        if (mPlayer.isPlaying()) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        requestAudioFocus()
        mPlayer.play()
    }

    fun pause() {
        mPlayer.pause()
    }

    fun release() {
        playJob?.cancel()
        mPlayer.release()
    }

    fun seekTo(position: Int) {
        mPlayer.seekTo(position)
    }

    private fun updatePosition() {
        if (startPositionFlow.value >= endPositionFlow.value) { //死循环
            pause()
            return
        }
        updatePositionJob?.cancel()
        updatePositionJob = viewModelScope.launch(Dispatchers.Default) {
            var isPlaying = false
            do {
                isPlaying = playStateFlow.value
                currentPositionFlow.value = mPlayer.getCurrentPosition()
                if (currentPositionFlow.value >= endPositionFlow.value) {
                    seekTo(startPositionFlow.value)
                    break
                }
                if (durationFlow.value > 60 * 1000) {
                    delay(100)
                } else {
                    delay(20)
                }
            } while (isPlaying)
        }
    }

    fun extractPosition(startPosition: Int, endPosition: Int) {
        pause()
        startPositionFlow.value = startPosition
        endPositionFlow.value = endPosition
    }

    fun updateLanguage(language: Pair<String, String>) {
        languageFlow.value = language
    }

    fun generateValidation(
        onStart: () -> Unit,
        onSuccess: (ValidateInfo) -> Unit,
        onFailed: () -> Unit
    ) {
        onStart()
        viewModelScope.launch {
            val file = audioFile
            if (file != null) {
                Log.d("xxx", "generateValidation: file=$file")
                if (durationFlow.value >= MIN_DURATION) {
                    val url = uploadUrl ?: UploadRepository.fileToUrl(file) //切歌后重新上传
                    if (url != null) {
                        Log.d("xxx", "generateValidation: uploadUrl=$url")
                        uploadUrl = url
                        toGenerateValidation(
                            url = url,
                            onSuccess = onSuccess,
                            onFailed = onFailed
                        )
                    } else {
                        toast("上传文件失败")
                        onFailed()
                    }
                } else {
                    toast("请选择时长至少为10秒的音频文件")
                    onFailed()
                }
            } else {
                toast("请选择音频文件或录制声音")
                onFailed()
            }
        }
    }

    private suspend fun toGenerateValidation(
        url: String,
        onSuccess: (ValidateInfo) -> Unit,
        onFailed: () -> Unit
    ) {
        try {
            val response = repository.generateValidation(
                voiceUrl = url,
                vocalStartS = startPositionFlow.value / 1000,
                vocalEndS = endPositionFlow.value / 1000,
                language = languageFlow.value.first
            )
            Log.d("xxx", "toGenerateValidation: response=$response")
            if (response.code == 200) {
                val taskId = response.data?.taskId
                if (!taskId.isNullOrEmpty()) {
                    val validateInfo = repository.queryValidateInfo(taskId)
                    if (validateInfo != null) {
                        if (!validateInfo.validateInfo.isNullOrEmpty()) {
                            onSuccess(validateInfo)
                        } else {
                            toast(validateInfo.errorMessage.orEmpty())
                            onFailed()
                        }
                    } else {
                        toast("生成验证短句失败")
                        onFailed()
                    }
                } else {
                    onFailed()
                }
            } else {
                toast(response.msg)
                onFailed()
            }
        } catch (e: Exception) {
            Log.d("xxx", "toGenerateValidation: err=$e")
            toast(e.message.orEmpty())
            onFailed()
        }
    }

    private fun toast(text: String) {
        ToastUtils.showToast(getApplication(), text, Toast.LENGTH_LONG)
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
        audioFile?.delete()
    }
}