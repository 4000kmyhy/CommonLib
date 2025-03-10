package com.lib.bass.cutter

import com.lib.bass.cutter.entity.MergerMusic
import com.lib.bass.cutter.viewModel.OnCutterSaveCallback
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
class MergerSaver(private val params: MergerParams) {

    suspend fun save(callback: OnCutterSaveCallback) {
        if (params.mergerList.isNullOrEmpty() || params.targetPath.isNullOrEmpty()) return

        withContext(Dispatchers.IO) {

            var handle = BASSmix.BASS_Mixer_StreamCreate(44100, 2, BASS.BASS_STREAM_DECODE)

//            var startPositionInList = 0
//            var endPositionInList = 0
//            for (i in params.mergerList!!.indices) {
//                val mergerMusic = params.mergerList!![i]
//                val actualDuration = mergerMusic.getActualDuration()
//                startPositionInList = if (i == 0) {
//                    0
//                } else {
//                    val adjustTime = Math.min(actualDuration, params.adjustTime)
//                    if (endPositionInList - adjustTime > 0) {
//                        endPositionInList - adjustTime
//                    } else {
//                        0
//                    }
//                }
//                endPositionInList = startPositionInList + actualDuration
//
//                val chan = BassUtils.getCutChannel(
//                    mergerMusic.path,
//                    mergerMusic.startPosition,
//                    mergerMusic.endPosition
//                )
//                BASSmix.BASS_Mixer_StreamAddChannelEx(
//                    handle, chan, BASSmix.BASS_MIXER_BUFFER,
//                    BASS.BASS_ChannelSeconds2Bytes(
//                        handle,
//                        startPositionInList / 1000.0
//                    ),
//                    BASS.BASS_ChannelSeconds2Bytes(handle, actualDuration / 1000.0)
//                )
//            }

            for (mergerMusic in params.mergerList!!) {
                val chan = BassUtils.getCutChannel(
                    mergerMusic.path,
                    mergerMusic.startPosition,
                    mergerMusic.endPosition
                )
                BASSmix.BASS_Mixer_StreamAddChannelEx(
                    handle, chan, BASSmix.BASS_MIXER_BUFFER,
                    BASS.BASS_ChannelSeconds2Bytes(
                        handle,
                        mergerMusic.startPositionInList / 1000.0
                    ),
                    BASS.BASS_ChannelSeconds2Bytes(handle, mergerMusic.getActualDuration() / 1000.0)
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

        private val params = MergerParams()

        fun setMergerList(mergerList: ArrayList<MergerMusic>): Builder {
            params.mergerList = mergerList
            return this
        }

        fun setTargetPath(path: String?): Builder {
            params.targetPath = path
            return this
        }

        fun setAdjustTime(adjustTime: Int): Builder {
            params.adjustTime = adjustTime
            return this
        }

        fun setFormat(format: String): Builder {
            params.format = format
            return this
        }

        fun create(): MergerSaver {
            return MergerSaver(params)
        }
    }

    class MergerParams {
        var mergerList: ArrayList<MergerMusic>? = null
        var targetPath: String? = null
        var adjustTime = 0
        var format = BassUtils.FORMAT_MP3
    }
}