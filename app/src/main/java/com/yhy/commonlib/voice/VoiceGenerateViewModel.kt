package com.yhy.commonlib.voice

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.lib.bass.BassPlayer
import com.lib.bass.utils.BassUtils.STATE_PREPARED
import com.lib.bass.utils.BassUtils.initBass
import com.yhy.commonlib.api.KieRepository
import com.yhy.commonlib.api.UploadRepository
import com.yhy.commonlib.entity.ValidateInfo
import com.yhy.commonlib.utils.ToastUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/17 15:37
 **/
class VoiceGenerateViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val validateJson = savedStateHandle["validateJson"] ?: ""
    private var mValidate = try {
        Gson().fromJson(validateJson, ValidateInfo::class.java)
    } catch (e: Exception) {
        null
    }

    val validateSentence = MutableStateFlow(mValidate?.validateInfo.orEmpty())

    var audioFile by mutableStateOf<File?>(null)
        private set
    val playStateFlow = MutableStateFlow(false)
    val currentPositionFlow = MutableStateFlow(0)
    val durationFlow = MutableStateFlow(0)

    private val mPlayer = BassPlayer()
    private var playJob: Job? = null
    private var updatePositionJob: Job? = null

    private var uploadUrl: String? = null

    private val mAudioManager by lazy {
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val repository: KieRepository = KieRepository.getInstance()

    var voiceName by mutableStateOf("")
    var description by mutableStateOf("")
    var style by mutableStateOf("")

    //beginner、intermediate、advanced、professional
    var singerSkillLevel by mutableStateOf("beginner")

    var voiceId by mutableStateOf(null as String?)
        private set

    init {
        initBass()

        mPlayer.setOnPlayerListener { player, state, playStateChanged ->
            when (state) {
                STATE_PREPARED -> {
                    durationFlow.value = mPlayer.getDuration()
                }
            }
            if (playStateChanged) {
                playStateFlow.value = player.isPlaying()
                updatePosition()
            }
        }
    }

    fun regenerateValidation(
        onStart: () -> Unit,
        onCompletion: () -> Unit
    ) {
        val requestTaskId = mValidate?.taskId
        if (requestTaskId.isNullOrEmpty()) return
        onStart()
        viewModelScope.launch {
            try {
                val response = repository.regenerateValidation(requestTaskId)
                Log.d("xxx", "regenerateValidation: response=$response")
                if (response.code == 200) {
                    val taskId = response.data?.taskId
                    if (!taskId.isNullOrEmpty()) {
                        val validateInfo = repository.queryValidateInfo(taskId)
                        if (validateInfo != null) {
                            if (!validateInfo.validateInfo.isNullOrEmpty()) {
                                mValidate = validateInfo
                                validateSentence.value = validateInfo.validateInfo
                            } else {
                                toast(validateInfo.errorMessage.orEmpty())
                            }
                        } else {
                            toast("生成验证短句失败")
                        }
                    }
                } else {
                    toast(response.msg)
                }
            } catch (e: Exception) {
                Log.d("xxx", "regenerateValidation: err=$e")
                toast(e.message.orEmpty())
            } finally {
                onCompletion()
            }
        }
    }

    fun loadFile(path: String) {
        reset()
        audioFile?.delete()
        audioFile = File(path)
        uploadUrl = null
        setDataSource(path)
    }

    fun deleteFile() {
        reset()
        audioFile?.delete()
        audioFile = null
        uploadUrl = null
    }

    private fun setDataSource(path: String) {
        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.IO) {
            mPlayer.setDataSource(path)
        }
    }

    private fun reset() {
        mPlayer.reset()
        playStateFlow.value = false
        currentPositionFlow.value = 0
        durationFlow.value = 0
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
        updatePositionJob?.cancel()
        updatePositionJob = viewModelScope.launch(Dispatchers.Default) {
            var isPlaying = false
            do {
                isPlaying = playStateFlow.value
                currentPositionFlow.value = mPlayer.getCurrentPosition()
                if (durationFlow.value > 60 * 1000) {
                    delay(100)
                } else {
                    delay(20)
                }
            } while (isPlaying)
        }
    }

    fun generateVoice(
        onStart: () -> Unit,
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        val requestTaskId = mValidate?.taskId
        if (requestTaskId.isNullOrEmpty()) return
        onStart()
        viewModelScope.launch {
            val file = audioFile
            if (file != null) {
                Log.d("xxx", "generateVoice: file=$file")
                //todo voiceName判空
                val url = uploadUrl ?: UploadRepository.fileToUrl(file) //切歌后重新上传
                if (url != null) {
                    Log.d("xxx", "generateVoice: uploadUrl=$url")
                    uploadUrl = url
                    toGenerateVoice(
                        taskId = requestTaskId,
                        url = url,
                        onSuccess = onSuccess,
                        onFailed = onFailed
                    )
                } else {
                    toast("上传文件失败")
                    onFailed()
                }
            } else {
                toast("请录制声音")
                onFailed()
            }
        }
    }

    private suspend fun toGenerateVoice(
        taskId: String,
        url: String,
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        try {
            val response = repository.generateVoice(
                taskId = taskId,
                verifyUrl = url,
                voiceName = voiceName,
                description = description,
                style = style,
                singerSkillLevel = singerSkillLevel
            )
            Log.d("xxx", "toGenerateVoice: response=$response")
            if (response.code == 200) {
                val taskId = response.data?.taskId
                if (!taskId.isNullOrEmpty()) {
                    val voiceInfo = repository.queryVoiceInfo(taskId)
                    if (voiceInfo != null) {
                        if (!voiceInfo.voiceId.isNullOrEmpty()) {
                            voiceId = voiceInfo.voiceId
                            toast("生成成功：${voiceInfo.voiceId}")
                            onSuccess()
                        } else {
                            toast(voiceInfo.errorMessage.orEmpty())
                            onFailed()
                        }
                    } else {
                        toast("生成声音失败")
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
            Log.d("xxx", "toGenerateVoice: err=$e")
            toast(e.message.orEmpty())
            onFailed()
        }
    }

    fun checkVoice() {
        val taskId = voiceId
        if (taskId.isNullOrEmpty()) return
        viewModelScope.launch {
            try {
                val result = repository.checkVoice(taskId)
                toast("isAvailable = $result")
            } catch (e: Exception) {
                Log.d("xxx", "checkVoice: err=$e")
                toast(e.message.orEmpty())
            }
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