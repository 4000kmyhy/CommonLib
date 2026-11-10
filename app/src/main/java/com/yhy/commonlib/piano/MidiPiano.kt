package com.yhy.commonlib.piano

import android.os.Build
import android.util.Log
import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS
import com.un4seen.bass.BASSMIDI
import com.yhy.commonlib.midi.MidiConstant
import com.yhy.commonlib.midi.MidiNote
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/25 9:38
 **/
class MidiPiano {

    companion object {
        private const val TAG = "MidiPiano"
    }

    var handle = 0
        private set
    private var endHandle = 0
    private var font = 0 // soundfont

    init {
        configureBASS()
        BassUtils.initBass(BASS.BASS_DEVICE_LATENCY)
    }

    /**
     * 配置 BASS 低延迟参数
     */
    private fun configureBASS() {
        // 1. 减小缓冲区（默认 500ms → 20-50ms）
        // 值越小延迟越低，但可能出现爆音
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_BUFFER, 30)  // 30ms

        // 2. 减小更新周期（默认 100ms → 5-10ms）
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_UPDATEPERIOD, 5)  // 5ms

        // 4. Android 专用：启用 AAudio（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            BASS.BASS_SetConfig(BASS.BASS_CONFIG_ANDROID_AAUDIO, 1)
        }

        // 5. 使用更小的浮点缓冲区
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_FLOATDSP, 1)
    }

    fun setSoundfont(file: String) {
        val newFont = BASSMIDI.BASS_MIDI_FontInit(file, 0)
        if (newFont == 0) {
            Log.d(TAG, "setSoundfont: failed=" + BASS.BASS_ErrorGetCode())
        } else {
            val sf = arrayOf(BASSMIDI.BASS_MIDI_FONT())
            sf[0].font = newFont
            sf[0].preset = -1 // use all presets
            sf[0].bank = 0 // use default bank(s)
            BASSMIDI.BASS_MIDI_StreamSetFonts(0, sf, 1) // set default soundfont
            if (handle != 0) {// set for current stream too
                BASSMIDI.BASS_MIDI_StreamSetFonts(handle, sf, 1)
            }
            BASSMIDI.BASS_MIDI_FontFree(font) // free old soundfont
            font = newFont
            Log.d(TAG, "setSoundfont: font=$font")
        }
    }

    fun hasSoundfont() = font != 0

    fun create() {
        handle = BASSMIDI.BASS_MIDI_StreamCreate(
            16,
            BASSMIDI.BASS_MIDI_NOFX or
                    BASSMIDI.BASS_MIDI_SINCINTER or
                    BASS.BASS_STREAM_AUTOFREE or
                    BASS.BASS_SAMPLE_FLOAT,
            44100
        )
        if (handle == 0) {
            Log.d(TAG, "create: failed=" + BASS.BASS_ErrorGetCode())
        } else {
            prepare()
        }
    }

    private fun prepare() {
        // get default soundfont in case of matching soundfont being used
        val sf = arrayOf(BASSMIDI.BASS_MIDI_FONT())
        if (BASSMIDI.BASS_MIDI_StreamGetFonts(handle, sf, 1) > 0) {
            font = sf[0].font
        }

        // limit CPU usage to 70%
        BASS.BASS_ChannelSetAttribute(
            handle,
            BASSMIDI.BASS_ATTRIB_MIDI_CPU,
            70f
        )

        BASS.BASS_ChannelSetAttribute(
            handle,
            BASSMIDI.BASS_ATTRIB_MIDI_PPQN,
            MidiConstant.PPQN.toFloat()
        )

        BASS.BASS_ChannelSetAttribute(
            handle,
            BASS.BASS_ATTRIB_BUFFER,
            30f
        )

        setupEndSync()

        play()
    }

    fun setupEndSync() {
        if (endHandle != 0) {
            BASS.BASS_ChannelRemoveSync(handle, endHandle)
            endHandle = 0
        }
        endHandle = BASS.BASS_ChannelSetSync(
            handle,
            BASS.BASS_SYNC_END,
            0,
            endSync,
            0
        )
    }

    private val endSync = BASS.SYNCPROC { handle, channel, data, user ->
        Log.d("xxx", "midi playEnd: ")
        create()
    }

    fun setProgram(program: Int = 0, channel: Int = 0) {
        if (handle == 0) return
        if (channel != 9) {
            BASSMIDI.BASS_MIDI_StreamEvent(
                handle,
                channel,
                BASSMIDI.MIDI_EVENT_PROGRAM,
                program
            )
        }
    }

    fun noteOn(pitch: Int, velocity: Int = 100, channel: Int = 0) {
        if (handle == 0) return
        play()

        if (pitch in 0..127) {
            val param = (pitch and 0xFF) or ((velocity shl 8) and 0xFF00)
            BASSMIDI.BASS_MIDI_StreamEvent(
                handle,
                channel,
                BASSMIDI.MIDI_EVENT_NOTE,
                param
            )
        }
    }

    fun noteOff(pitch: Int, channel: Int = 0) {
        if (handle == 0) return
        if (pitch in 0..127) {
            val param = (pitch and 0xFF) or (0 shl 8)
            BASSMIDI.BASS_MIDI_StreamEvent(
                handle,
                channel,
                BASSMIDI.MIDI_EVENT_NOTE,
                param
            )
        }
    }

    fun playNoteList(
        noteList: List<MidiNote>,
        channel: Int = 0,
        ppqn: Int = MidiConstant.PPQN,
        bpm: Int = MidiConstant.BPM,
    ) {
        if (handle == 0) return
        resetStream()
        play()

        val events = ArrayList<BASSMIDI.BASS_MIDI_EVENT>()

        // 1. 设置 tempo（微秒/四分音符）
        events.add(BASSMIDI.BASS_MIDI_EVENT().apply {
            event = BASSMIDI.MIDI_EVENT_TEMPO
            param = 60000000 / bpm  // 转换为微秒/四分音符
            tick = 0
        })

        noteList.forEach { note ->
            if (note.pitch in 0..127) {
                val startTick = MidiConstant.secondsToTicks(note.start, ppqn, bpm).toInt()
                val endTick = MidiConstant.secondsToTicks(note.end, ppqn, bpm).toInt()
                val velocity =
                    if (note.velocity > 0) note.velocity.coerceIn(1, 127) else 100

                // Note On
                events.add(BASSMIDI.BASS_MIDI_EVENT().apply {
                    event = BASSMIDI.MIDI_EVENT_NOTE
                    param = (note.pitch and 0xFF) or ((velocity shl 8) and 0xFF00)
                    this.chan = channel
                    tick = startTick
                })

                // Note Off
                events.add(BASSMIDI.BASS_MIDI_EVENT().apply {
                    event = BASSMIDI.MIDI_EVENT_NOTE
                    param = (note.pitch and 0xFF) or (0 shl 8)  // velocity = 0 表示释放
                    this.chan = channel
                    tick = endTick
                })
            }
        }

        events.sortBy { it.tick }

        events.add(BASSMIDI.BASS_MIDI_EVENT().apply {
            event = BASSMIDI.MIDI_EVENT_END
            tick = events.maxOfOrNull { it.tick } ?: 0  // 在最后一个音符后触发
        })

        val mode = BASSMIDI.BASS_MIDI_EVENTS_STRUCT or BASSMIDI.BASS_MIDI_EVENTS_TIME
        BASSMIDI.BASS_MIDI_StreamEvents(
            handle,
            mode,
            events.toTypedArray(),
            events.size
        )
    }

    fun resetStream() {
        if (handle == 0) return

        // 2. 清空所有 MIDI 事件
        // 发送空的 events 数组 + CANCEL 标志
        val emptyEvents = arrayOfNulls<BASSMIDI.BASS_MIDI_EVENT>(0)
        BASSMIDI.BASS_MIDI_StreamEvents(
            handle,
            BASSMIDI.BASS_MIDI_EVENTS_STRUCT or BASSMIDI.BASS_MIDI_EVENTS_CANCEL,
            emptyEvents,
            0
        )

//        // 3. 重置位置到 0
//        BASS.BASS_ChannelSetPosition(handle, 0, BASS.BASS_POS_BYTE)

        // 4. 关闭所有音符（防止残留）
        for (ch in 0..15) {
            BASSMIDI.BASS_MIDI_StreamEvent(handle, ch, BASSMIDI.MIDI_EVENT_NOTESOFF, 0)
        }
    }

    fun play() {
        if (handle == 0) return
        if (isPlaying()) return
        BASS.BASS_ChannelPlay(handle, false)
    }

    fun pause() {
        if (handle == 0) return
        if (!isPlaying()) return
        BASS.BASS_ChannelPause(handle)
    }

    fun isPlaying() = getChannelActive() == BASS.BASS_ACTIVE_PLAYING

    fun getChannelActive(): Int {
        if (handle == 0) return 0
        return BASS.BASS_ChannelIsActive(handle)
    }

    fun getCurrentTick(): Long {
        if (handle == 0) return 0L
        return BASS.BASS_ChannelGetPosition(handle, BASSMIDI.BASS_POS_MIDI_TICK)
    }

    fun getCurrentPosition(): Int {
        if (handle == 0) return 0
        val second = BASS.BASS_ChannelBytes2Seconds(
            handle,
            BASS.BASS_ChannelGetPosition(handle, BASS.BASS_POS_BYTE)
        )
        return (second * 1000).roundToInt().coerceAtLeast(0)
    }

    fun release() {
        if (handle != 0) {
            BASS.BASS_StreamFree(handle)
            handle = 0
        }
    }

    fun releaseFont() {
        if (font != 0) {
            BASSMIDI.BASS_MIDI_FontFree(font)
            font = 0
        }
    }

    fun destroy() {
        release()
        releaseFont()
    }
}