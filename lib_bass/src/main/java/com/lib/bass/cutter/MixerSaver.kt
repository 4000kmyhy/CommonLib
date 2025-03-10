package com.lib.bass.cutter

import com.lib.bass.cutter.entity.MergerMusic
import com.lib.bass.cutter.viewModel.OnCutterSaveCallback
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
 * time: 2024/4/8 11:16
 **/
class MixerSaver(private val params: MixerParams) {

    suspend fun save(callback: OnCutterSaveCallback) {
        if (params.mergerMusic1 == null || params.mergerMusic2 == null || params.targetPath.isNullOrEmpty()) return

        withContext(Dispatchers.IO) {

            var handle = BASSmix.BASS_Mixer_StreamCreate(44100, 2, BASS.BASS_STREAM_DECODE)

            params.mergerMusic1?.let {
                var chan = BassUtils.getCutChannel(
                    it.path,
                    it.startPosition,
                    it.endPosition
                )
                chan = BASS_FX.BASS_FX_TempoCreate(
                    chan,
                    BASS.BASS_SAMPLE_FLOAT or BASS.BASS_STREAM_DECODE
                )
                val gainEffect = GainEffect(chan)
                gainEffect.setGain(it.gainValue)

                BASSmix.BASS_Mixer_StreamAddChannelEx(
                    handle, chan, BASSmix.BASS_MIXER_BUFFER,
                    BASS.BASS_ChannelSeconds2Bytes(
                        handle,
                        it.startPositionInList / 1000.0
                    ),
                    BASS.BASS_ChannelSeconds2Bytes(handle, it.getActualDuration() / 1000.0)
                )
            }
            params.mergerMusic2?.let {
                var chan = BassUtils.getCutChannel(
                    it.path,
                    it.startPosition,
                    it.endPosition
                )
                chan = BASS_FX.BASS_FX_TempoCreate(
                    chan,
                    BASS.BASS_SAMPLE_FLOAT or BASS.BASS_STREAM_DECODE
                )
                val gainEffect = GainEffect(chan)
                gainEffect.setGain(it.gainValue)

                BASSmix.BASS_Mixer_StreamAddChannelEx(
                    handle, chan, BASSmix.BASS_MIXER_BUFFER,
                    BASS.BASS_ChannelSeconds2Bytes(
                        handle,
                        it.startPositionInList / 1000.0
                    ),
                    BASS.BASS_ChannelSeconds2Bytes(handle, it.getActualDuration() / 1000.0)
                )
            }

            handle = BASS_FX.BASS_FX_TempoCreate(
                handle,
                BASS.BASS_SAMPLE_FLOAT or BASS.BASS_STREAM_DECODE
            )

            val encoder = BassUtils.getEncodeChannel(handle, params.targetPath!!, params.format)
            if (encoder == 0) {
                BassUtils.logError("Encode")
                return@withContext
            }

            val totalLength = BASS.BASS_ChannelGetLength(handle, BASS.BASS_POS_BYTE)
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
                sum += length
                var progress2 = (sum * 1.0f / totalLength * 100).roundToInt()
                progress2 = Math.min(progress2, 100)

                if (progress != progress2) {
                    progress = progress2
                    withContext(Dispatchers.Main) {
                        callback.onProgressUpdated(progress)
                    }
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

        private val params = MixerParams()

        fun setMergerMusics(mergerMusic1: MergerMusic?, mergerMusic2: MergerMusic?): Builder {
            params.mergerMusic1 = mergerMusic1
            params.mergerMusic2 = mergerMusic2
            return this
        }

        fun setTargetPath(path: String?): Builder {
            params.targetPath = path
            return this
        }

        fun setFormat(format: String): Builder {
            params.format = format
            return this
        }

        fun create(): MixerSaver {
            return MixerSaver(params)
        }
    }

    class MixerParams {
        var mergerMusic1: MergerMusic? = null
        var mergerMusic2: MergerMusic? = null
        var targetPath: String? = null
        var format = BassUtils.FORMAT_MP3
    }
}