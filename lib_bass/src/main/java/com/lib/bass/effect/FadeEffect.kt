package com.lib.bass.effect

import com.un4seen.bass.BASS

/**
 * desc:
 **
 * user: xujj
 * time: 2024/3/12 14:51
 **/
class FadeEffect(playHandle: Int) :
    FxEffect<BASS.BASS_FX_VOLUME_PARAM>(BASS.BASS_FX_VOLUME_PARAM(), BASS.BASS_FX_VOLUME) {

    var fadeInTime = 0
    var fadeOutTime = 0

    init {
        setEnable(true, playHandle)
    }

    fun fadeIn() {
        if (fadeInTime > 0) {
            val param = getParam()
            param.fTarget = 1f
            param.fCurrent = 0f
            param.fTime = fadeInTime / 1000f
            setParam(param)
        }
    }

    fun fadeOut() {
        if (fadeOutTime > 0) {
            val param = getParam()
            param.fTarget = 0f
            param.fCurrent = 1f
            param.fTime = fadeOutTime / 1000f
            setParam(param)
        }
    }

    fun reset() {
        val param = getParam()
        if (param.fCurrent != 1f) {
            param.fTarget = 1f
            param.fTime = 0f
            setParam(param)
        }
    }
}