package com.lib.bass

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.FloatRange
import com.lib.bass.utils.BassUtils
import com.lib.bass.utils.IPlayerListener
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import kotlin.math.abs

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/27 11:51
 **/
class BassPlayer {

    companion object {

        private const val TAG = "BassPlayer"
    }

    var playChan = 0 //创建播放解码流的通道
        private set
    var playHandle = 0 //用于播放、暂停等的句柄
        private set
    private var endHandle = 0 //用于监听播放完成
    private var reverseChan = 0//回放句柄
    private var bpmChan = 0 //获取bpm

    var scratchHandle = 0 //搓碟音效句柄
        private set

    private var isLoop = false //是否循环
    var isReverse = false //是否回放
        private set

    //音频时长
    private var mDuration: Int = 0

    //当前播放状态
    var currentState = BassUtils.STATE_IDLE
        private set

    private val mHandler = Handler(Looper.getMainLooper())

    private var playerListener: IPlayerListener? = null

    fun setOnPlayerListener(listener: IPlayerListener) {
        playerListener = listener
    }

    private fun setCurrentState(state: Int, force: Boolean = false) {
        //播放状态改变
        val playStateChanged =
            (currentState == BassUtils.STATE_PLAYING) != (state == BassUtils.STATE_PLAYING) || force
        Log.d(TAG, "setCurrentState: $state $playStateChanged")
        currentState = state
        playerListener?.onPlayStateChanged(this, state, playStateChanged)
    }

    fun setDataSource(
        path: String,
        isLoop: Boolean = false,
        isCreateScratch: Boolean = false
    ) {
        synchronized(this) {
            Log.d(TAG, "setDataSource: $path")
            //重置
            reset()
            //进入加载状态
            setCurrentState(BassUtils.STATE_LOADING)
            //创建解码通道
            playChan = BassUtils.streamCreateFile(path)
            Log.d(TAG, "setDataSource: " + playChan + " " + currentState)
            //加载完前已经释放
            if (currentState == BassUtils.STATE_IDLE) {
                reset()
                return
            }
            if (playChan != 0) {
                prepare(isLoop, isCreateScratch)
                //准备完成
                setCurrentState(BassUtils.STATE_PREPARED, true)
            } else {
                BassUtils.logError("BASS_StreamCreateFile")
                setCurrentState(BassUtils.STATE_ERROR)
            }
        }
    }

    fun setDataSource(
        context: Context,
        assetName: String,
        isLoop: Boolean = false,
        isCreateScratch: Boolean = false
    ) {
        synchronized(this) {
            Log.d(TAG, "setDataSource: $assetName")
            //重置
            reset()
            //进入加载状态
            setCurrentState(BassUtils.STATE_LOADING)
            //创建解码通道
            playChan = BASS.BASS_StreamCreateFile(
                BASS.Asset(context.assets, assetName), 0, 0,
                BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN
            )
            //加载完前已经释放
            if (currentState == BassUtils.STATE_IDLE) {
                reset()
                return
            }
            if (playChan != 0) {
                prepare(isLoop, isCreateScratch)
                //准备完成
                setCurrentState(BassUtils.STATE_PREPARED, true)
            } else {
                BassUtils.logError("BASS_StreamCreateFile")
                setCurrentState(BassUtils.STATE_ERROR)
            }
        }
    }

    fun setDataSourceURL(
        url: String,
        isLoop: Boolean = false,
        isCreateScratch: Boolean = false
    ) {
        synchronized(this) {
            Log.d(TAG, "setDataSourceURL: $url")
            //重置
            reset()
            //进入加载状态
            setCurrentState(BassUtils.STATE_LOADING)
            //创建解码通道
            playChan = BASS.BASS_StreamCreateURL(
                url, 0,
                BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN,
                null,
                null
            )
            //加载完前已经释放
            if (currentState == BassUtils.STATE_IDLE) {
                reset()
                return
            }
            if (playChan != 0) {
                prepare(isLoop, isCreateScratch)
                //准备完成
                setCurrentState(BassUtils.STATE_PREPARED, true)
            } else {
                BassUtils.logError("BASS_StreamCreateURL")
                setCurrentState(BassUtils.STATE_ERROR)
            }
        }
    }

