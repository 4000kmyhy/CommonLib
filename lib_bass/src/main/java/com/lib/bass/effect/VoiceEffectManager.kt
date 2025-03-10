package com.lib.bass.effect

import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS_FX

/**
 * desc:
 **
 * user: xujj
 * time: 2024/4/23 15:09
 **/
class VoiceEffectManager {

    private val chorus = FxEffect(BASS_FX.BASS_BFX_CHORUS(), BASS_FX.BASS_FX_BFX_CHORUS)
    private val echo = FxEffect(BASS_FX.BASS_BFX_ECHO4(), BASS_FX.BASS_FX_BFX_ECHO4)
    private val bqf = FxEffect(BASS_FX.BASS_BFX_BQF(), BASS_FX.BASS_FX_BFX_BQF)
    private val distortion = FxEffect(BASS_FX.BASS_BFX_DISTORTION(), BASS_FX.BASS_FX_BFX_DISTORTION)

    private var customTempo = 0f
    private var customPitch = 0f
    private var customFreq = 1f

    private fun resetEffect(handle: Int) {
        chorus.setEnable(false, handle)
        echo.setEnable(false, handle)
        bqf.setEnable(false, handle)
        distortion.setEnable(false, handle)
    }

    fun setEffectId(effectId: Int, handle: Int) {
        resetEffect(handle)
        var tempo = 0f
        var pitch = 0f
        var freq = 44100f
        when (effectId) {
            0 -> {//自定义
                tempo = customTempo
                pitch = customPitch
                freq = 44100f * customFreq
            }

            1 -> {//男声
                pitch = 10f * (0.65f - 1)
            }

            2 -> {//女声
                pitch = 10f * (1.4f - 1)
            }

            3 -> {//萝莉
                pitch = 10f * (1.8f - 1)
            }

            4 -> {//大叔
                pitch = 10f * (0.75f - 1)
            }

            5 -> {//怪兽
                pitch = 10f * (0.5f - 1)
            }

            6 -> {//机器人
                chorus.setEnable(true, handle)
                val param = chorus.getParam()
                param.fWetMix = 0.7f
                param.fRate = 1f
                chorus.setParam(param)
            }

            7 -> {//外星人
                chorus.setEnable(true, handle)
                val param = chorus.getParam()
                param.fWetMix = 0.75f
                param.fRate = 50f
                chorus.setParam(param)

                pitch = 10f * (1.4f - 1)
            }

            8 -> {//小黄人
                freq = 44100f * 1.7f
            }

            9 -> {//小黄蜂
                freq = 44100f * 2.1f
                pitch = 10f * (1.8f - 1)
            }

            10 -> {//花栗鼠
                freq = 44100f * 1.7f
                pitch = 10f * (1.2f - 1)
            }

            11 -> {//空灵
                echo.setEnable(true, handle)
                val param = echo.getParam()
                param.fDelay = 0.3f
                param.fFeedback = 0.2f
                echo.setParam(param)
            }

            12 -> {//混合
                echo.setEnable(true, handle)
                val param = echo.getParam()
                param.fDelay = 0.1f
                param.fFeedback = 0.5f
                echo.setParam(param)
            }

            13 -> {//山谷
                echo.setEnable(true, handle)
                val param = echo.getParam()
                param.fDelay = 1f
                param.fFeedback = 0.4f
                param.fDryMix = -0.1f
                param.fWetMix = -0.5f
                echo.setParam(param)
            }

            14 -> {//电话
                bqf.setEnable(true, handle)
                val param = bqf.getParam()
                param.lFilter = BASS_FX.BASS_BFX_BQF_HIGHPASS
                param.fQ = 0.3f
                param.fCenter = 1200f
                bqf.setParam(param)
            }

            15 -> {//收音机
                bqf.setEnable(true, handle)
                val param = bqf.getParam()
                param.lFilter = BASS_FX.BASS_BFX_BQF_LOWPASS
                param.fQ = 0.2f
                param.fCenter = 4000f
                bqf.setParam(param)
            }

            16 -> {//扩音器
                bqf.setEnable(true, handle)
                val param = bqf.getParam()
                param.lFilter = BASS_FX.BASS_BFX_BQF_HIGHPASS
                param.fQ = 0.5f
                param.fCenter = 800f
                bqf.setParam(param)

                //使声音失真
                distortion.setEnable(true, handle)
                val param2 = distortion.getParam()
                param2.fVolume = 1.2f
                distortion.setParam(param2)
            }

            17 -> {//魔鬼
                freq = 44100f * 0.9f
                pitch = 10f * (0.7f - 1)

                //使声音失真
                distortion.setEnable(true, handle)
                val param2 = distortion.getParam()
                param2.fVolume = 1.6f
                distortion.setParam(param2)
            }

            18 -> {//合唱
                chorus.setEnable(true, handle)
                val param = chorus.getParam()
                param.fWetMix = 0.4f
                param.fRate = 8f
                chorus.setParam(param)

                echo.setEnable(true, handle)
                val param2 = echo.getParam()
                param2.fDelay = 0.1f
                param2.fFeedback = 0.5f
                echo.setParam(param2)
            }

            19 -> {//旋转
                chorus.setEnable(true, handle)
                val param = chorus.getParam()
                param.fWetMix = 1f
                param.fRate = 12f
                chorus.setParam(param)
            }
        }
        BassUtils.setTempo(handle, tempo)
        BassUtils.setPitch(handle, pitch)
        BassUtils.setFreq(handle, freq)
    }

    fun setTempo(handle: Int, tempo: Float) {
        customTempo = tempo
        BassUtils.setTempo(handle, tempo)
    }

    fun setPitch(handle: Int, pitch: Float) {
        customPitch = pitch
        BassUtils.setPitch(handle, pitch)
    }

    fun setFreq(handle: Int, freq: Float) {
        customFreq = freq
        BassUtils.setFreq(handle, 44100f * freq)
    }

    fun setCustomValue(tempo: Float, pitch: Float, freq: Float) {
        customTempo = tempo
        customPitch = pitch
        customFreq = freq
    }

    fun reset() {
        setCustomValue(0f, 0f, 1f)
    }
}