package com.lib.bass.effect

import com.un4seen.bass.BASS_FX

/**
 * desc:
 **
 * user: xujj
 * time: 2024/3/12 15:47
 **/
class GainEffect(playHandle: Int) :
    FxEffect<BASS_FX.BASS_BFX_DAMP>(BASS_FX.BASS_BFX_DAMP(), BASS_FX.BASS_FX_BFX_DAMP) {

    init {
        setEnable(true, playHandle)
        val param = getParam()
        param.fGain = 1f
        param.fTarget = 0.001f
        param.fQuiet = 0f
        param.fRate = 0f
        param.fDelay = 0f
        param.lChannel = BASS_FX.BASS_BFX_CHANALL
        setParam(param)
    }

    fun setGain(gain: Float) {
        val param = getParam()
        param.fGain = gain
        setParam(param)
    }
}