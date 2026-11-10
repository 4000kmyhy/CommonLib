package com.yhy.commonlib.utils

import android.content.Context
import android.util.Log
import com.yhy.commonlib.midi.MidiNote
import org.json.JSONArray
import org.json.JSONObject

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/22 9:51
 **/
object MusicalNotationUtils {

    const val C4Position = 39 // 3 + 12 * 3

    /**
     * C,C#,D,D#,E,F,F#,G,G#,A,A#,B
     */
    fun pitchToPosition(pitch: Int, sharp: Boolean): Int {
        return when (pitch) {
            1 -> if (sharp) 1 else 0
            2 -> if (sharp) 3 else 2
            3 -> 4
            4 -> if (sharp) 6 else 5
            5 -> if (sharp) 8 else 7
            6 -> if (sharp) 10 else 9
            7 -> 11
            else -> 0
        }
    }

    fun keyOffset(key: String): Int {
        return when (key) {
            "1=A" -> -3
            "1=A#" -> -2
            "1=B" -> -1
            "1=C" -> 0
            "1=C#" -> 1
            "1=D" -> 2
            "1=D#" -> 3
            "1=E" -> 4
            "1=F" -> 5
            "1=F#" -> 6
            "1=G" -> 7
            "1=G#" -> 8
            else -> 0
        }
    }

    fun parseNoteList(context: Context, assetName: String): List<MidiNote> {
        val noteList = ArrayList<MidiNote>()
        val jsonString = FileUtils.parseFile(context, assetName)
        try {
            val jsonObject = JSONObject(jsonString)
            val key = jsonObject.optString("key")
            val keyOffset = keyOffset(key)
            val bpm = jsonObject.optInt("bpm", 120)
            val beatTime = 60.0 / bpm
            var startTime = 0.0
            var endTime = 0.0
            val sectionArray = jsonObject.getJSONArray("sections")
            for (i in 0 until sectionArray.length()) {
                val section = sectionArray.getJSONObject(i)

                val noteArray = section.optJSONArray("notes")
                if (noteArray != null) {
                    val pair = parseNoteArray(noteArray, startTime, beatTime, keyOffset)
                    noteList.addAll(pair.first)
                    endTime = pair.second
                }

                val chordArray = section.optJSONArray("chord")
                if (chordArray != null) {
                    val pair = parseNoteArray(chordArray, startTime, beatTime, keyOffset)
                    noteList.addAll(pair.first)
                    endTime = endTime.coerceAtMost(pair.second)//可能有滑音
                }

                startTime = endTime
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("xxx", "parseNoteList2: " + e)
        }
        return noteList
    }

    private fun parseNoteArray(
        noteArray: JSONArray,
        sectionTime: Double,
        beatTime: Double,
        keyOffset: Int
    ): Pair<List<MidiNote>, Double> {
        val noteList = ArrayList<MidiNote>()
        var startTime = sectionTime
        for (j in 0 until noteArray.length()) {
            val note = noteArray.getJSONObject(j)

            val duration = note.optDouble("duration", 1.0)

            val notePitch = getNotePitch(note, keyOffset)

            val endTime = startTime + beatTime * duration
            noteList.add(
                MidiNote(
                    pitch = notePitch,
                    start = startTime,
                    end = endTime,
                    velocity = 100
                )
            )

            val extraArray = note.optJSONArray("extra")
            if (extraArray != null) {
                for (k in 0 until extraArray.length()) {
                    val extra = extraArray.getJSONObject(k)
                    val notePitch = getNotePitch(extra, keyOffset)
                    noteList.add(
                        MidiNote(
                            pitch = notePitch,
                            start = startTime,
                            end = endTime,
                            velocity = 100
                        )
                    )
                }
            }

            startTime = endTime
        }
        return noteList to startTime
    }

    private fun getNotePitch(
        note: JSONObject,
        keyOffset: Int
    ): Int {
        val octave = note.optString("octave", "m")
        val pitch = note.optInt("pitch")
        val sharp = note.optBoolean("sharp")
        return if (pitch != 0) {
            21 + C4Position + keyOffset +
                    pitchToPosition(pitch, sharp) +
                    when (octave) {
                        "ll" -> -12 * 2
                        "l" -> -12
                        "h" -> 12
                        "hh" -> 12 * 2
                        else -> 0
                    }
        } else {
            -1
        }
    }
}