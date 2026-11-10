package com.yhy.commonlib.midi

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/3 11:31
 **/
class MidiData {
}


data class MidiNote(
    val pitch: Int,
    val start: Double,// 秒为单位
    val end: Double,// 秒为单位
    val velocity: Int,
//    val channel: Int = 0,
//    val track: Int = 0
)

data class InstrumentTrack(
    val name: String = "",
    val notes: List<MidiNote> = emptyList()
) {
    companion object {
        fun List<InstrumentTrack>.mergeDistinct(): List<InstrumentTrack> {
            val groupMap = mutableMapOf<String, MutableList<MidiNote>>()
            forEach { track ->
                val notesList = groupMap.getOrPut(track.name) { mutableListOf() }
                notesList.addAll(track.notes)
            }
            return groupMap.map {
                InstrumentTrack(
                    name = it.key,
                    notes = it.value.distinct()// 未处理不同力度情况
                )
            }
        }
    }

    override fun toString(): String {
        return "InstrumentTrack(name='$name', notes=${notes.size})"
    }
}