package com.yhy.commonlib.main

import android.media.AudioFormat
import android.media.AudioRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lib.bass.utils.BassUtils
import com.un4seen.bass.BASS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/7 14:54
 **/
class WaveformViewModel : ViewModel() {

    companion object {
        private const val sampleRateInHz = 44100
        private const val channelConfig = AudioFormat.CHANNEL_IN_MONO
        private const val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        fun getMinBufferSize(): Int {
            return AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
        }
    }

    val waveformFlow = MutableStateFlow(Pair(0f, listOf<IntArray>()))

    init {
        BassUtils.initBass()
    }

    fun loadWaveform(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            waveformFlow.value = Pair(0f, listOf())
            val waveformData = ArrayList<IntArray>()

            val handle = BASS.BASS_StreamCreateFile(
                data, 0, 0,
                BASS.BASS_STREAM_DECODE or BASS.BASS_STREAM_PRESCAN
            )
            val length = BASS.BASS_ChannelGetLength(handle, BASS.BASS_POS_BYTE)
            val minBufferSize = getMinBufferSize()
            val buffer = ByteBuffer.allocate(minBufferSize)
            while (true) {
                val res = BASS.BASS_ChannelGetData(handle, buffer, buffer.limit())
                if (res < 0) break
                val rms = byteArray2SamplerArray(buffer.array(),2)
                waveformData.add(rms)

                val position = BASS.BASS_ChannelGetPosition(handle, BASS.BASS_POS_BYTE)
                val progress = 1f * position / length
                waveformFlow.value = Pair(progress, waveformData.toList())
            }
//            waveformFlow.value = Pair(1f, waveformData.toList())
        }
    }

    private fun calculateRMS(samples: ByteArray): Float {
        var sum = 0f
        for (sample in samples) {
            sum += (sample * sample)
        }
        return sqrt((sum / samples.size).toDouble()).toFloat()
    }

    private fun calculateRMS2(samples: ByteArray): Float {
        var sum = 0f
        for (sample in samples) {
            sum += (sample * sample)
        }
        return 10 * log10(1.0 * sum / samples.size).toFloat()
    }
}

fun byteArray2SamplerArray(data: ByteArray, size: Int = 1): IntArray {
    val tempSamplerData = IntArray(size)
    val byteCountPreSampler = 16 / 8
    if (data.isEmpty()) {
        return tempSamplerData
    }
    val samplerCount = if (data.size % byteCountPreSampler == 0) {
        data.size / byteCountPreSampler
    } else {
        data.size / byteCountPreSampler + 1
    }
    if (samplerCount == 0) {
        return tempSamplerData
    }
    var tempData = 0
    var j = 0
    for (i in 0 until samplerCount) {
        tempData = 0
        for (k in 0 until byteCountPreSampler) {
            var tempBuf = 0
            if (j + k < data.size) {
                tempBuf = data[j + k] shl k * 8
            }
            tempData = tempData or tempBuf
        }
        val position = i / (samplerCount / size)
        if (position < size) {
            tempSamplerData[position] = tempSamplerData[position].coerceAtLeast(abs(tempData))
        } else {
            break
        }
        j += byteCountPreSampler
    }
    return tempSamplerData
}

private infix fun Byte.shl(that: Int): Int = this.toInt().shl(that)