/*
	BASSMIDI test player
	Copyright (c) 2006-2024 Un4seen Developments Ltd.
*/
package com.yhy.commonlib

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.lib.bass.utils.BassConverter
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS.BASS_CHANNELINFO
import com.un4seen.bass.BASS.SYNCPROC
import com.un4seen.bass.BASSMIDI
import com.un4seen.bass.BASSMIDI.BASS_MIDI_FONT
import com.un4seen.bass.BASSMIDI.BASS_MIDI_FONTINFO
import com.un4seen.bass.BASSMIDI.BASS_MIDI_MARK
import com.yhy.commonlib.midi.InstrumentTrack
import com.yhy.commonlib.midi.MidiConstant
import com.yhy.commonlib.midi.MidiGenerator
import com.yhy.commonlib.piano.MidiPiano
import com.yhy.commonlib.utils.MusicalNotationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MidiTest : ComponentActivity() {
    var chan: Int = 0 // channel handle
    var font: Int = 0 // soundfont
    var speed: Float = 1f // tempo adjustment
    var updatefont: Int = 0
    var lyrics: String? = null // lyrics buffer

    private val midiPiano = MidiPiano()

    // display error messages
    fun Error(es: String?) {
        // get error code in current thread for display in UI thread
        val s = String.format("%s\n(error code: %d)", es, BASS.BASS_ErrorGetCode())
        runOnUiThread(Runnable {
            AlertDialog.Builder(this@MidiTest)
                .setMessage(s)
                .setPositiveButton("OK", null)
                .show()
        })
    }

    var LyricSync: SYNCPROC = SYNCPROC { handle: Int, channel: Int, data: Int, user: Any? ->
        val mark = BASS_MIDI_MARK()
        BASSMIDI.BASS_MIDI_StreamGetMark(
            channel,
            (user as Int?)!!,
            data,
            mark
        ) // get the lyric/text
        if (mark.text.get(0) == '@') return@SYNCPROC  // skip info

        if (mark.text.get(0) == '\\')  // clear display
            lyrics += "\n\n" + mark.text.substring(1)
        else if (mark.text.get(0) == '/')  // new line
            lyrics += "\n" + mark.text.substring(1)
        else lyrics += mark.text
        var lines: Int
        var nlpos = 0
        lines = 1
        while ((lyrics!!.indexOf('\n', nlpos).also { nlpos = it }) >= 0) {
            // count lines
            lines++
            nlpos++
        }
        if (lines > 3) { // remove old lines so that new lines fit in display...
            nlpos = 0
            do {
                nlpos = lyrics!!.indexOf('\n', nlpos) + 1
            } while (--lines > 3)
            lyrics = lyrics!!.substring(nlpos)
        }
        runOnUiThread(Runnable {
            (findViewById<View?>(R.id.lyrics) as TextView).setText(lyrics)
        })
    }

    var EndSync: SYNCPROC = SYNCPROC { handle: Int, channel: Int, data: Int, user: Any? ->
        lyrics = "" // clear lyrics
        runOnUiThread(Runnable {
            (findViewById<View?>(R.id.lyrics) as TextView).setText("")
        })
    }

    // look for a marker (eg. loop points)
    fun FindMarker(handle: Int, text: String?, mark: BASS_MIDI_MARK): Boolean {
        var a = 0
        while (BASSMIDI.BASS_MIDI_StreamGetMark(handle, BASSMIDI.BASS_MIDI_MARK_MARKER, a, mark)) {
            Log.d("xxx", "FindMarker: " + mark.text)
            if (mark.text == text) {
                Log.d("xxx", "FindMarker: $text true")
                return true
            } // found it

            a++
        }
        Log.d("xxx", "FindMarker: $text false " + BASS.BASS_ErrorGetCode())
        return false
    }

    var LoopSync: SYNCPROC = SYNCPROC { handle: Int, channel: Int, data: Int, user: Any? ->
        val mark = BASS_MIDI_MARK()
        if (FindMarker(channel, "loopstart", mark))  // found a loop start point
            BASS.BASS_ChannelSetPosition(
                channel,
                mark.pos.toLong(),
                BASS.BASS_POS_BYTE or BASSMIDI.BASS_MIDI_DECAYSEEK
            ) // rewind to it (and let old notes decay)
        else BASS.BASS_ChannelSetPosition(
            channel,
            0,
            BASS.BASS_POS_BYTE or BASSMIDI.BASS_MIDI_DECAYSEEK
        ) // else rewind to the beginning instead
    }

    fun OpenClicked(v: View?) {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.setType("audio/midi")
        i.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        try {
            startActivityForResult(i, 1)
        } catch (e: Exception) {
            Error(e.message)
        }
    }

    fun OpenFontClicked(v: View?) {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.setType("*/*")
        i.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        try {
            startActivityForResult(i, 2)
        } catch (e: Exception) {
            Error(e.message)
        }
    }

    fun ConvertClicked(v: View?) {
        val noteList = MusicalNotationUtils.parseNoteList(this, "nmn/unamattina2.json")
        val file = File(Environment.getExternalStorageDirectory().path + "/Music", "unamattina.mp3")
        file.parentFile?.mkdirs()
        Log.d("xxx", "ConvertClicked: " + font)
        lifecycleScope.launch {
            MidiGenerator.convertToMp3(
                listOf(
//                    InstrumentTrack("Lead 1 (square)", noteList),
                    InstrumentTrack("Acoustic Grand Piano", noteList),
                ),
                file,
                object : BassConverter.OnConvertCallback {
                    override fun onProgressUpdated(progress: Int) {

                    }

                    override suspend fun onSucceed(path: String) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MidiTest, path, Toast.LENGTH_SHORT).show()
                        }
                    }

                    override suspend fun onFailed() {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MidiTest, "失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
        playTrack(
            listOf(
//                InstrumentTrack("Lead 1 (square)", noteList),
//                InstrumentTrack("Acoustic Grand Piano", noteList),
                InstrumentTrack("Soprano Sax", noteList),
//                InstrumentTrack("Trumpet", noteList),
//                InstrumentTrack("Oboe", noteList),
//                InstrumentTrack("Acoustic Guitar (steel)", noteList),
//                InstrumentTrack("Harmonica", noteList),
//                InstrumentTrack("Electric Piano 1", noteList),
//                InstrumentTrack("Electric Piano 2", noteList),
//                InstrumentTrack("Acoustic Guitar (nylon)", noteList),
//                InstrumentTrack("Acoustic Guitar (steel)", noteList),
//                InstrumentTrack("Electric Guitar (jazz)", noteList),
//                InstrumentTrack("Electric Guitar (clean)", noteList),
            )
        )
    }

    fun EffectsClicked(v: View) {
        if ((v as CheckBox).isChecked()) BASS.BASS_ChannelFlags(
            chan,
            0,
            BASSMIDI.BASS_MIDI_NOFX
        ) // enable FX
        else BASS.BASS_ChannelFlags(
            chan,
            BASSMIDI.BASS_MIDI_NOFX,
            BASSMIDI.BASS_MIDI_NOFX
        ) // disable FX
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == RESULT_OK) {
            Log.d("xxx", "onActivityResult: " + resultData?.data)
            val fd: ParcelFileDescriptor?
            try {
                fd = contentResolver.openFileDescriptor(resultData?.data!!, "r")
            } catch (e: Exception) {
                Error(e.message)
                return
            }
            if (requestCode == 1) {
                BASS.BASS_StreamFree(chan) // free old stream before opening new
                (findViewById<View?>(R.id.lyrics) as TextView).setText("") // clear lyrics display
                if ((BASSMIDI.BASS_MIDI_StreamCreateFile(
                        fd,
                        0,
                        0,
                        BASS.BASS_SAMPLE_LOOP or (if ((findViewById<View?>(R.id.effects) as CheckBox).isChecked()) 0 else BASSMIDI.BASS_MIDI_NOFX),
                        1
                    ).also { chan = it }) == 0
                ) {
                    // it ain't a MIDI
                    (findViewById<View?>(R.id.open) as Button).setText("Open MIDI file...")
                    (findViewById<View?>(R.id.title) as TextView).setText("")
                    (findViewById<View?>(R.id.position) as SeekBar).setMax(0)
                    Error("Can't play the file")
                    return
                }
                updatePlayChan()
            } else {
                val newfont = BASSMIDI.BASS_MIDI_FontInit(fd, 0)
                if (newfont == 0) {
                    Error("Can't load the file")
                } else {
                    val sf = arrayOf<BASS_MIDI_FONT>(BASS_MIDI_FONT())
                    sf[0]!!.font = newfont
                    sf[0]!!.preset = -1 // use all presets
                    sf[0]!!.bank = 0 // use default bank(s)
                    BASSMIDI.BASS_MIDI_StreamSetFonts(0, sf, 1) // set default soundfont
                    if (chan != 0) BASSMIDI.BASS_MIDI_StreamSetFonts(
                        chan,
                        sf,
                        1
                    ) // set for current stream too

                    BASSMIDI.BASS_MIDI_FontFree(font) // free old soundfont
                    font = newfont
                }
                Log.d("xxx", "onActivityResult: font=$font chan=$chan")

//                lifecycleScope.launch(Dispatchers.IO) {
//                    val jsonString = FileUtils.parseFile(this@MidiTest, "midi.json")
//                    try {
//                        val tracks = ArrayList<InstrumentTrack>()
//                        val instruments = JSONObject(jsonString)
//                            .getJSONObject("data")
//                            .getJSONObject("midiData")
//                            .getJSONArray("instruments")
//                        for (i in 0 until instruments.length()) {
//                            val temp = instruments.optString(i)
//                            val track = try {
//                                Gson().fromJson(temp, InstrumentTrack::class.java)
//                            } catch (e: Exception) {
//                                null
//                            }
//                            if (track != null) {
//                                tracks.add(track)
//                            }
//                        }
//                        Log.d("xxx", "onActivityResult: $tracks")
//                        playTrack(tracks)
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
            }
        }
    }

    private fun updatePlayChan() {
        val info = BASS_CHANNELINFO()
        BASS.BASS_ChannelGetInfo(chan, info)
        (findViewById<View?>(R.id.open) as Button).setText(
            if (info.filename != null) File(
                info.filename
            ).getName() else "?"
        )
        // set the title (track name of first track)
        val mark = BASS_MIDI_MARK()
        if (BASSMIDI.BASS_MIDI_StreamGetMark(
                chan,
                BASSMIDI.BASS_MIDI_MARK_TRACK,
                0,
                mark
            ) && mark.track == 0
        ) (findViewById<View?>(R.id.title) as TextView).setText(mark.text)
        else (findViewById<View?>(R.id.title) as TextView).setText("")
        // update pos scroller range (using tick length)
        val length = BASS.BASS_ChannelGetLength(
            chan,
            BASSMIDI.BASS_POS_MIDI_TICK
        ).toInt() / 120
//        val totalBytes = BASS.BASS_ChannelGetLength(chan, BASS.BASS_POS_BYTE)
//        // 获取总时长（秒）
//        val totalSeconds = BASS.BASS_ChannelBytes2Seconds(chan, totalBytes)
//        Log.d("xxx", "onActivityResult: duration=$length $totalSeconds")
        (findViewById<View?>(R.id.position) as SeekBar).setMax(length)
        // set looping syncs
        if (FindMarker(chan, "loopend", mark))  // found a loop end point
            BASS.BASS_ChannelSetSync(
                chan,
                BASS.BASS_SYNC_POS or BASS.BASS_SYNC_MIXTIME,
                mark.pos.toLong(),
                LoopSync,
                0
            ) // set a sync there

        BASS.BASS_ChannelSetSync(
            chan,
            BASS.BASS_SYNC_END or BASS.BASS_SYNC_MIXTIME,
            0,
            LoopSync,
            0
        ) // set one at the end too (eg. in case of seeking past the loop point)
        // clear lyrics buffer and set lyrics syncs
        lyrics = ""
        if (BASSMIDI.BASS_MIDI_StreamGetMark(
                chan,
                BASSMIDI.BASS_MIDI_MARK_LYRIC,
                0,
                mark
            )
        )  // got lyrics
            BASS.BASS_ChannelSetSync(
                chan,
                BASSMIDI.BASS_SYNC_MIDI_MARK,
                BASSMIDI.BASS_MIDI_MARK_LYRIC.toLong(),
                LyricSync,
                BASSMIDI.BASS_MIDI_MARK_LYRIC
            )
        else if (BASSMIDI.BASS_MIDI_StreamGetMark(
                chan,
                BASSMIDI.BASS_MIDI_MARK_TEXT,
                20,
                mark
            )
        )  // got text instead (over 20 of them)
            BASS.BASS_ChannelSetSync(
                chan,
                BASSMIDI.BASS_SYNC_MIDI_MARK,
                BASSMIDI.BASS_MIDI_MARK_TEXT.toLong(),
                LyricSync,
                BASSMIDI.BASS_MIDI_MARK_TEXT
            )
        BASS.BASS_ChannelSetSync(chan, BASS.BASS_SYNC_END, 0, EndSync, 0)
        BASS.BASS_ChannelSetAttribute(
            chan,
            BASSMIDI.BASS_ATTRIB_MIDI_SPEED,
            speed
        ) // apply tempo adjustment
        // get default soundfont in case of matching soundfont being used
        val sf = arrayOf(BASS_MIDI_FONT())
        if (BASSMIDI.BASS_MIDI_StreamGetFonts(chan, sf, 1) > 0) font = sf[0]!!.font
        BASS.BASS_ChannelSetAttribute(
            chan,
            BASSMIDI.BASS_ATTRIB_MIDI_CPU,
            70f
        ) // limit CPU usage to 70%
        BASS.BASS_ChannelPlay(chan, false) // start playing
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        if (Build.VERSION.SDK_INT >= 23) requestPermissions(
            arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE),
            0
        )

        // initialize default output device
        if (!BASS.BASS_Init(-1, 44100, 0)) {
            Error("Can't initialize device")
            return
        }

        BASSMIDI.BASS_MIDI_GetVersion() // force BASSMIDI to load
        (findViewById<View?>(R.id.voices) as SeekBar).setProgress(BASS.BASS_GetConfig(BASSMIDI.BASS_CONFIG_MIDI_VOICES)) // get default voice limit

        // load optional plugins for packed soundfonts
        BASS.BASS_PluginLoad("libbassflac.so", 0)

        //		BASS.BASS_PluginLoad("libbasswv.so", 0);
//		BASS.BASS_PluginLoad("libbassopus.so", 0);
        (findViewById<View?>(R.id.position) as SeekBar).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    BASS.BASS_ChannelSetPosition(
                        chan,
                        (progress * 120).toLong(),
                        BASSMIDI.BASS_POS_MIDI_TICK
                    ) // set the position
                    // clear lyrics
                    lyrics = ""
                    (findViewById<View?>(R.id.lyrics) as TextView).setText("")
                }
            }
        })

        (findViewById<View?>(R.id.tempo) as SeekBar).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    speed = (10 + progress) / 20f // up to +/- 50% bpm
                    BASS.BASS_ChannelSetAttribute(
                        chan,
                        BASSMIDI.BASS_ATTRIB_MIDI_SPEED,
                        speed
                    ) // apply tempo adjustment
                }
            }
        })

        (findViewById<View?>(R.id.voices) as SeekBar).setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    BASS.BASS_SetConfig(
                        BASSMIDI.BASS_CONFIG_MIDI_VOICES,
                        progress
                    ) // set default voice limit
                    if (chan != 0) BASS.BASS_ChannelSetAttribute(
                        chan,
                        BASSMIDI.BASS_ATTRIB_MIDI_VOICES,
                        progress.toFloat()
                    ) // apply to current MIDI file too
                }
            }
        })

        // timer to update the display
        val timerHandler = Handler(mainLooper)
        val timer: Runnable = object : Runnable {
            override fun run() {
                val active = BASS.FloatValue()
                if (chan != 0) {
                    val tick = BASS.BASS_ChannelGetPosition(chan, BASSMIDI.BASS_POS_MIDI_TICK)
                        .toInt() // get position in ticks
                    val tempo = BASSMIDI.BASS_MIDI_StreamGetEvent(
                        chan,
                        0,
                        BASSMIDI.MIDI_EVENT_TEMPO
                    ) // get the file's tempo
                    (findViewById<View?>(R.id.position) as SeekBar).setProgress(tick / 120) // update position bar
                    (findViewById<View?>(R.id.bpm) as TextView).setText(
                        String.format(
                            "%.1f",
                            speed * 60000000.0 / tempo
                        )
                    ) // display bpm
                    BASS.BASS_ChannelGetAttribute(
                        chan,
                        BASSMIDI.BASS_ATTRIB_MIDI_VOICES_ACTIVE,
                        active
                    ) // get active voices
                }
                (findViewById<View?>(R.id.voicetext) as TextView).setText(
                    String.format(
                        "%d / %d",
                        active.value.toInt(),
                        BASS.BASS_GetConfig(BASSMIDI.BASS_CONFIG_MIDI_VOICES)
                    )
                ) // display voices
                (findViewById<View?>(R.id.cpu) as TextView).setText(
                    String.format(
                        "CPU: %d%%",
                        BASS.BASS_GetCPU().toInt()
                    )
                ) // display CPU usage
                if ((++updatefont and 1) == 1) { // only updating font info once a second
                    var text = "no soundfont"
                    val i = BASS_MIDI_FONTINFO()
                    if (BASSMIDI.BASS_MIDI_FontGetInfo(font, i)) text =
                        String.format("name: %s\nloaded: %d / %d\n", i.name, i.samload, i.samsize)
                    (findViewById<View?>(R.id.fontinfo) as TextView).setText(text)
                }
                timerHandler.postDelayed(this, 500)
            }
        }
        timerHandler.postDelayed(timer, 500)

