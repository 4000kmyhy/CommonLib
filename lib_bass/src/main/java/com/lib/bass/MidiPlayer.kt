package com.lib.bass

import android.util.Log
import com.lib.bass.utils.BassUtils.STATE_COMPLETED
import com.lib.bass.utils.BassUtils.STATE_ERROR
import com.lib.bass.utils.BassUtils.STATE_IDLE
import com.lib.bass.utils.BassUtils.STATE_PAUSED
import com.lib.bass.utils.BassUtils.STATE_PLAYING
import com.lib.bass.utils.BassUtils.STATE_PREPARED
import com.lib.bass.utils.BassUtils.STATE_STOP
import com.un4seen.bass.BASS
import com.un4seen.bass.BASSMIDI
import com.un4seen.bass.BASSMIDI.BASS_MIDI_FONT
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2026/5/29 16:16
 **/

fun interface IMidiPlayerListener {
    fun onPlayStateChanged(
        player: MidiPlayer,
        state: Int,
        playStateChanged: Boolean,
        errorCode: Int
    )
}

class MidiPlayer {

    companion object {
        private const val TAG = "MidiPlayer"
    }

    var currentState = STATE_IDLE
        private set
    var chan = 0 // channel handle
        private set
    var font = 0 // soundfont
        private set
    private var speed = 1f // tempo adjustment

    //音频总长度：秒
    private var mTotalTime = 0.0

    private var mListener: IMidiPlayerListener? = null

    fun setOnMidiPlayerListener(listener: IMidiPlayerListener) {
        mListener = listener
    }

    private fun setCurrentState(
        state: Int,
        force: Boolean = false,
        errorCode: Int = BASS.BASS_OK
    ) {
        Log.d(TAG, "setCurrentState: $state")
        val hasChanged = currentState != state
        //播放状态改变
        val playStateChanged =
            (currentState == STATE_PLAYING) != (state == STATE_PLAYING) || force
        currentState = state
        if (hasChanged || force) { //状态发生改变或强制更新
            mListener?.onPlayStateChanged(this, state, playStateChanged, errorCode)
        }
    }

    fun setSoundfont(
        file: String,
        onSuccess: () -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        val newFont = BASSMIDI.BASS_MIDI_FontInit(file, 0)
        if (newFont == 0) {
            Log.d(TAG, "setSoundfont: failed=" + BASS.BASS_ErrorGetCode())
            onFailed()
        } else {
            val sf = arrayOf(BASS_MIDI_FONT())
            sf[0].font = newFont
            sf[0].preset = -1 // use all presets
            sf[0].bank = 0 // use default bank(s)
            BASSMIDI.BASS_MIDI_StreamSetFonts(0, sf, 1) // set default soundfont
            if (chan != 0) {// set for current stream too
                BASSMIDI.BASS_MIDI_StreamSetFonts(chan, sf, 1)
            }
            BASSMIDI.BASS_MIDI_FontFree(font) // free old soundfont
            font = newFont
            Log.d(TAG, "setSoundfont: font=$font")
            onSuccess()
        }
    }

    fun hasSoundfont() = font != 0

    fun setMidiSource(file: String) {
        synchronized(this) {
            release()
            chan = BASSMIDI.BASS_MIDI_StreamCreateFile(
                file,
                0,
                0,
                BASSMIDI.BASS_MIDI_NOFX or BASSMIDI.BASS_MIDI_SINCINTER,
                1
            )
            if (chan != 0) {
                prepare()
            } else {
                setCurrentState(STATE_ERROR, true, BASS.BASS_ErrorGetCode())
                Log.d(TAG, "setMidiSource: error=" + BASS.BASS_ErrorGetCode())
            }
        }
    }

    fun setMidiSource(channel: Int) {
        synchronized(this) {
            release()
            chan = channel
            if (chan != 0) {
                prepare()
            } else {
                setCurrentState(STATE_ERROR, true, BASS.BASS_ErrorGetCode())
                Log.d(TAG, "setMidiSource: error=" + BASS.BASS_ErrorGetCode())
            }
        }
    }

    private fun prepare() {
        //获取总时长
        mTotalTime = BASS.BASS_ChannelBytes2Seconds(
            chan,
            BASS.BASS_ChannelGetLength(chan, BASS.BASS_POS_BYTE)
        )

        BASS.BASS_ChannelSetSync(
            chan,
            BASS.BASS_SYNC_END,
            0,
            endSync,
            0
        )

        setSpeed(speed)

        // get default soundfont in case of matching soundfont being used
        val sf = arrayOf(BASS_MIDI_FONT())
        if (BASSMIDI.BASS_MIDI_StreamGetFonts(chan, sf, 1) > 0) {
            font = sf[0].font
        }

        // limit CPU usage to 70%
        BASS.BASS_ChannelSetAttribute(
            chan,
            BASSMIDI.BASS_ATTRIB_MIDI_CPU,
            70f
        )

        setCurrentState(STATE_PREPARED, true)
    }

