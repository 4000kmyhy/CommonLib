package com.lib.bass.effect

import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import java.util.Arrays

/**
 * desc:
 **
 * user: xujj
 * time: 2023/11/10 16:48
 **/
class FxManager {

    private var playHandle = 0

    fun openFx(handle: Int) {
        this.playHandle = handle
    }

    /**
     * 十段eq
     */
    private var equalizer = FxEffect(BASS_FX.BASS_BFX_PEAKEQ(), BASS_FX.BASS_FX_BFX_PEAKEQ)
    private val eqFreq = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    /**
     * 三段eq
     */
    private val eqLow = FxEffect(BASS.BASS_DX8_PARAMEQ(), BASS.BASS_FX_DX8_PARAMEQ)
    private val eqMid = FxEffect(BASS.BASS_DX8_PARAMEQ(), BASS.BASS_FX_DX8_PARAMEQ)
    private val eqHigh = FxEffect(BASS.BASS_DX8_PARAMEQ(), BASS.BASS_FX_DX8_PARAMEQ)

    /**
     * 自动放大
     */
    private var damp = FxEffect(BASS_FX.BASS_BFX_DAMP(), BASS_FX.BASS_FX_BFX_DAMP)

    /**
     * 回音
     */
    private var echo = FxEffect(BASS_FX.BASS_BFX_ECHO4(), BASS_FX.BASS_FX_BFX_ECHO4)

    /**
     * 移相器
     */
    private var phaser = FxEffect(BASS_FX.BASS_BFX_PHASER(), BASS_FX.BASS_FX_BFX_PHASER)

    /**
     * 合唱/Flanger
     */
    private var chorus = FxEffect(BASS_FX.BASS_BFX_CHORUS(), BASS_FX.BASS_FX_BFX_CHORUS)

    /**
     * 回响
     */
    private var reverb = FxEffect(BASS.BASS_DX8_REVERB(), BASS.BASS_FX_DX8_REVERB)

    /**
     * 旋转
     */
    private var rotate = FxEffect(BASS_FX.BASS_BFX_ROTATE(), BASS_FX.BASS_FX_BFX_ROTATE)

    /**
     * 扭曲
     */
    private var distortion = FxEffect(BASS_FX.BASS_BFX_DISTORTION(), BASS_FX.BASS_FX_BFX_DISTORTION)

    /**
     * 自动哇音
     */
    private var autowah = FxEffect(BASS_FX.BASS_BFX_AUTOWAH(), BASS_FX.BASS_FX_BFX_AUTOWAH)

    /**
     * 过滤器
     */
//    private var filter = FxEffect(BASS_FX.BASS_BFX_APF(), BASS_FX.BASS_FX_BFX_APF)
    private var filter = FxEffect(BASS_FX.BASS_BFX_PEAKEQ(), BASS_FX.BASS_FX_BFX_PEAKEQ)

    fun setEqEnable(enable: Boolean, eqValue: IntArray) {
        equalizer.setEnable(enable, playHandle)
        if (enable) {
            val param = equalizer.getParam()
            param.fBandwidth = 1f
            param.lChannel = BASS_FX.BASS_BFX_CHANALL
            val eqGain = IntArray(10)
            Arrays.fill(eqGain, 0)
            System.arraycopy(
                eqValue, 0, eqGain, 0,
                Math.min(eqValue.size, eqGain.size)
            )
            for (band in eqGain.indices) {
                param.lBand = band
                param.fCenter = eqFreq[band]
                param.fGain = eqGain[band].toFloat()
                equalizer.setParam(param)
            }
        }
    }

    fun setEqBandLevel(@androidx.annotation.IntRange(from = 0, to = 9) band: Int, gain: Int) {
        var param = equalizer.getParam()
        //先设置band，再重新getParam
        param.lBand = band
        param = equalizer.getParam()
        param.fCenter = eqFreq[band]
        param.fGain = gain.toFloat()
        equalizer.setParam(param)
    }

