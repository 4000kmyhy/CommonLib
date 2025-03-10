package com.lib.bass.utils

import kotlin.math.abs

/**
 * desc:
 **
 * user: xujj
 * time: 2024/3/15 10:59
 **/
fun byteArray2Sampler(data: ByteArray?, size: Int): Int? {
    val byteCountPreSampler = 16 / 8
    if (data == null || size == 0) {
        return null
    }
    val samplerCount = if (size % byteCountPreSampler == 0) {
        size / byteCountPreSampler
    } else {
        size / byteCountPreSampler + 1
    }
    if (samplerCount == 0) {
        return null
    }
    var tempData = 0
    var maxSamplerData = 0
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
        maxSamplerData = maxSamplerData.coerceAtLeast(abs(tempData))
        j += byteCountPreSampler
    }
    return maxSamplerData
}

fun byteArray2SamplerArray(data: ByteArray?, size: Int): IntArray? {
    val byteCountPreSampler = 16 / 8
    if (data == null || size == 0) {
        return null
    }
    val samplerCount = if (size % byteCountPreSampler == 0) {
        size / byteCountPreSampler
    } else {
        size / byteCountPreSampler + 1
    }
    if (samplerCount == 0) {
        return null
    }
    var tempData = 0
//        val tempSamplerData = IntArray(samplerCount)
    val tempSamplerData = IntArray(4)
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
//            tempSamplerData[i] = tempData
        val position = i / (samplerCount / 4)
        if (position < 4) {
            tempSamplerData[position] = tempSamplerData[position].coerceAtLeast(abs(tempData))
        } else {
            break
        }
        j += byteCountPreSampler
    }
    return tempSamplerData
}

private infix fun Byte.shl(that: Int): Int = this.toInt().shl(that)