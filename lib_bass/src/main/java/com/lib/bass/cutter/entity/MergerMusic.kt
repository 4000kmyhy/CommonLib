package com.lib.bass.cutter.entity

import com.lib.bass.effect.GainEffect
import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX

/**
 * desc:
 **
 * user: xujj
 * time: 2024/4/1 15:47
 **/
class MergerMusic(
    val id: Long,
    val title: String,
    val path: String,
    var duration: Int
) {

    var startPosition = -1
    var endPosition = -1

    var startPositionInList = 0
    var endPositionInList = 0

    fun getActualDuration() = endPosition - startPosition

    var playHandle = 0
    private var endHandle = 0

    private var gainEffect: GainEffect? = null
    var gainValue = 1f
        private set

    fun setDataSource() {
        val playChan = BassUtils.streamCreateFile(path)
        playHandle = BASS_FX.BASS_FX_TempoCreate(
            playChan,
            BASS.BASS_SAMPLE_LOOP or BASS_FX.BASS_FX_FREESOURCE
        )
        if (playHandle != 0) {
            BASS.BASS_ChannelFlags(playHandle, 0, BASS.BASS_SAMPLE_LOOP)//不循环

            duration = BassUtils.getDuration(playHandle)
            if (startPosition < 0 || startPosition > duration) {
                startPosition = 0
            }
            if (endPosition < startPosition || endPosition > duration) {
                endPosition = duration
            }

            gainEffect = GainEffect(playHandle)
            gainEffect?.setGain(gainValue)
        }
    }

    fun isPlaying() = BassUtils.isPlaying(playHandle)

    fun getCurrentPosition() = BassUtils.getCurrentPosition(playHandle)

    fun play() {
        BassUtils.play(playHandle)
    }

    fun pause() {
        BassUtils.pause(playHandle)
    }

    fun stop() {
        BassUtils.stop(playHandle)
    }

    fun release() {
        BASS.BASS_ChannelRemoveSync(playHandle, endHandle)
        BassUtils.release(playHandle)
    }

    fun seekTo(position: Int) {
        BassUtils.seekTo(playHandle, position)
    }

    fun setSync(callback: () -> Unit) {
        BASS.BASS_ChannelRemoveSync(playHandle, endHandle)
        endHandle = BASS.BASS_ChannelSetSync(
            playHandle, BASS.BASS_SYNC_END, 0,
            { handle, channel, data, user ->
                callback.invoke()
            },
            0
        )
    }

    fun setGain(gain: Float) {
        gainValue = gain
        gainEffect?.setGain(gain)
    }

    override fun toString(): String {
        return "MergerMusic(id=$id, title='$title', path='$path', duration=$duration, startPosition=$startPosition, endPosition=$endPosition, playHandle=$playHandle, startPositionInList=$startPositionInList, endPositionInList=$endPositionInList)"
    }
}