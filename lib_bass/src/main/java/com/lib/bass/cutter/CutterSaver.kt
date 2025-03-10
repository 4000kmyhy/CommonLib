package com.lib.bass.cutter

import com.lib.bass.cutter.viewModel.OnCutterSaveCallback
import com.lib.bass.effect.FadeEffect
import com.lib.bass.effect.GainEffect
import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS
import com.un4seen.bass.BASS_FX
import com.un4seen.bass.BASSenc
import com.un4seen.bass.BASSmix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2024/3/12 16:21
 **/
class CutterSaver(private val params: CutterParams) {

    suspend fun save(callback: OnCutterSaveCallback) {
        if (params.sourcePath.isNullOrEmpty() || params.targetPath.isNullOrEmpty()) return

        withContext(Dispatchers.IO) {

            var handle = BASSmix.BASS_Mixer_StreamCreate(44100, 2, BASS.BASS_STREAM_DECODE)
            var totalLength = 0L //总长度

            if (params.isTrim) {//裁剪两边
                val chan = BassUtils.getCutChannel(
                    params.sourcePath!!,
                    params.startPosition,
                    params.endPosition
                )
                BASSmix.BASS_Mixer_StreamAddChannel(handle, chan, BASSmix.BASS_MIXER_BUFFER)

                totalLength = BASS.BASS_ChannelSeconds2Bytes(
                    handle,
                    (params.endPosition - params.startPosition) / 1000.0
                )
            } else {//裁剪中间
                val chan1 = BassUtils.getCutChannel(
                    params.sourcePath!!,
                    0,
                    params.startPosition
                )
                val chan2 = BassUtils.getCutChannel(
                    params.sourcePath!!,
                    params.endPosition,
                    params.duration
                )
                BASSmix.BASS_Mixer_StreamAddChannel(handle, chan1, BASSmix.BASS_MIXER_BUFFER)
                BASSmix.BASS_Mixer_StreamAddChannelEx(
                    handle, chan2, BASSmix.BASS_MIXER_BUFFER,
                    BASS.BASS_ChannelSeconds2Bytes(handle, params.startPosition / 1000.0),
                    0
                )

                totalLength = BASS.BASS_ChannelSeconds2Bytes(
                    handle,
                    (params.duration - (params.endPosition - params.startPosition)) / 1000.0
                )
            }

            handle = BASS_FX.BASS_FX_TempoCreate(
                handle,
                BASS.BASS_SAMPLE_FLOAT or BASS.BASS_STREAM_DECODE
            )

            //音量、速度、音调
            val gainEffect = GainEffect(handle)
            gainEffect.setGain(params.gain)
            BassUtils.setTempo(handle, params.tempo)
            if (params.masterTempo) {
                BassUtils.setPitch(handle, 0f)
            } else {
                BassUtils.setPitch(handle, params.tempo / 10f)
            }

            val fadeEffect = FadeEffect(handle)
            fadeEffect.fadeInTime = params.fadeInTime
            fadeEffect.fadeOutTime = params.fadeOutTime
            //淡入
            fadeEffect.fadeIn()
            //可淡出
            var doFadeOut = true

            val encoder = BassUtils.getEncodeChannel(handle, params.targetPath!!, params.format)
            if (encoder == 0) {
                BassUtils.logError("Encode")
                return@withContext
            }

            val speed = BassUtils.getSpeed(params.tempo)

            val fadeOutLength = BASS.BASS_ChannelSeconds2Bytes(handle, params.fadeOutTime / 1000.0)
            var sum = 0
            var length = 0
            var progress = 0
            val buffer = ByteBuffer.allocate(1024)
            while (BASS.BASS_ChannelGetData(
                    handle,
                    buffer,
                    buffer.limit()
                ).also { length = it } > 0
            ) {
                sum += (length * speed).toInt()
                var progress2 = (sum * 100f / totalLength).roundToInt()
                progress2 = Math.min(progress2, 100)

                if (progress != progress2) {
                    progress = progress2
                    withContext(Dispatchers.Main) {
                        callback.onProgressUpdated(progress)
                    }
                }

                //淡出
                if (sum >= totalLength - fadeOutLength * speed && doFadeOut) {
                    doFadeOut = false
                    fadeEffect.fadeOut()
                }
            }

            //释放、结束编码
            BASS.BASS_StreamFree(handle)
            BASSenc.BASS_Encode_Stop(encoder)

            withContext(Dispatchers.Main) {
                callback.onCompletion()
            }
        }
    }

    class Builder {

        private val params = CutterParams()

        fun setSourcePath(path: String?): Builder {
            params.sourcePath = path
            return this
        }

        fun setTargetPath(path: String?): Builder {
            params.targetPath = path
            return this
        }

        fun setTrim(isTrim: Boolean): Builder {
            params.isTrim = isTrim
            return this
        }

        fun setDuration(duration: Int, startPosition: Int, endPosition: Int): Builder {
            params.duration = duration
            params.startPosition = startPosition
            params.endPosition = endPosition
            return this
        }

        fun setFxValue(gain: Float, tempo: Float, masterTempo: Boolean): Builder {
            params.gain = gain
            params.tempo = tempo
            params.masterTempo = masterTempo
            return this
        }

        fun setFadeTime(fadeInTime: Int, fadeOutTime: Int): Builder {
            params.fadeInTime = fadeInTime
            params.fadeOutTime = fadeOutTime
            return this
        }

        fun setFormat(format: String): Builder {
            params.format = format
            return this
        }

        fun create(): CutterSaver {
            return CutterSaver(params)
        }
    }

    class CutterParams {
        var sourcePath: String? = null
        var targetPath: String? = null
        var isTrim = true
        var duration = 0
        var startPosition = 0
        var endPosition = 0
        var gain = 1f
        var tempo = 0f
        var masterTempo = true
        var fadeInTime = 0
        var fadeOutTime = 0
        var format = BassUtils.FORMAT_MP3
    }
}