    private fun prepare(isLoop: Boolean, isCreateScratch: Boolean) {
        //添加回放效果
        reverseChan = BASS_FX.BASS_FX_ReverseCreate(
            playChan,
            2.0F,
            BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN
        )
        //设置向前播放
        setReverse(isReverse)

        //添加采样率效果，会对BASS_STREAM_DECODE解码
        playHandle = BASS_FX.BASS_FX_TempoCreate(
            reverseChan,
            BASS.BASS_SAMPLE_LOOP or BASS_FX.BASS_FX_FREESOURCE
        )
        //设置循环
        setLoop(isLoop)

        //获取总时长，会对BASS_STREAM_DECODE解码
        mDuration = BassUtils.getDuration(playHandle)

        //监听播放完成
        endHandle = BASS.BASS_ChannelSetSync(
            playHandle,
            BASS.BASS_SYNC_END,
            0,
            endSync,
            0
        )

        if (isCreateScratch) {
            scratchHandle = BASS_FX.BASS_FX_TempoCreate(
                playChan,
                BASS.BASS_SAMPLE_LOOP or BASS_FX.BASS_FX_FREESOURCE
            )
        }
    }

    private val endSync = BASS.SYNCPROC { handle, channel, data, user ->
        if (isLoop) {
            setCurrentState(
                if (isPlaying()) BassUtils.STATE_PLAYING else BassUtils.STATE_PAUSED,
                true
            )
        } else {
            setCurrentState(BassUtils.STATE_COMPLETED)
        }
    }

    fun play() {
        if (isPlaying(true)) return
        mHandler.removeCallbacks(pauseRunnable)
        if (playHandle != 0) {
            BASS.BASS_ChannelSetAttribute(
                playHandle,
                BASS.BASS_ATTRIB_FREQ,
                44100f
            )
            BASS.BASS_ChannelPlay(playHandle, false)
            Log.d(TAG, "play1: " + isPlaying(true))
            setCurrentState(BassUtils.STATE_PLAYING)
            Log.d(TAG, "play2: " + isPlaying(true))
        }
    }

    fun pause(isFade: Boolean = false) {
        if (!isPlaying(true)) return
        mHandler.removeCallbacks(pauseRunnable)
        if (playHandle != 0) {
            if (isFade) {
                BASS.BASS_ChannelSlideAttribute(
                    playHandle,
                    BASS.BASS_ATTRIB_FREQ,
                    100f,
                    500
                )
                mHandler.postDelayed(pauseRunnable, 500)
            } else {
                BASS.BASS_ChannelPause(playHandle)
            }
            setCurrentState(BassUtils.STATE_PAUSED)
        }
    }

    private val pauseRunnable = Runnable {
        BASS.BASS_ChannelPause(playHandle)
    }

    fun stop() {
        if (playHandle != 0) {
            BASS.BASS_ChannelStop(playHandle)
            setCurrentState(BassUtils.STATE_STOP)
        }
    }

    fun reset() {
        stop()
        reset_()
    }

    fun release() {
        if (playHandle != 0 && endHandle != 0) {
            BASS.BASS_ChannelRemoveSync(playHandle, endHandle)
        }
        reset_()
    }

    /**
     * 重置，释放句柄
     */
    private fun reset_() {
        if (playHandle != 0) {
            BASS.BASS_StreamFree(playHandle)
            playHandle = 0
        }
        if (bpmChan != 0) {
            BASS_FX.BASS_FX_BPM_Free(bpmChan)
            bpmChan = 0
        }
        if (scratchHandle != 0) {
            BASS.BASS_StreamFree(scratchHandle)
            scratchHandle = 0
        }
        mHandler.removeCallbacksAndMessages(null)
        setCurrentState(BassUtils.STATE_IDLE)
    }

    fun seekTo(position: Int) {
        BassUtils.seekTo(playHandle, position)
        setCurrentState(if (isPlaying()) BassUtils.STATE_PLAYING else BassUtils.STATE_PAUSED, true)
    }

