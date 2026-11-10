package com.yhy.commonlib.viewmodel

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.lib.bass.IMidiPlayerListener
import com.lib.bass.MidiPlayer
import com.lib.bass.utils.BassUtils
import com.yhy.commonlib.midi.InstrumentTrack
import com.yhy.commonlib.midi.InstrumentTrack.Companion.mergeDistinct
import com.yhy.commonlib.midi.MidiGenerator
import com.yhy.commonlib.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

fun jsonToTrackList(context: Context, name: String): List<InstrumentTrack> {
    val tracks = ArrayList<InstrumentTrack>()
    val jsonString = FileUtils.parseFile(context, "$name.json")
    try {
        val instruments = JSONObject(jsonString)
            .getJSONObject("data")
            .getJSONObject("midiData")
            .getJSONArray("instruments")
        for (i in 0 until instruments.length()) {
            val temp = instruments.optString(i)
            val track = try {
                Gson().fromJson(temp, InstrumentTrack::class.java)
            } catch (e: Exception) {
                null
            }
            if (track != null) {
                tracks.add(track)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return tracks
}


/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/7 10:16
 **/
class MidiViewModel(application: Application) : AndroidViewModel(application) {

    val trackPairsFlow = MutableStateFlow(listOf<Pair<InstrumentTrack, Boolean>>())
    val selectedListFlow = MutableStateFlow(listOf<InstrumentTrack>())

    private val midiPlayer = MidiPlayer()

    val currentPositionFlow = MutableStateFlow(0)
    val durationFlow = MutableStateFlow(0)
    val playStateFlow = MutableStateFlow(false)

    private var playJob: Job? = null

    init {
        BassUtils.initBass()

        midiPlayer.setOnMidiPlayerListener(object : IMidiPlayerListener {
            override fun onPlayStateChanged(
                player: MidiPlayer,
                state: Int,
                playStateChanged: Boolean,
                errorCode: Int
            ) {
                Log.d("xxx", "onPlayStateChanged: $state $playStateChanged")
                if (playStateChanged) {
                    playStateFlow.value = player.isPlaying()
                    updateProgress()
                }
            }
        })

        val file = File(application.cacheDir.path, "FluidR3_GM.sf2")
        midiPlayer.setSoundfont(file.path)

        loadJson()
    }

    fun loadJson() {
        viewModelScope.launch(Dispatchers.IO) {
            val tracks = (jsonToTrackList(getApplication(), "midi2") +
                    jsonToTrackList(getApplication(), "midi3") +
                    jsonToTrackList(getApplication(), "midi4") +
                    jsonToTrackList(getApplication(), "midi5") +
                    jsonToTrackList(getApplication(), "midi6") +
                    jsonToTrackList(getApplication(), "sky_vocal") +
                    jsonToTrackList(getApplication(), "sky_bgm"))
//                .mergeDistinct()
//            val tracks = jsonToTrackList(getApplication(), "test")
            trackPairsFlow.value = tracks.map { it to true }
            selectedListFlow.value = getSelectedList()
            setMidSource(selectedListFlow.value)
        }

//        val jsonString = FileUtils.parseFile(getApplication(), "output1.json")
//        try {
//            val tracks = ArrayList<InstrumentTrack>()
//            val instruments = JSONObject(jsonString)
//                .getJSONArray("tracks")
//            for (i in 0 until instruments.length()) {
//                val temp = instruments.getJSONObject(i)
//                val notes = temp.getJSONArray("notes")
//                for (j in 0 until notes.length()) {
//                    val tempObject = notes.optString(j)
//                    val midiNote = try {
//                        Gson().fromJson(tempObject, MidiNote::class.java)
//                    } catch (e: Exception) {
//                        null
//                    }
//                    if (midiNote != null) {
//                        val trackName =
//                            MidiGenerator.INSTRUMENT_MAP.entries.firstOrNull { it.value == midiNote.track }?.key
//                                ?: ""
//                        val index = tracks.indexOfFirst { it.name == trackName }
//                        if (index >= 0) {
//                            val track = tracks[index]
//                            tracks[index] = track.copy(notes = track.notes + midiNote)
//                        } else {
//                            val newTrack = InstrumentTrack(trackName, listOf(midiNote))
//                            tracks.add(newTrack)
//                        }
//                    }
//                }
//            }
//            trackListFlow.value = tracks.map { it to true }
//            setMidSource(tracks)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    fun getSelectedList(): List<InstrumentTrack> {
        return trackPairsFlow.value
            .filter { it.second }
            .map { it.first }
            .mergeDistinct()
    }

    fun setSelected(index: Int) {
        trackPairsFlow.update {
            if (index !in it.indices) return@update it
            it.mapIndexed { i, pair ->
                if (index == i) pair.copy(second = !pair.second) else pair
            }
        }
        selectedListFlow.value = getSelectedList()
        val position = currentPositionFlow.value
        setMidSource(selectedListFlow.value, position)
    }

    fun selectAll() {
        val trackList = trackPairsFlow.value
        val isNotAll = trackList.any { !it.second }
        trackPairsFlow.value = trackList.map {
            it.first to isNotAll
        }
        selectedListFlow.value = getSelectedList()
        val position = currentPositionFlow.value
        setMidSource(selectedListFlow.value, position)
    }

    fun setMidSource(tracks: List<InstrumentTrack>, position: Int = 0) {
        Log.d("xxx", "setMidSource: " + tracks.map { it.name })
        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.IO) {
            val handle = MidiGenerator.createMidiEvents(tracks)
            midiPlayer.setMidiSource(handle)
            Log.d("xxx", "setMidSource: " + midiPlayer.getBpm())
            if (position > 0) {
                seekTo(position)
            }
        }
    }

    fun playOrPause() {
        if (midiPlayer.isPlaying()) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        midiPlayer.play()
        //requestAudio
    }

    fun pause() {
        midiPlayer.pause()
    }

    private fun updateProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            do {
                currentPositionFlow.value = midiPlayer.getCurrentPosition()
                durationFlow.value = midiPlayer.getDuration()
                delay(10)
            } while (midiPlayer.isPlaying())
        }
    }

    fun seekTo(position: Int) {
        midiPlayer.seekTo(position)
    }

    fun convertToMidi() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(Environment.getExternalStorageDirectory(), "Download/test.mid")
            file.parentFile?.mkdirs()

            try {
                MidiGenerator.generateMidiFile(
                    selectedListFlow.value,
                    file
                )
                Toast.makeText(getApplication(), "转换成功：" + file.path, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(getApplication(), "转换失败：" + e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun playMidi() {
        val file = File(Environment.getExternalStorageDirectory(), "Download/test.mid")
        playJob?.cancel()
        playJob = viewModelScope.launch(Dispatchers.IO) {
            midiPlayer.setMidiSource(file.path)
            midiPlayer.play()
        }
    }
}