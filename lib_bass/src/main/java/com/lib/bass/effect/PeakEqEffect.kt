package com.lib.bass.effect

import androidx.annotation.IntRange
import com.un4seen.bass.BASS_FX

/**
 * desc:
 **
 * user: xujj
 * time: 2024/9/14 10:07
 **/
class PeakEqEffect : FxEffect<BASS_FX.BASS_BFX_PEAKEQ>(
    BASS_FX.BASS_BFX_PEAKEQ(),
    BASS_FX.BASS_FX_BFX_PEAKEQ
) {

    private var playHandle = 0
    private val eqFreq =
        floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    private var isEqEnable = false
    private val eqValue = IntArray(10)

    fun init(
        enable: Boolean,
        value: IntArray
    ) {
        isEqEnable = enable
        System.arraycopy(
            value, 0,
            eqValue, 0,
            Math.min(value.size, eqValue.size)
        )
    }

    fun updateHandle(handle: Int) {
        playHandle = handle
        setEqEnable(isEqEnable)
    }

    fun setEqEnable(enable: Boolean) {
        setEnable(enable, playHandle)
        if (enable) {
            val param = getParam()
            param.fBandwidth = 1f
            param.lChannel = BASS_FX.BASS_BFX_CHANALL
            for (band in 0 until 10) {
                param.lBand = band
                param.fCenter = eqFreq[band]
                param.fGain = eqValue[band].toFloat()
                setParam(param)
            }
        }
    }

    fun setEqValue(value: IntArray) {
        System.arraycopy(
            value, 0,
            eqValue, 0,
            Math.min(value.size, eqValue.size)
        )
        var param = getParam()
        for (band in 0 until 10) {
            param.lBand = band
            param = getParam()
//            param.fCenter = eqFreq[band]
            param.fGain = eqValue[band].toFloat()
            setParam(param)
        }
    }

    fun setEqBandLevel(@IntRange(from = 0, to = 9) band: Int, gain: Int) {
        eqValue[band] = gain
        var param = getParam()
        //先设置band，再重新getParam
        param.lBand = band
        param = getParam()
//        param.fCenter = eqFreq[band]
        param.fGain = gain.toFloat()
        setParam(param)
    }
}