    fun isPlaying(isReal: Boolean = false): Boolean {
        val state = BassUtils.isPlaying(playHandle)
        return if (isReal) {
            state
        } else {
            state && currentState == BassUtils.STATE_PLAYING
        }
    }

    fun getCurrentPosition(): Int {
        return BassUtils.getCurrentPosition(playHandle)
    }

    fun getDuration(): Int {
        if (playHandle == 0) return 0
        return mDuration
    }

    fun setLoop(isLoop: Boolean) {
        this.isLoop = isLoop
        BassUtils.setLooping(playHandle, isLoop)
    }

    fun setReverse(isReverse: Boolean) {
        this.isReverse = isReverse
        BassUtils.setReverse(reverseChan, isReverse)
    }

    fun setVolume(volume: Float) {
        BassUtils.setVolume(playHandle, volume)
    }

    fun setPan(@FloatRange(from = -1.0, to = 1.0) pan: Float) {
        BassUtils.setPan(playHandle, pan)
    }

    fun setTempo(@FloatRange(from = -50.0, to = 100.0) tempo: Float) {
        BassUtils.setTempo(playHandle, tempo)
    }

    fun setPitch(@FloatRange(from = -10.0, to = 10.0) pitch: Float) {
        BassUtils.setPitch(playHandle, pitch)
    }

    fun setFreq(freq: Float) {
        BassUtils.setFreq(playHandle, freq)
    }

    fun getRateRatio() = BASS_FX.BASS_FX_TempoGetRateRatio(playHandle)

    /**
     * 获取实时bpm
     */
    fun getBpm(path: String, position: Int = 0): Float {
        if (bpmChan == 0) {
            bpmChan = BassUtils.streamCreateFile(path)
        }
        val startSec = position / 1000.0
        val maxSec = mDuration / 1000.0
        val endSec = Math.min(startSec + 10, maxSec - 1)
        //官方demo
        var bpmValue = BASS_FX.BASS_FX_BPM_DecodeGet(
            bpmChan,
            startSec,
            endSec,
            0,
            BASS_FX.BASS_FX_BPM_MULT2 or BASS_FX.BASS_FX_FREESOURCE,
            null,
            0
        )
        if (bpmValue <= 0) bpmValue = 120f
        if (bpmValue >= 240) bpmValue /= 2
        bpmValue = Math.round(bpmValue * 10) / 10f//四舍五入保留一位小数
        return bpmValue
    }

    fun getNewBpm(bpm: Float): Float {
        if (playHandle != 0) {
            var bpmValue = bpm * getRateRatio()
            bpmValue = Math.round(bpmValue * 10) / 10f//四舍五入保留一位小数
            return bpmValue
        } else {
            return bpm
        }
    }

    fun scratching(positionDiff: Int, timeDiff: Int) {
        if (scratchHandle == 0) return
        pauseScratch()
        if (timeDiff <= 0) return
        if (abs(positionDiff) <= 10) return

        var newRate =
            44100f * abs(positionDiff) / 60 * 1000 / 128 / timeDiff * 2f
        newRate = Math.min(newRate, 100000f)
        newRate = Math.max(newRate, 100f)

        if (!BASS.BASS_ChannelIsSliding(scratchHandle, BASS.BASS_ATTRIB_FREQ)) {
            BASS.BASS_ChannelSlideAttribute(
                scratchHandle,
                BASS.BASS_ATTRIB_FREQ,
                newRate,
                timeDiff
            )
        } else {
            BASS.BASS_ChannelSetAttribute(
                scratchHandle,
                BASS.BASS_ATTRIB_FREQ,
                newRate
            )
        }
        if (!BassUtils.isPlaying(scratchHandle)) {
            BASS.BASS_ChannelPlay(scratchHandle, false)
        }
    }

    fun pauseScratch() {
        if (BassUtils.isPlaying(scratchHandle)) {
            BASS.BASS_ChannelPause(scratchHandle)
        }
    }

    fun setScratchVolume(volume: Float) {
        BassUtils.setVolume(scratchHandle, volume)
    }

    fun setScratchPan(@FloatRange(from = -1.0, to = 1.0) pan: Float) {
        BassUtils.setPan(scratchHandle, pan)
    }
}