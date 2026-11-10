package com.yhy.commonlib.midi

import android.util.Log
import com.lib.bass.utils.BassConverter
import com.un4seen.bass.BASS
import com.un4seen.bass.BASSMIDI
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/3 9:46
 **/
object MidiGenerator {

    suspend fun convertToMp3(
        instruments: List<InstrumentTrack>,
        targetFile: File,
        callback: BassConverter.OnConvertCallback,
        ppqn: Int = MidiConstant.PPQN,
        bpm: Int = MidiConstant.BPM,
    ) {
        val handle = createMidiEvents(
            instruments,
            ppqn,
            bpm,
            BASS.BASS_STREAM_DECODE or BASSMIDI.BASS_MIDI_NOFX or BASSMIDI.BASS_MIDI_SINCINTER
        )
        BassConverter.convertChannel(handle, targetFile, callback)
    }

    fun createMidiEvents(
        instruments: List<InstrumentTrack>,
        ppqn: Int =  MidiConstant.PPQN,
        bpm: Int = MidiConstant. BPM,
        flags: Int = BASSMIDI.BASS_MIDI_NOFX or BASSMIDI.BASS_MIDI_SINCINTER
    ): Int {
        if (instruments.isEmpty()) return 0
        try {
            val events = ArrayList<BASSMIDI.BASS_MIDI_EVENT>()// 创建 MIDI 事件数组

            // 1. 设置 tempo（微秒/四分音符）
            events.add(BASSMIDI.BASS_MIDI_EVENT().apply {
                event = BASSMIDI.MIDI_EVENT_TEMPO
                param = 60000000 / bpm  // 转换为微秒/四分音符
                tick = 0
            })

            // 3. 为每个轨道分配通道和设置 Program Change
            var currentChannel = 0

            for (track in instruments) {
                // 分配通道
                val channel = if (track.name.equals("Drums", ignoreCase = true)) {
                    MidiConstant.   DRUM_CHANNEL
                } else {
                    val ch = currentChannel
                    currentChannel++
                    if (currentChannel ==  MidiConstant.DRUM_CHANNEL) currentChannel++ // 跳过鼓通道
                    ch.coerceAtMost(15)
                }

                // 添加 Program Change 事件（设置乐器音色）
                if (channel != MidiConstant. DRUM_CHANNEL) {
                    val program =  MidiConstant.getProgramNumber(track.name)
                    events.add(BASSMIDI.BASS_MIDI_EVENT().apply {
                        event = BASSMIDI.MIDI_EVENT_PROGRAM
                        param = program
                        this.chan = channel
                        tick = 0
                    })
                }

                Log.d(
                    "xxx", "createMidiEvents: " +
                            "Track=${track.name}, " +
                            "channel=$channel, " +
                            "notes=${track.notes.size}"
                )

                // 添加该轨道的所有音符事件
                for (note in track.notes) {
                    if (note.pitch in 0..127) {
                        val startTick =  MidiConstant.secondsToTicks(note.start, ppqn, bpm).toInt()
                        val endTick =  MidiConstant.secondsToTicks(note.end, ppqn, bpm).toInt()
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
            }

            // 4. 添加 END 标记（必须！）
            events.add(BASSMIDI.BASS_MIDI_EVENT().apply {
                event = BASSMIDI.MIDI_EVENT_END
                tick = events.maxOfOrNull { it.tick } ?: 0
            })

            events.sortBy { it.tick }

            return BASSMIDI.BASS_MIDI_StreamCreateEvents(
                events.toTypedArray(),
                ppqn,
                flags,
                44100
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun generateMidiFile(
        tracks: List<InstrumentTrack>,
        outputFile: File,
        ticksPerQuarterNote: Int = MidiConstant.PPQN,
        bpm: Int = MidiConstant.BPM
    ) {
        val midiData = buildMidiFile(tracks, ticksPerQuarterNote, bpm)
        FileOutputStream(outputFile).use { fos ->
            fos.write(midiData)
        }
    }

    /**
     * 构建MIDI文件数据
     */
    private fun buildMidiFile(
        tracks: List<InstrumentTrack>,
        ticksPerQuarterNote: Int,
        bpm: Int
    ): ByteArray {
        // 收集所有音轨的MIDI事件
        val trackEventsList = mutableListOf<List<Pair<Long, ByteArray>>>()

        // 创建全局速度轨（Track 0）
        val tempoEvent = buildTempoEvent(bpm)
        trackEventsList.add(listOf(0L to tempoEvent))

        var currentChannel = 0

        // 为每个InstrumentTrack创建音轨事件
        tracks.forEach { instrumentTrack ->
            val events = mutableListOf<Pair<Long, ByteArray>>()

            // 分配通道
            val channel = if (instrumentTrack.name.equals("Drums", ignoreCase = true)) {
                MidiConstant.DRUM_CHANNEL
            } else {
                val ch = currentChannel
                currentChannel++
                if (currentChannel == MidiConstant.DRUM_CHANNEL) currentChannel++ // 跳过鼓通道
                ch.coerceAtMost(15)// MIDI有16个通道（0-15）
            }

            if (channel != MidiConstant.DRUM_CHANNEL) {
                // 添加Program Change（设置乐器）
                val program = MidiConstant.getProgramNumber(instrumentTrack.name)
                val programChangeEvent = buildProgramChangeEvent(channel, program) // 0 = 钢琴
                events.add(0L to programChangeEvent)
            }

            Log.d(
                "xxx", "buildMidiFile: " +
                        "Track=${instrumentTrack.name}, " +
                        "channel=$channel, " +
                        "notes=${instrumentTrack.notes.size}"
            )

            // 添加音量控制 (CC7)
            val volumeEvent = buildControlChangeEvent(channel, 7, 100)
            events.add(0L to volumeEvent)

            // 添加音符事件
            instrumentTrack.notes.forEach { note ->
                val startTick = MidiConstant.secondsToTicks(note.start, ticksPerQuarterNote, bpm)
                val endTick = MidiConstant.secondsToTicks(note.end, ticksPerQuarterNote, bpm)
                val pitch = note.pitch.coerceIn(0, 127)
                val velocity = if (note.velocity > 0) note.velocity.coerceIn(1, 127) else 100

                // Note On
                val noteOn = buildNoteEvent(true, channel, pitch, velocity)
                events.add(startTick to noteOn)

                // Note Off
                val noteOff = buildNoteEvent(false, channel, pitch, 0)
                events.add(endTick to noteOff)
            }

            // 按时间排序
            events.sortBy { it.first }
            trackEventsList.add(events)
        }

        // 构建完整的MIDI文件
        return buildCompleteMidiFile(trackEventsList, ticksPerQuarterNote)
    }

    /**
     * 构建速度事件
     */
    private fun buildTempoEvent(bpm: Int): ByteArray {
        val usPerQuarterNote = 60_000_000 / bpm
        return byteArrayOf(
            0xFF.toByte(),           // Meta事件
            0x51.toByte(),           // Tempo
            0x03.toByte(),           // 数据长度
            ((usPerQuarterNote shr 16) and 0xFF).toByte(),
            ((usPerQuarterNote shr 8) and 0xFF).toByte(),
            (usPerQuarterNote and 0xFF).toByte()
        )
    }

    /**
     * 构建Program Change事件
     */
    private fun buildProgramChangeEvent(channel: Int, program: Int): ByteArray {
        require(channel in 0..15) { "Channel must be 0-15" }
        require(program in 0..127) { "Program must be 0-127" }
        return byteArrayOf(
            (0xC0 or channel).toByte(),  // Program Change命令
            program.toByte()
        )
    }

    /**
     * 构建Control Change事件
     */
    private fun buildControlChangeEvent(channel: Int, controller: Int, value: Int): ByteArray {
        require(channel in 0..15) { "Channel must be 0-15" }
        require(controller in 0..127) { "Controller must be 0-127" }
        require(value in 0..127) { "Value must be 0-127" }
        return byteArrayOf(
            (0xB0 or channel).toByte(),  // Control Change命令
            controller.toByte(),
            value.toByte()
        )
    }

    /**
     * 构建Note On/Off事件
     */
    private fun buildNoteEvent(isOn: Boolean, channel: Int, pitch: Int, velocity: Int): ByteArray {
        require(channel in 0..15) { "Channel must be 0-15" }
        require(pitch in 0..127) { "Pitch must be 0-127" }
        require(velocity in 0..127) { "Velocity must be 0-127" }

        val command = if (isOn) 0x90 else 0x80
        return byteArrayOf(
            (command or channel).toByte(),
            pitch.toByte(),
            velocity.toByte()
        )
    }

    /**
     * 构建完整的MIDI文件
     */
    private fun buildCompleteMidiFile(
        tracks: List<List<Pair<Long, ByteArray>>>,
        ticksPerQuarterNote: Int
    ): ByteArray {
        val output = ByteArrayOutputStream()

        // 1. 写入文件头 MThd
        writeMThd(output, tracks.size, ticksPerQuarterNote)

        // 2. 写入每个音轨 MTrk
        tracks.forEach { events ->
            writeMTrk(output, events)
        }

        return output.toByteArray()
    }

    /**
     * 写入MThd块（MIDI文件头）
     */
    private fun writeMThd(
        output: ByteArrayOutputStream,
        trackCount: Int,
        ticksPerQuarterNote: Int
    ) {
        // "MThd" 标识
        output.write("MThd".toByteArray())

        // 头部长度（6字节）
        writeInt32(output, 6)

        // 格式类型（1 = 多轨同步）
        writeInt16(output, 1)

        // 音轨数量
        writeInt16(output, trackCount)

        // 每四分音符tick数
        writeInt16(output, ticksPerQuarterNote)
    }

    /**
     * 写入MTrk块（音轨数据）
     */
    private fun writeMTrk(output: ByteArrayOutputStream, events: List<Pair<Long, ByteArray>>) {
        // "MTrk" 标识
        output.write("MTrk".toByteArray())

        // 临时存储轨道数据
        val trackData = ByteArrayOutputStream()
        var lastTick: Long = 0

        // 写入所有事件
        events.forEach { (tick, eventData) ->
            // 写入可变长度时间差
            writeVariableLength(trackData, tick - lastTick)
            // 写入事件数据
            trackData.write(eventData)
            lastTick = tick
        }

        // 添加End of Track事件
        writeVariableLength(trackData, 0)
        trackData.write(byteArrayOf(0xFF.toByte(), 0x2F.toByte(), 0x00.toByte()))

        // 写入数据长度
        val data = trackData.toByteArray()
        writeInt32(output, data.size)
        output.write(data)
    }

    /**
     * 写入可变长度值（用于MIDI时间差）
     */
    private fun writeVariableLength(output: ByteArrayOutputStream, value: Long) {
        var v = value
        val buffer = ByteArrayOutputStream()

        // 将值编码为7位一组的字节
        buffer.write((v and 0x7F).toInt())
        v = v shr 7

        while (v > 0) {
            buffer.write(((v and 0x7F) or 0x80).toInt())
            v = v shr 7
        }

        // 反转字节顺序（MIDI规范要求大端序）
        val bytes = buffer.toByteArray()
        for (i in bytes.indices.reversed()) {
            output.write(bytes[i].toInt())
        }
    }

    /**
     * 写入32位整数（大端序）
     */
    private fun writeInt32(output: ByteArrayOutputStream, value: Int) {
        output.write((value shr 24) and 0xFF)
        output.write((value shr 16) and 0xFF)
        output.write((value shr 8) and 0xFF)
        output.write(value and 0xFF)
    }

    /**
     * 写入16位整数（大端序）
     */
    private fun writeInt16(output: ByteArrayOutputStream, value: Int) {
        output.write((value shr 8) and 0xFF)
        output.write(value and 0xFF)
    }
}