    fun init3BandEq(lowGain: Float, midGain: Float, highGain: Float) {
        //low
        eqLow.setEnable(true, playHandle)
        eqLow.setParam(eqLow.getParam().also {
            it.fGain = lowGain
            it.fBandwidth = 18f
            it.fCenter = 125f
        })
        //mid
        eqMid.setEnable(true, playHandle)
        eqMid.setParam(eqMid.getParam().also {
            it.fGain = midGain
            it.fBandwidth = 18f
            it.fCenter = 1000f
        })
        //high
        eqHigh.setEnable(true, playHandle)
        eqHigh.setParam(eqHigh.getParam().also {
            it.fGain = highGain
            it.fBandwidth = 18f
            it.fCenter = 8000f
        })
    }

    fun set3EqGain(band: Int, fGain: Float) {
        when (band) {
            0 -> {
                eqLow.setParam(eqLow.getParam().also {
                    it.fGain = fGain
                })
            }

            1 -> {
                eqMid.setParam(eqMid.getParam().also {
                    it.fGain = fGain
                })
            }

            2 -> {
                eqHigh.setParam(eqHigh.getParam().also {
                    it.fGain = fGain
                })
            }
        }
    }

    fun initDamp(fGain: Float) {
        damp.setEnable(true, playHandle)
        damp.setParam(damp.getParam().also {
            it.fTarget = 0.001f
            it.fQuiet = 0f
            it.fRate = 0f
            it.fGain = fGain
            it.fDelay = 0f
        })
    }

    fun setDampGain(fGain: Float) {
        damp.setParam(damp.getParam().also {
            it.fGain = fGain
        })
    }

    fun setEchoEnable(enable: Boolean, value: FloatArray) {
        echo.setEnable(enable, playHandle)
        if (enable) {
            val param = echo.getParam()
            //[0.5,1.5]
            param.fDryMix = if (value[0] < 0f) {
                1f - 0.5f * value[0]
            } else {
                1f
            }
            //[0.5,1.5]
            param.fWetMix = if (value[0] > 0f) {
                1f + 0.5f * value[0]
            } else {
                1f
            }
            param.bStereo = true
            param.lChannel = BASS_FX.BASS_BFX_CHANALL
            param.fFeedback = 0f
            //[0.1,0.5]
            param.fDelay = 0.3f + 0.2f * value[1]
            echo.setParam(param)
        }
    }

    fun setEchoValue(value: FloatArray) {
        val param = echo.getParam()
        param.fDryMix = if (value[0] < 0f) {
            1f - 0.5f * value[0]
        } else {
            1f
        }
        param.fWetMix = if (value[0] > 0f) {
            1f + 0.5f * value[0]
        } else {
            1f
        }
        param.fDelay = 0.3f + 0.2f * value[1]
        echo.setParam(param)
    }

    fun setPhaserEnable(enable: Boolean, value: FloatArray) {
        phaser.setEnable(enable, playHandle)
        if (enable) {
            val param = phaser.getParam()
            param.fDryMix = if (value[0] < 0f) {
                1f - 0.5f * value[0]
            } else {
                1f
            }
            param.fWetMix = if (value[0] > 0f) {
                1f + 0.5f * value[0]
            } else {
                1f
            }
            param.fRate = 1.0f
            param.fRange = 4.0f
            param.fFeedback = 0f
            //[0,400]
            param.fFreq = 200f + 200f * value[1]
            param.lChannel = BASS_FX.BASS_BFX_CHANALL
            phaser.setParam(param)
        }
    }

    fun setPhaserValue(value: FloatArray) {
        val param = phaser.getParam()
        param.fDryMix = if (value[0] < 0f) {
            1f - 0.5f * value[0]
        } else {
            1f
        }
        param.fWetMix = if (value[0] > 0f) {
            1f + 0.5f * value[0]
        } else {
            1f
        }
        param.fFreq = 200f + 200f * value[1]
        phaser.setParam(param)
    }

    fun setFlangerEnable(enable: Boolean, value: FloatArray) {
        chorus.setEnable(enable, playHandle)
        if (enable) {
            val param = chorus.getParam()
            param.fDryMix = if (value[0] < 0f) {
                1f - 0.5f * value[0]
            } else {
                1f
            }
            param.fWetMix = if (value[0] > 0f) {
                1f + 0.5f * value[0]
            } else {
                1f
            }
            param.fFeedback = 0f
            param.fRate = 25f + 25f * value[1]
            param.lChannel = BASS_FX.BASS_BFX_CHANALL
            chorus.setParam(param)
        }
    }

