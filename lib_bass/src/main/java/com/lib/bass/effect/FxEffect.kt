package com.lib.bass.effect

import com.un4seen.bass.BASS

/**
 * desc:
 **
 * user: xujj
 * time: 2024/3/12 14:46
 **/
open class FxEffect<T>(private val param: T, private val type: Int) {

    private var fxHandle = 0
    private var playHandle = 0

    fun setEnable(enable: Boolean, handle: Int) {
        if (enable) {
            if (fxHandle == 0 || playHandle != handle) {
                playHandle = handle
                fxHandle = BASS.BASS_ChannelSetFX(playHandle, type, 0)
            }
        } else {
            if (fxHandle != 0 && playHandle != 0) {
                BASS.BASS_ChannelRemoveFX(playHandle, fxHandle)
                fxHandle = 0
                playHandle = 0
            }
        }
    }

    fun isEnable() = fxHandle != 0

    fun getParam(): T {
        BASS.BASS_FXGetParameters(fxHandle, param)
        return param
    }

    fun setParam(param: T) {
        BASS.BASS_FXSetParameters(fxHandle, param)
    }
}