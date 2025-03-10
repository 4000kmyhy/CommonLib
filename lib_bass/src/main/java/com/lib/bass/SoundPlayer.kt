package com.lib.bass

import android.content.Context
import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS

class SoundPlayer() {

    constructor(path: String) : this() {
        setDataSource(path)
    }

    constructor(context: Context, fileName: String) : this() {
        setDataSource(context, fileName)
    }

    private var sample = 0
    private var channel = 0

    fun setDataSource(path: String) {
        release()
        sample = BASS.BASS_SampleLoad(path, 0, 0, 8, BASS.BASS_SAMPLE_OVER_POS)
    }

    fun setDataSource(context: Context, fileName: String) {
        release()
        sample = BASS.BASS_SampleLoad(
            BASS.Asset(context.assets, fileName),
            0,
            0,
            8,
            BASS.BASS_SAMPLE_OVER_POS
        )
    }

    fun play(volume: Float = 1f) {
        if (sample != 0) {
            channel = BASS.BASS_SampleGetChannel(sample, false)
            BassUtils.play(channel)
            setVolume(volume)
        }
    }

    fun pause() {
        BassUtils.pause(channel)
    }

    fun stop() {
        BassUtils.stop(channel)
    }

    fun replay(volume: Float = 1f) {
        stop()
        play(volume)
    }

    fun release() {
        stop()
        BassUtils.release(sample)
        sample = 0
    }

    fun setVolume(volume: Float) {
        BassUtils.setVolume(channel, volume)
    }
}