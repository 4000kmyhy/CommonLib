package com.yhy.lib_piano

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/25 14:33
 **/
object PianoConstant {

    //9组，第0组2个（A0，B0），第8组1个（C8），其他组7个（Cn~Bn）
    const val WhiteKeyNum = 52

    const val C4Position = 39 // 3 + 12 * 3

    val KeyNotes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * A=0,A#=1,B=2,C=3,C#=4,D=5,D#=6,E=7,F=8,F#=9,G=10,G#=11
     * B、E和最后一个C8没有黑键
     */
    fun hasBlackKey(position: Int): Boolean {
        val positionInGroup = position % 7
        return positionInGroup != 1 && positionInGroup != 4 && position != WhiteKeyNum - 1
    }

    fun getCPosition(group: Int = 4): Int {
        return group * 7 - 5
    }

    /**
     * 白键的position转所有键的position
     * 例如C4是第24个白键（position=23），realPosition=39
     */
    fun getRealPosition(position: Int): Int {
        val positionInGroup = position % 7
        val group = position / 7
        val realPositionInGroup = when (positionInGroup) {
            0 -> 0
            1 -> 2
            2 -> 3
            3 -> 5
            4 -> 7
            5 -> 8
            6 -> 10
            else -> 0
        }
        return group * 12 + realPositionInGroup
    }

    fun getKeyNote(realPosition: Int, showGroup: Boolean = true): String {
        val group = (realPosition + 9) / 12
        val positionInGroup = (realPosition + 9) % 12
        val note = KeyNotes[positionInGroup]
        return if (showGroup) "$note$group" else note
    }

    fun isWhiteKey(realPosition: Int): Boolean {
        val positionInGroup = (realPosition + 9) % 12
        val note = KeyNotes[positionInGroup]
        return !note.contains("#")
    }

    fun keyPositionToPitch(position: Int) = position + 21
}