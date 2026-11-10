package com.yhy.commonlib.midi

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/3 11:27
 **/
object MidiConstant {

    // 默认分辨率（每四分音符的tick数）
    const val PPQN = 960

    const val BPM = 120

    // 鼓组特殊通道（通道9，索引从0开始）
    const val DRUM_CHANNEL = 9

    // 标准MIDI乐器映射（General MIDI）
    val INSTRUMENT_MAP = mapOf(
        "Acoustic Grand Piano" to 0,// 钢琴 (Piano)
        "Bright Acoustic Piano" to 1,
        "Electric Grand Piano" to 2,
        "Honky-tonk Piano" to 3,
        "Electric Piano 1" to 4,
        "Electric Piano 2" to 5,
        "Harpsichord" to 6,
        "Clavinet" to 7,
        "Celesta" to 8,// 半音阶打击乐器 (Chromatic Percussion)
        "Glockenspiel" to 9,
        "Music Box" to 10,
        "Vibraphone" to 11,
        "Marimba" to 12,
        "Xylophone" to 13,
        "Tubular Bells" to 14,
        "Dulcimer" to 15,
        "Drawbar Organ" to 16,// 风琴 (Organ)
        "Percussive Organ" to 17,
        "Rock Organ" to 18,
        "Church Organ" to 19,
        "Reed Organ" to 20,
        "Accordion" to 21,
        "Harmonica" to 22,
        "Tango Accordion" to 23,
        "Acoustic Guitar (nylon)" to 24,// 吉他 (Guitar)
        "Acoustic Guitar (steel)" to 25,
        "Electric Guitar (jazz)" to 26,
        "Electric Guitar (clean)" to 27,
        "Electric Guitar (muted)" to 28,
        "Overdriven Guitar" to 29,
        "Distortion Guitar" to 30,
        "Guitar Harmonics" to 31,
        "Acoustic Bass" to 32,// 贝斯 (Bass)
        "Electric Bass (finger)" to 33,
        "Electric Bass (pick)" to 34,
        "Fretless Bass" to 35,
        "Slap Bass 1" to 36,
        "Slap Bass 2" to 37,
        "Synth Bass 1" to 38,
        "Synth Bass 2" to 39,
        "Violin" to 40,// 独奏弦乐 (Solo Strings)
        "Viola" to 41,
        "Cello" to 42,
        "Contrabass" to 43,
        "Tremolo Strings" to 44,
        "Pizzicato Strings" to 45,
        "Orchestral Harp" to 46,
        "Timpani" to 47,
        "String Ensemble 1" to 48,// 弦乐合奏 (Ensemble)
        "String Ensemble 2" to 49,
        "SynthStrings 1" to 50,
        "SynthStrings 2" to 51,
        "Choir Aahs" to 52,
        "Voice Oohs" to 53,
        "Synth Voice" to 54,
        "Orchestra Hit" to 55,
        "Trumpet" to 56,// 铜管 (Brass)
        "Trombone" to 57,
        "Tuba" to 58,
        "Muted Trumpet" to 59,
        "French Horn" to 60,
        "Brass Section" to 61,
        "SynthBrass 1" to 62,
        "SynthBrass 2" to 63,
        "Soprano Sax" to 64,// 簧片乐器 (Reed)
        "Alto Sax" to 65,
        "Tenor Sax" to 66,
        "Baritone Sax" to 67,
        "Oboe" to 68,
        "English Horn" to 69,
        "Bassoon" to 70,
        "Clarinet" to 71,
        "Piccolo" to 72,// 管乐器 (Pipe)
        "Flute" to 73,
        "Recorder" to 74,
        "Pan Flute" to 75,
        "Blown Bottle" to 76,
        "Shakuhachi" to 77,
        "Whistle" to 78,
        "Ocarina" to 79,
        "Lead 1 (square)" to 80,// 合成主音 (Synth Lead)
        "Lead 2 (sawtooth)" to 81,
        "Lead 3 (calliope)" to 82,
        "Lead 4 (chiff)" to 83,
        "Lead 5 (charang)" to 84,
        "Lead 6 (voice)" to 85,
        "Lead 7 (fifths)" to 86,
        "Lead 8 (bass + lead)" to 87,
        "Pad 1 (new age)" to 88,// 合成背景音色 (Synth Pad)
        "Pad 2 (warm)" to 89,
        "Pad 3 (polysynth)" to 90,
        "Pad 4 (choir)" to 91,
        "Pad 5 (bowed)" to 92,
        "Pad 6 (metallic)" to 93,
        "Pad 7 (halo)" to 94,
        "Pad 8 (sweep)" to 95,
        "FX 1 (rain)" to 96,// 合成效果音色 (Synth Effects)
        "FX 2 (soundtrack)" to 97,
        "FX 3 (crystal)" to 98,
        "FX 4 (atmosphere)" to 99,
        "FX 5 (brightness)" to 100,
        "FX 6 (goblins)" to 101,
        "FX 7 (echoes)" to 102,
        "FX 8 (sci-fi)" to 103,
        "Sitar" to 104,// 民族乐器 (Ethnic)
        "Banjo" to 105,
        "Shamisen" to 106,
        "Koto" to 107,
        "Kalimba" to 108,
        "Bag pipe" to 109,
        "Fiddle" to 110,
        "Shanai" to 111,
        "Tinkle Bell" to 112,// 打击乐器 (Percussive)
        "Agogo" to 113,
        "Steel Drums" to 114,
        "Woodblock" to 115,
        "Taiko Drum" to 116,
        "Melodic Tom" to 117,
        "Synth Drum" to 118,
        "Reverse Cymbal" to 119,
        "Guitar Fret Noise" to 120,// 声音效果 (Sound Effects)
        "Breath Noise" to 121,
        "Seashore" to 122,
        "Bird Tweet" to 123,
        "Telephone Ring" to 124,
        "Helicopter" to 125,
        "Applause" to 126,
        "Gunshot" to 127
    )

