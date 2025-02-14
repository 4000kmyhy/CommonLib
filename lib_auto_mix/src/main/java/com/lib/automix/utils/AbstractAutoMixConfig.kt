package com.lib.automix.utils

import android.app.Service
import android.content.Context
import android.content.Intent
import android.widget.FrameLayout
import com.lib.media.entity.Music

/**
 * desc:
 **
 * user: xujj
 * time: 2024/3/28 17:37
 **/
abstract class AbstractAutoMixConfig {

    var isAutoMix = false
    var onAutoMixListener: OnAutoMixListener? = null
    var isAutoSync = true
    var currentDeck = true

    fun enterAutoMix(listener: OnAutoMixListener) {
        isAutoMix = true
        onAutoMixListener = listener
        enterAutoMix()
    }

    fun exitAutoMix(listener: OnAutoMixListener) {
        if (onAutoMixListener == listener) {//退出的是当前的
            isAutoMix = false
            onAutoMixListener = null
            exitAutoMix()
        }
    }

    abstract fun enterAutoMix()

    abstract fun exitAutoMix()

    abstract fun getCurrentMusic(isDeckA: Boolean): Music?

    abstract fun playOrPause(isDeckA: Boolean, isTransition: Boolean, fadeTime: Long)

    abstract fun play(isDeckA: Boolean)

    abstract fun pause(isDeckA: Boolean)

    abstract fun startScratch(isDeckA: Boolean, progress: Int, timeDiff: Int)

    abstract fun pauseScratch(isDeckA: Boolean)

    abstract fun startTransition(isDeckA: Boolean, music: Music, fadeTime: Long)

    abstract fun isPlaying(isDeckA: Boolean): Boolean

    abstract fun getCurrentPosition(isDeckA: Boolean): Int

    abstract fun getDuration(isDeckA: Boolean): Int

    abstract fun getVolumeProgress(): Int

    abstract fun getRateRatio(isDeckA: Boolean): Float

    abstract fun getLibraryIntent(context: Context): Intent

    abstract fun getServiceClass(): Class<out Service?>?

    open fun updateCurrentDeck(isDeckA: Boolean) {
        currentDeck = isDeckA
    }

    abstract fun createAdapterBanner(context: Context, frameLayout: FrameLayout)
}