//        lifecycleScope.launch(Dispatchers.IO) {
//            val midiList = getMidi(this@MidiTest)
//            playMidiNotes(midiList[0].notes)
//        }

        val file = File(application.cacheDir.path, "FluidR3_GM.sf2")
        val newfont = BASSMIDI.BASS_MIDI_FontInit(file.path, 0)
        if (newfont == 0) {
            Error("Can't load the file")
        } else {
            val sf = arrayOf<BASS_MIDI_FONT>(BASS_MIDI_FONT())
            sf[0]!!.font = newfont
            sf[0]!!.preset = -1 // use all presets
            sf[0]!!.bank = 0 // use default bank(s)
            BASSMIDI.BASS_MIDI_StreamSetFonts(0, sf, 1) // set default soundfont
            if (chan != 0) BASSMIDI.BASS_MIDI_StreamSetFonts(
                chan,
                sf,
                1
            ) // set for current stream too

            BASSMIDI.BASS_MIDI_FontFree(font) // free old soundfont
            font = newfont
        }

//        val noteList = MusicalNotationUtils.parseNoteList2(this, "unamattina2.json")
//        playTrack(
//            listOf(
////                InstrumentTrack("Lead 1 (square)", noteList),
//                InstrumentTrack("Acoustic Grand Piano", noteList),
////                InstrumentTrack("Soprano Sax", noteList),
////                InstrumentTrack("Trumpet", noteList),
////                InstrumentTrack("Oboe", noteList),
////                InstrumentTrack("Acoustic Guitar (steel)", noteList),
////                InstrumentTrack("Harmonica", noteList),
////                InstrumentTrack("Electric Piano 1", noteList),
////                InstrumentTrack("Electric Piano 2", noteList),
////                InstrumentTrack("Acoustic Guitar (nylon)", noteList),
////                InstrumentTrack("Acoustic Guitar (steel)", noteList),
////                InstrumentTrack("Electric Guitar (jazz)", noteList),
////                InstrumentTrack("Electric Guitar (clean)", noteList),
//            )
//        )

        midiPiano.create()

        (findViewById<View>(R.id.btn_key)).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    midiPiano.noteOn(50)
                }

                MotionEvent.ACTION_UP -> {
                    midiPiano.noteOff(50)
                }
            }
            return@setOnTouchListener v.onTouchEvent(event)
        }
    }

    fun playTrack(tracks: List<InstrumentTrack>) {
        BASS.BASS_StreamFree(chan) // free old stream before opening new
        // ppqn: Pulses Per Quarter Note，通常 96 或 480
        val ppqn = MidiConstant.PPQN
        // 2. 创建 MIDI 事件数组
        val events = createMidiEventsFromTracks(tracks, ppqn = ppqn, bpm = 120)

        // 3. 直接创建流！
        // flags: BASS_STREAM_AUTOFREE 可自动释放
        val streamHandle = BASSMIDI.BASS_MIDI_StreamCreateEvents(
            events,// 事件数组
            ppqn,// ppqn
            BASS.BASS_STREAM_AUTOFREE or BASSMIDI.BASS_MIDI_SINCINTER,  // flags，启用 sinc 插值混音，提升音质（会略微增加 CPU 占用）
            44100             // 采样率，0 = 使用设备默认
        )
        Log.d("xxx", "playMidi: play=$streamHandle" + " " + BASS.BASS_ErrorGetCode())

        // 4. 播放
        if (streamHandle != 0) {
//            BASS.BASS_ChannelPlay(streamHandle, false)
            chan = streamHandle
            updatePlayChan()
        }
    }

    fun createMidiEventsFromTracks(
        tracks: List<InstrumentTrack>,
        ppqn: Int = 480,
        bpm: Int = 120,
    ): Array<BASSMIDI.BASS_MIDI_EVENT> {
        val events = mutableListOf<BASSMIDI.BASS_MIDI_EVENT>()

        // 1. 设置 tempo（微秒/四分音符）
        val tempoEvent = BASSMIDI.BASS_MIDI_EVENT().apply {
            event = BASSMIDI.MIDI_EVENT_TEMPO
            param = 60000000 / bpm  // 转换为微秒/四分音符
            tick = 0
        }
        events.add(tempoEvent)

        // 2. 计算 tick 到秒的转换
        val secondsPerQuarter = 60.0 / bpm
        val tickPerSecond = ppqn / secondsPerQuarter

        // 3. 为每个轨道分配通道和设置 Program Change
        var currentChannel = 0

        for (track in tracks) {
            // 分配通道
            val channel = if (track.name.equals("Drums", ignoreCase = true)) {
                MidiConstant.DRUM_CHANNEL
            } else {
                val ch = currentChannel
                currentChannel++
                if (currentChannel == MidiConstant.DRUM_CHANNEL) currentChannel++ // 跳过鼓通道
                ch.coerceAtMost(15)
            }

            // 获取乐器程序号
            val program = MidiConstant.getProgramNumber(track.name)

            // 添加 Program Change 事件（设置乐器音色）
            if (channel != MidiConstant.DRUM_CHANNEL) {
                val programEvent = BASSMIDI.BASS_MIDI_EVENT().apply {
                    event = BASSMIDI.MIDI_EVENT_PROGRAM
                    param = program
                    this.chan = channel
                    tick = 0
                }
                events.add(programEvent)
            }

            // 可选：设置音量（Channel Volume）
//            val volumeEvent = BASSMIDI.BASS_MIDI_EVENT().apply {
//                event = BASSMIDI.MIDI_EVENT_VOLUME
//                param = 10000  // 范围 0-16383，10000 约 60% 音量
//                this.chan = channel
//                tick = 0
//            }
//            events.add(volumeEvent)

            // 添加该轨道的所有音符事件
            for (note in track.notes) {
                if (note.pitch in 0..127) {
                    val startTick = (note.start * tickPerSecond + 0.5).toInt()
                    val endTick = (note.end * tickPerSecond + 0.5).toInt()
                    val velocity = if (note.velocity > 0) note.velocity.coerceIn(1, 127) else 100

                    // Note On
                    val noteOn = BASSMIDI.BASS_MIDI_EVENT().apply {
                        event = BASSMIDI.MIDI_EVENT_NOTE
                        param = (note.pitch and 0xFF) or ((velocity shl 8) and 0xFF00)
                        this.chan = channel
                        tick = startTick
                    }
                    events.add(noteOn)

                    // Note Off
                    val noteOff = BASSMIDI.BASS_MIDI_EVENT().apply {
                        event = BASSMIDI.MIDI_EVENT_NOTE
                        param = (note.pitch and 0xFF) or (0 shl 8)  // velocity = 0 表示释放
                        this.chan = channel
                        tick = endTick
                    }
                    events.add(noteOff)
                }
            }
        }

        // 4. 添加 END 标记（必须！）
        val endEvent = BASSMIDI.BASS_MIDI_EVENT().apply {
            event = BASSMIDI.MIDI_EVENT_END
            tick = events.maxOfOrNull { it.tick } ?: 0
        }
        events.add(endEvent)

        // 按 tick 排序
        events.sortBy { it.tick }

        return events.toTypedArray()
    }

    public override fun onDestroy() {
        // free the output device and all plugins and the soundfont
        BASS.BASS_Free()
        BASS.BASS_PluginFree(0)
        BASSMIDI.BASS_MIDI_FontFree(font)

        super.onDestroy()
    }
}