    /**
     * 获取乐器对应的MIDI程序号
     */
    fun getProgramNumber(instrumentName: String): Int {
        // 首先尝试精确匹配
        INSTRUMENT_MAP[instrumentName]?.let { return it }

        // 尝试不区分大小写匹配
        INSTRUMENT_MAP.entries.find {
            it.key.equals(instrumentName, ignoreCase = true)
        }?.let { return it.value }

        // 尝试部分匹配
        for ((key, value) in INSTRUMENT_MAP) {
            if (key.contains(instrumentName, ignoreCase = true) ||
                instrumentName.contains(key, ignoreCase = true)
            ) {
                return value
            }
        }

        // 默认使用Acoustic Grand Piano
        return 0
    }

    fun programToCategory(program: Int): Int {
        return when (program) {
            in 0..7 -> 0//"Piano"
            in 8..15 -> 1//"Chromatic Percussion"
            in 16..23 -> 2//"Organ"
            in 24..31 -> 3//"Guitar"
            in 32..39 -> 4//"Bass"
            in 40..47 -> 5//"Solo Strings"
            in 48..55 -> 6//"Ensemble"
            in 56..63 -> 7//"Brass"
            in 64..71 -> 8// "Reed"
            in 72..79 -> 9//"Pipe"
            in 80..87 -> 10//"Synth Lead"
            in 88..95 -> 11//"Synth Pad"
            in 96..103 -> 12//"Synth Effects"
            in 104..111 -> 13// "Ethnic"
            in 112..119 -> 14// "Percussive"
            in 120..127 -> 15//"Sound Effects"
            else -> 0//"Piano"
        }
    }

    /**
     * 将秒转换为tick
     */
    fun secondsToTicks(seconds: Double, ppqn: Int, bpm: Int): Long {
        val ticksPerSecond = ppqn * (bpm / 60.0)
        return (seconds * ticksPerSecond).toLong()
    }
}