package com.yhy.commonlib.piano

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yhy.commonlib.midi.MidiNote
import com.yhy.lib_piano.PianoConstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToLong

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/25 18:20
 **/
class PianoViewModel(application: Application) : AndroidViewModel(application) {

    var playPositions by mutableStateOf(listOf<Int>())
        private set
    var tipPositions by mutableStateOf(listOf<Int>())
        private set
    var freePlayMode by mutableStateOf(false)

    private var noteList = listOf<MidiNote>()
    private var keyList = listOf<List<Int>>()
    private var tipIndex = 0
    private var freePlayIndex = 0

    private val midiPiano = MidiPiano()
    private var autoPlayJob: Job? = null

    init {
        midiPiano.create()
        val file = File(application.cacheDir.path, "FluidR3_GM.sf2")
        midiPiano.setSoundfont(file.path)
    }

    fun updatePlayPositions(positions: List<Int>) {
        playPositions = positions
    }

    fun keyOn(position: Int) {
        Log.d("xxx", "keyOn: $position")
        if (freePlayMode && keyList.isNotEmpty()) {
            val positions = keyList.getOrElse(freePlayIndex, { listOf() })
            positions.forEach {
                val pitch = PianoConstant.keyPositionToPitch(it)
                midiPiano.noteOn(pitch)
                updateTip(it)
            }
        } else {
            val pitch = PianoConstant.keyPositionToPitch(position)
            midiPiano.noteOn(pitch)
            updateTip(position)
        }
    }

    fun keyOff(position: Int) {
        Log.d("xxx", "keyOff: $position")
        if (freePlayMode && keyList.isNotEmpty()) {
            val positions = keyList.getOrElse(freePlayIndex, { listOf() })
            positions.forEach {
                val pitch = PianoConstant.keyPositionToPitch(it)
                midiPiano.noteOff(pitch)
            }
            freePlayIndex = (freePlayIndex + 1) % keyList.size
        } else {
            val pitch = PianoConstant.keyPositionToPitch(position)
            midiPiano.noteOff(pitch)
        }
    }

    fun setNoteList(noteList: List<MidiNote>) {
        this.noteList = noteList
        val keyMaps = mutableMapOf<Double, List<Int>>()
        noteList.sortedBy { it.start }.forEach { note ->
            val position = note.pitch - 21
            if (position >= 0) {
                if (keyMaps.contains(note.start)) {
                    val list = keyMaps[note.start] ?: listOf()
                    if (!list.contains(position)) {
                        keyMaps[note.start] = list + position
                    }
                } else {
                    keyMaps[note.start] = listOf(position)
                }
            }
        }
        keyList = keyMaps.values.toList()

        //重置提示
        if (keyList.isNotEmpty()) {
            tipIndex = 0
            tipPositions = keyList[0]
        }

        //重置播放
        autoPlayJob?.cancel()
        midiPiano.resetStream()
        playPositions = listOf()

        freePlayIndex = 0
    }

    fun playNoteList() {
        if (noteList.isEmpty()) return
        autoPlayJob?.cancel()
        autoPlayJob = viewModelScope.launch(Dispatchers.Default) {
            midiPiano.playNoteList(noteList)

            playPositions = listOf()
            val mutableList = noteList.sortedBy { it.start }.toMutableList()
            val startPosition = midiPiano.getCurrentPosition()
            while (true) {
                if (mutableList.isEmpty()) {
                    playPositions = listOf()
                    break
                }

                mutableList.toList().forEach { note ->
                    val currentPosition = midiPiano.getCurrentPosition() - startPosition
                    if (currentPosition / 1000.0 in note.start..note.end) {
                        mutableList.remove(note)
                        async {
//                            midiPiano.noteOn(note.pitch)
                            val position = note.pitch - 21
                            if (position >= 0) {
                                playPositions = playPositions + position
                                val delayMillis =
                                    (note.end * 1000 - note.start * 1000).roundToLong()
                                delay(delayMillis)
//                            midiPiano.noteOff(note.pitch)
                                playPositions = if (mutableList.isNotEmpty()) {
                                    playPositions.filter { it != position }
                                } else {
                                    listOf()
                                }
                            }
                        }
                    }
                }

                delay(10)
            }
        }
    }

    private fun updateTip(position: Int) {
        Log.d("xxx", "updateTip: " + tipPositions)
        if (keyList.isEmpty()) return
        val newList = tipPositions - position
        if (newList.isNotEmpty()) {
            tipPositions = newList
        } else {
            tipIndex = (tipIndex + 1) % keyList.size
            tipPositions = keyList.getOrElse(tipIndex, { listOf() })
        }
    }

    fun pause() {
        Log.d("xxx", "pause: ")
        midiPiano.pause()
        autoPlayJob?.cancel()
        playPositions = listOf()
    }

    fun release() {
        Log.d("xxx", "release: ")
        midiPiano.release()
    }

    override fun onCleared() {
        super.onCleared()
        midiPiano.destroy()
    }
}