    fun setFlangerValue(value: FloatArray) {
        val param = chorus.getParam()
        param.fDryMix = if (value[0] < 0f) {
            1f - 0.5f * value[0]
        } else {
            1f
        }
        param.fWetMix = if (value[0] > 0f) {
            1f + 0.5f * value[0]
        } else {
            1f
        }
        param.fRate = 25f + 25f * value[1]
        chorus.setParam(param)
    }

    fun setReverbEnable(enable: Boolean, value: FloatArray) {
        reverb.setEnable(enable, playHandle)
        if (enable) {
            setReverbValue(value)
        }
    }

    fun setReverbValue(value: FloatArray) {
        val param = reverb.getParam()
        //[10,20]
        val v = (15 + (value[0] + value[1]) * 2.5f).toInt()
        param.fReverbMix = (if (v != 0) Math.log(v / 20.0) * 20 else -96).toFloat()
        reverb.setParam(param)
    }

    fun setRotateEnable(enable: Boolean, value: FloatArray) {
        rotate.setEnable(enable, playHandle)
        if (enable) {
            val param = rotate.getParam()
            //[0,1]
            val v = 0.5f + (value[0] + value[1]) * 0.25f
            param.fRate = v
            param.lChannel = BASS_FX.BASS_BFX_CHANALL
            rotate.setParam(param)
        }
    }

    fun setRotateValue(value: FloatArray) {
        val param = rotate.getParam()
        //[0,1]
        val v = 0.5f + (value[0] + value[1]) * 0.25f
        param.fRate = v
        rotate.setParam(param)
    }

    fun setDistortionEnable(enable: Boolean, value: FloatArray) {
        distortion.setEnable(enable, playHandle)
        if (enable) {
            val param = distortion.getParam()
            param.fDryMix = 0.5f - 0.5f * value[0]
            param.fWetMix = 0.5f + 0.5f * value[0]
            param.fFeedback = 0f
            param.fVolume = 1f + 1f * value[1]
            param.lChannel = BASS_FX.BASS_BFX_CHANALL
            distortion.setParam(param)
        }
    }

    fun setDistortionValue(value: FloatArray) {
        val param = distortion.getParam()
        param.fDryMix = 0.5f - 0.5f * value[0]
        param.fWetMix = 0.5f + 0.5f * value[0]
        param.fVolume = 1f + 1f * value[1]
        distortion.setParam(param)
    }


    fun setAutoWahEnable(enable: Boolean, value: FloatArray) {
        autowah.setEnable(enable, playHandle)
        if (enable) {
            val param = autowah.getParam()
            param.fDryMix = if (value[0] < 0f) {
                1f - 0.5f * value[0]
            } else {
                1f
            }
            param.fWetMix = if (value[0] > 0f) {
                1f + 0.5f * value[0]
            } else {
                1f
            }
            param.fRate = 5.0f
            param.fRange = 4.0f
            param.fFeedback = 0f
            //[0,400]
            param.fFreq = 200f + 200f * value[1]
            param.lChannel = BASS_FX.BASS_BFX_CHANALL
            autowah.setParam(param)
        }
    }

    fun setAutoWahValue(value: FloatArray) {
        val param = autowah.getParam()
        param.fDryMix = if (value[0] < 0f) {
            1f - 0.5f * value[0]
        } else {
            1f
        }
        param.fWetMix = if (value[0] > 0f) {
            1f + 0.5f * value[0]
        } else {
            1f
        }
        param.fFreq = 200f + 200f * value[1]
        autowah.setParam(param)
    }

    fun setFilterEnable(enable: Boolean, value: FloatArray) {
        filter.setEnable(enable, playHandle)
        if (enable) {
            val param = filter.getParam()
//            param.fGain = 0.3f + 0.2f * value[0]
//            param.fDelay = 0.3f + 0.2f * value[1]
            param.lBand = 0
            param.fBandwidth = 2.5f
            param.fCenter = 10f
            param.fGain = 5f * (value[0] + value[1])
            param.lChannel = BASS_FX.BASS_BFX_CHANALL
            filter.setParam(param)
        }
    }

    fun setFilterValue(value: FloatArray) {
        val param = filter.getParam()
        param.fGain = 5f * (value[0] + value[1])
//        param.fGain = 0.3f + 0.2f * value[0]
//        param.fDelay = 0.3f + 0.2f * value[1]
        filter.setParam(param)
    }
}