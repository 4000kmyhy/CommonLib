package com.lib.game2048

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool

/**
 * desc:
 **
 * user: xujj
 * time: 2023/12/20 16:51
 **/
class SoundPoolPlayer(maxStreams: Int = 1) {

    private val mSoundPool: SoundPool
    private var soundId = 0
    private var streamId = 0

    init {
        val builder = SoundPool.Builder()
        //传入最多播放音频数量,
        builder.setMaxStreams(maxStreams)
        //AudioAttributes是一个封装音频各种属性的方法
        val attrBuilder = AudioAttributes.Builder()
        //设置音频流的合适的属性
        attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC)
        //加载一个AudioAttributes
        builder.setAudioAttributes(attrBuilder.build())
        mSoundPool = builder.build()
    }

    constructor(context: Context, rawId: Int) : this() {
        load(context, rawId)
    }

    fun load(context: Context, rawId: Int) {
        soundId = mSoundPool.load(context, rawId, 1)
    }

    fun play() {
        streamId = mSoundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun pause() {
        mSoundPool.pause(streamId)
    }

    fun stop() {
        mSoundPool.stop(streamId)
    }

    fun release() {
        mSoundPool.release()
    }

    fun replay() {
        stop()
        play()
    }
}