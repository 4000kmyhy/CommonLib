package com.lib.automix.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lib.automix.AutoMixConstant
import com.lib.automix.R
import com.lib.automix.utils.AutoMixConfigUtils
import com.lib.automix.utils.OnAutoMixListener
import com.lib.automix.utils.PermissionUtils
import com.lib.media.entity.Music
import com.lib.media.utils.MusicLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * desc:
 **
 * user: xujj
 * time: 2024/6/28 15:04
 **/
class AutoMixViewModel(application: Application) : AndroidViewModel(application),
    OnAutoMixListener {

    companion object {
        const val NO_PERMISSION = 0
        const val HAS_PERMISSION = 1
    }

    private val _uiState = MutableStateFlow(NO_PERMISSION)
    val uiState: StateFlow<Int> = _uiState

    private val localMusicList = mutableStateOf<List<Music>?>(null)//本地音乐集合

    val currentMusic = mutableStateOf<Music?>(null)
    val transitionMusic = mutableStateOf<Music?>(null)//转换中的音乐

    val currentDeck = mutableStateOf(true)

    val playState = MutableStateFlow(false)
    val currentPosition = MutableStateFlow(0)
    val duration = MutableStateFlow(0)

    private val _transitionState = MutableStateFlow(false)
    val transitionState: StateFlow<Boolean> = _transitionState
    private val _transitionPosition = MutableStateFlow(0)
    val transitionPosition: StateFlow<Int> = _transitionPosition
    private val _volumeProgress = MutableStateFlow(100)
    val volumeProgress: StateFlow<Int> = _volumeProgress

    val musicItems = MutableLiveData<List<Music>>()

    private val mHandler = Handler(Looper.getMainLooper())

    val fadeTime = MutableStateFlow(10 * 1000L)
    val fadeEndTime = MutableStateFlow(30 * 1000L)
    val shuffleMode = MutableStateFlow(false)
    val syncMode = MutableStateFlow(true)

    private val config = AutoMixConfigUtils.getConfig()

    init {
        config?.enterAutoMix(this)

        shuffleMode.value = AutoMixConstant.getShuffleModeSP(application)
        fadeTime.value = AutoMixConstant.getFadeTimeSP(application)
        fadeEndTime.value = AutoMixConstant.getFadeEndTimeSP(application)
        syncMode.value = AutoMixConstant.getSyncModeSP(application)
        config?.isAutoSync = syncMode.value
    }

    fun updateUiState() {
        if (PermissionUtils.hasPermission(getApplication())) {
            _uiState.value = HAS_PERMISSION
            if (localMusicList.value.isNullOrEmpty()) {
                loadMusicData()
            }
        } else {
            _uiState.value = NO_PERMISSION
        }
    }

    private fun loadMusicData() {
        viewModelScope.launch(Dispatchers.IO) {
            val musicList = MusicLoader.getMusicList(getApplication())
            withContext(Dispatchers.Main) {
                localMusicList.value = musicList
                musicItems.value = ArrayList()

                config?.let { config ->
                    val musicA = config.getCurrentMusic(true)
                    val musicB = config.getCurrentMusic(false)

                    if (musicA != null) {
                        //1. A碟歌曲存在，设置为当前播放歌曲
                        currentMusic.value = musicA
                        updateCurrentDeck(true)
                        _volumeProgress.value = 0
                        startTransition(true, musicA)

                        if (musicB != null) {
                            //B碟歌曲存在，插入队列，再随机插入1首歌曲
                            insertRandomMusic(1, musicB)
                        } else {
                            //B碟歌曲不存在，随机插入两首歌曲
                            insertRandomMusic(2)
                        }
                    } else if (musicB != null) {
                        //2. A碟歌曲不存在，B碟歌曲存在
                        currentMusic.value = musicB
                        updateCurrentDeck(false)
                        _volumeProgress.value = 100
                        startTransition(false, musicB)

                        //随机插入两首歌曲
                        insertRandomMusic(2)
                    } else {
                        //3. A、B碟歌曲都不存在
                        val music = getRandomMusic()
                        if (music != null) {
                            currentMusic.value = music
                            updateCurrentDeck(true)
                            _volumeProgress.value = 0
                            startTransition(true, music)
                        }

                        //随机插入两首歌曲
                        insertRandomMusic(2)
                    }
                }
            }
        }
    }

    private fun getRandomMusic(): Music? {
        localMusicList.value.let {
            if (!it.isNullOrEmpty()) {
                val index = Random.nextInt(it.size)
                return it[index]
            }
        }
        return null
    }

    private fun insertRandomMusic(count: Int, music: Music? = null) {
        val musics = ArrayList<Music>()
        if (music != null) {
            musics.add(music)
        }
        repeat(count) {
            val randomMusic = getRandomMusic()
            if (randomMusic != null) {
                musics.add(randomMusic)
            }
        }
        insertMusics(Int.MAX_VALUE, musics)
    }

    fun insertMusic(index: Int, music: Music) {
        musicItems.value = musicItems.value?.toMutableList()?.also {
            if (index in it.indices) {
                it.add(index, music)
            } else {
                it.add(music)
            }
        }
    }

    fun insertMusics(index: Int, musics: List<Music>) {
        musicItems.value = musicItems.value?.toMutableList()?.also {
            if (index in it.indices) {
                it.addAll(index, musics)
            } else {
                it.addAll(musics)
            }
        }
    }

    fun removeMusic(index: Int) {
        musicItems.value = musicItems.value?.toMutableList()?.also {
            if (index in it.indices) {
                it.removeAt(index)
            }
            if (it.size == 1) {//只有一首歌曲，新插入一首
                getRandomMusic()?.let { it1 -> it.add(it1) }
            } else if (it.size == 0) {
                getRandomMusic()?.let { it1 -> it.add(it1) }
                getRandomMusic()?.let { it1 -> it.add(it1) }
            }
        }
    }

    /**
     * 转换下一首
     */
    fun transitionNext(force: Boolean = false) {
        if (shuffleMode.value) {
            musicItems.value?.let {
                val index = Random.nextInt(it.size)
                startTransition(index, force)
            }
        } else {
            startTransition(0, force)
        }
    }

    fun startTransition(index: Int, force: Boolean = false) {
        if (transitionState.value && !force) {
            Toast.makeText(
                getApplication(),
                R.string.disable_transition,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            musicItems.value?.let {
                if (index in it.indices) {
                    val music = it[index]
                    val isDeckA = currentDeck.value
                    transitionMusic.value = music
                    startTransition(!isDeckA, music)
                }
            }
            removeMusic(index)
        }
    }

    private fun startTransition(isDeckA: Boolean, music: Music) {
        config?.startTransition(isDeckA, music, fadeTime.value)
        //auto fade
        mHandler.removeCallbacks(autoFadeRunnable)
        mHandler.postDelayed(autoFadeRunnable, 100)
    }

    private val autoFadeRunnable = object : Runnable {
        override fun run() {
            if (!transitionState.value) {
                val currentPosition = config?.getCurrentPosition(currentDeck.value) ?: 0
                val duration = config?.getDuration(currentDeck.value) ?: 0
                val rateRatio = config?.getRateRatio(currentDeck.value) ?: 1f
                if (duration > 0 && duration - currentPosition < fadeEndTime.value * rateRatio) {
                    transitionNext()
                }
            }
            mHandler.postDelayed(this, 100)
        }
    }

    fun playOrPause() {
        config?.playOrPause(currentDeck.value, transitionState.value, fadeTime.value)
    }

    private var isPlayingBeforeScratch = false
    private var scratchTime: Long = 0

    fun onStartTrackingTouch() {
        isPlayingBeforeScratch = config?.isPlaying(currentDeck.value) ?: false
        config?.pause(currentDeck.value)
        scratchTime = System.currentTimeMillis()
    }

    fun onProgressChanged(progress: Int) {
        //避免转动过程中按下播放键
        config?.pause(currentDeck.value)
        val timeDiff = System.currentTimeMillis() - scratchTime
        scratchTime = System.currentTimeMillis()
        config?.startScratch(currentDeck.value, progress, timeDiff.toInt())
    }

    fun onStopTrackingTouch() {
        if (isPlayingBeforeScratch) {
            config?.play(currentDeck.value)
        }
        config?.pauseScratch(currentDeck.value)
    }

    fun toggleShuffleMode() {
        val value = !shuffleMode.value
        shuffleMode.value = value
        AutoMixConstant.setShuffleModeSP(getApplication(), value)
    }

    fun setFadeTime(time: Long) {
        fadeTime.value = time
        AutoMixConstant.setFadeTimeSP(getApplication(), time)
    }

    fun setFadeEndTime(time: Long) {
        fadeEndTime.value = time
        AutoMixConstant.setFadeEndTimeSP(getApplication(), time)
    }

    fun toggleSyncMode() {
        val value = !syncMode.value
        syncMode.value = value
        AutoMixConstant.setSyncModeSP(getApplication(), value)
        config?.isAutoSync = value
    }

    private fun updateCurrentDeck(isDeckA: Boolean) {
        currentDeck.value = isDeckA
        config?.updateCurrentDeck(isDeckA)
        //转盘的总时长
        duration.value = config?.getDuration(currentDeck.value) ?: 0
    }

    private val updatePositionRunnable = object : Runnable {
        override fun run() {
            config?.also {
                currentPosition.value = it.getCurrentPosition(currentDeck.value)
                if (transitionState.value) {
                    _transitionPosition.value = it.getCurrentPosition(!currentDeck.value)
                    _volumeProgress.value = it.getVolumeProgress()
                    mHandler.postDelayed(this, 50)
                } else {
                    if (it.isPlaying(currentDeck.value)) {
                        mHandler.postDelayed(this, 50)
                    }
                }
            }
        }
    }

    override fun onPlayStateChanged(isDeckA: Boolean, state: Int, playStateChanged: Boolean) {
        if (transitionState.value) {
            when (state) {
                5 /* STATE_COMPLETED */ -> {
                    if (currentDeck.value != isDeckA) {//转换过程中另一碟播完
                        mHandler.post {
                            transitionNext(true)
                        }
                    }
                }
            }
            if (playStateChanged) {
                playState.value = config?.isPlaying(isDeckA) ?: false ||
                        config?.isPlaying(!isDeckA) ?: false
                mHandler.post(updatePositionRunnable)
            }
        } else {
            if (currentDeck.value == isDeckA) {
                when (state) {
                    1 /* STATE_PREPARED */ -> {
                        duration.value = config?.getDuration(isDeckA) ?: 0
                    }
                }
                if (playStateChanged) {
                    playState.value = config?.isPlaying(isDeckA) ?: false
                    mHandler.post(updatePositionRunnable)
                }
            }
        }
    }

    override fun fadeVolume(isFading: Boolean) {
        _transitionState.value = isFading
        if (!isFading) {
            updateCurrentDeck(!currentDeck.value)
            currentMusic.value = transitionMusic.value
            transitionMusic.value = null

            playState.value = config?.isPlaying(currentDeck.value) ?: false
            mHandler.post(updatePositionRunnable)
        }
    }

    override fun onNotifyPlay() {
        playOrPause()
    }

    override fun onCleared() {
        super.onCleared()
        destroy()
    }

    fun destroy() {
        config?.exitAutoMix(this)
        mHandler.removeCallbacksAndMessages(null)
    }
}