    private val endSync = BASS.SYNCPROC { handle, channel, data, user ->
        setCurrentState(STATE_COMPLETED)
    }

    fun play() {
        if (chan == 0) return
        if (isPlaying(true)) return
        BASS.BASS_ChannelPlay(chan, false)
        setCurrentState(STATE_PLAYING)
    }

    fun pause() {
        if (chan == 0) return
        if (!isPlaying(true)) return
        BASS.BASS_ChannelPause(chan)
        setCurrentState(STATE_PAUSED)
    }

    fun stop() {
        if (chan == 0) return
        BASS.BASS_ChannelStop(chan)
        setCurrentState(STATE_STOP)
    }

    fun seekTo(tick: Long) {
        if (chan == 0) return
        BASS.BASS_ChannelSetPosition(
            chan,
            tick,
            BASSMIDI.BASS_POS_MIDI_TICK
        )
    }

    fun seekTo(position: Int): Boolean {
        if (chan == 0) return false
        val pos = if (mTotalTime > 0) {
            (position / 1000.0).coerceIn(0.0, mTotalTime - 0.001)
        } else {
            0.0
        }
        BASS.BASS_ChannelSetPosition(
            chan,
            BASS.BASS_ChannelSeconds2Bytes(chan, pos),
            BASS.BASS_POS_BYTE
        )
        return if (BASS.BASS_ErrorGetCode() == 0) {
            setCurrentState(if (isPlaying()) STATE_PLAYING else STATE_PAUSED, true)
            true
        } else {//7.invalid position
            val currentPos = getCurrentPosition() / 1000.0 //当前进度
            //跳转了无效位置，返回当前进度
            BASS.BASS_ChannelSetPosition(
                chan,
                BASS.BASS_ChannelSeconds2Bytes(chan, currentPos),
                BASS.BASS_POS_BYTE
            )
            setCurrentState(if (isPlaying()) STATE_PLAYING else STATE_PAUSED, true)
            false
        }
    }

    fun release() {
        if (chan != 0) {
            BASS.BASS_StreamFree(chan)
            chan = 0
        }
        mTotalTime = 0.0
        setCurrentState(STATE_IDLE)
    }

    fun isPlaying(isReal: Boolean = false): Boolean {
        if (chan == 0) return false
        val state = BASS.BASS_ChannelIsActive(chan) == BASS.BASS_ACTIVE_PLAYING
        return if (isReal) {
            state
        } else {
            state && currentState == STATE_PLAYING
        }
    }

    fun getCurrentPosition(): Int {
        if (chan == 0) return 0
        val second = BASS.BASS_ChannelBytes2Seconds(
            chan,
            BASS.BASS_ChannelGetPosition(chan, BASS.BASS_POS_BYTE)
        )
        return (second * 1000).roundToInt().coerceAtLeast(0)
    }

    fun getDuration(): Int {
        if (chan == 0) return 0
        return (mTotalTime * 1000).roundToInt().coerceAtLeast(0)
    }

    fun setSpeed(speed: Float) {
        if (chan == 0) return
        this.speed = speed
        BASS.BASS_ChannelSetAttribute(
            chan,
            BASSMIDI.BASS_ATTRIB_MIDI_SPEED,
            speed
        )
    }

    fun getBpm(): Float {
        if (chan == 0) return 0f
        val tempo = BASSMIDI.BASS_MIDI_StreamGetEvent(
            chan,
            0,
            BASSMIDI.MIDI_EVENT_TEMPO
        )
        return if (tempo > 0) {
            (speed * 60000000.0 / tempo).toFloat()
        } else {
            0f
        }
    }

    fun setFxEnable(enable: Boolean) {
        if (chan == 0) return
        if (enable) {
            BASS.BASS_ChannelFlags(
                chan,
                0,
                BASSMIDI.BASS_MIDI_NOFX
            )
        } else {
            BASS.BASS_ChannelFlags(
                chan,
                BASSMIDI.BASS_MIDI_NOFX,
                BASSMIDI.BASS_MIDI_NOFX
            )
        }
    }

    fun destroy() {
        release()
        if (font != 0) {
            BASSMIDI.BASS_MIDI_FontFree(font)
            font = 0
        }
    }
}