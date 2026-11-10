package com.yhy.commonlib.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import java.text.DecimalFormat

/**
 * desc:
 **
 * user: xujj
 * time: 2022/8/10 17:04
 **/
object StringUtils {

    @JvmStatic
    fun stringForTime(millisecond: Long): String {
        val second = millisecond / 1000
        val hh = second / 3600
        val mm = second % 3600 / 60
        val ss = second % 60
        return if (hh != 0L) {
            try {
                String.format("%02d:%02d:%02d", hh, mm, ss)
            } catch (e: OutOfMemoryError) {
                "00:00:00"
            }
        } else {
            try {
                String.format("%02d:%02d", mm, ss)
            } catch (e: OutOfMemoryError) {
                "00:00"
            }
        }
    }

    //String.format频繁调用会oom
    @JvmStatic
    fun stringForTime2(millisecond: Long): String {
        val second = millisecond / 1000
        val hh = second / 3600
        val mm = second % 3600 / 60
        val ss = second % 60
        return if (hh != 0L) {
            try {
                val hour = if (hh < 10) "0$hh" else "" + hh
                val min = if (mm < 10) "0$mm" else "" + mm
                val sec = if (ss < 10) "0$ss" else "" + ss
                "$hour:$min:$sec"
            } catch (e: OutOfMemoryError) {
                "00:00:00"
            }
        } else {
            try {
                val min = if (mm < 10) "0$mm" else "" + mm
                val sec = if (ss < 10) "0$ss" else "" + ss
                "$min:$sec"
            } catch (e: OutOfMemoryError) {
                "00:00"
            }
        }
    }

    /**
     * 获取00:00.0的时间
     */
    fun stringForMsTime(millisecond: Long): String {
        val second = millisecond / 1000.0
        val hh = (second / 3600).toInt()
        val mm = (second % 3600 / 60).toInt()
        val ss = (second % 60).toInt()
        val dec = (second % 60 * 10 % 10).toInt()
        return if (hh != 0) {
            try {
                String.format("%02d:%02d:%02d.%01d", hh, mm, ss, dec)
            } catch (e: OutOfMemoryError) {
                "00:00:00.0"
            }
        } else {
            try {
                String.format("%02d:%02d.%01d", mm, ss, dec)
            } catch (e: OutOfMemoryError) {
                "00:00.0"
            }
        }
    }

    @JvmStatic
    fun stringForHour(millisecond: Long): String {
        val second = millisecond / 1000
        val hh = second / 3600
        val mm = second % 3600 / 60
        val ss = second % 60
        return String.format("%02d:%02d:%02d", hh, mm, ss)
    }

    @JvmStatic
    fun stringForSize(size: Long): String {
        val showFloatFormat = DecimalFormat("0.0")
        return if (size >= 1024.0 * 1024.0 * 1024.0) {
            showFloatFormat.format(size / (1024.0 * 1024.0 * 1024.0)) + "GB"
        } else if (size >= 1024.0 * 1024.0) {
            showFloatFormat.format(size / (1024.0 * 1024.0)) + "MB"
        } else if (size >= 1024.0) {
            showFloatFormat.format(size / 1024.0) + "KB"
        } else {
            showFloatFormat.format(size) + "B"
        }
    }

    @JvmStatic
    fun stringForSpeed(speed: Double): String {
        val showFloatFormat = DecimalFormat("0.0")
        return if (speed >= 1024.0 * 1024.0) {
            showFloatFormat.format(speed / (1024.0 * 1024.0)) + " MB/s"
        } else if (speed >= 1024.0) {
            showFloatFormat.format(speed / 1024.0) + " KB/s"
        } else {
            showFloatFormat.format(speed) + " B/s"
        }
    }

    /**
     * 获取高亮文本
     */
    @JvmStatic
    fun getSpanText(text: String?, searchText: String?, color: Int): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)
        if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(searchText)) {
            val index = text!!.lowercase().indexOf(searchText!!.lowercase())
            if (index > -1) {
                try {
                    builder.setSpan(
                        ForegroundColorSpan(color),
                        index, index + searchText.length,
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                    )
                } catch (e: Exception) {
                    e.printStackTrace() //越界
                }
            }
        }
        return builder
    }

    /**
     * 过滤特殊字符
     */
    @JvmStatic
    fun patternStr(title: String): Boolean {
        val length = title.length
        for (i in 0 until length) {
            val c = title[i]
            if (c == '.' || c == ',' || c == '/' || c == '\\' || c == '@' || c == '#' || c == '*' || c == '&' || c.toInt() == 12290 || c == ':') {
                return false
            }
        }
        return true
    }

    /**
     * 首字母大写
     */
    fun String.uppercaseFirst(): String {
        return if (isNotEmpty()) {
            this[0].uppercaseChar() + substring(1)
        } else {
            this
        }
    }

    fun copyText(context: Context, text: String, label: String = "text") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun calculateBytes(str: String): Int {
        var bytes = 0
        for (char in str) {
            val code = char.code
            bytes += when {
                // ASCII 字符（包括数字、字母、常见符号）
                code <= 0x007F -> 1

                // 拉丁字母补充、希腊字母、西里尔字母等（通常视为1字节）
                code <= 0x07FF -> 1

                // 中日韩统一表意文字（CJK Unified Ideographs）
                code in 0x4E00..0x9FFF -> 2  // 基本汉字

                // 全角符号、日文假名、韩文字母等
                code in 0x3000..0x303F ||    // CJK符号和标点
                        code in 0x3040..0x309F ||    // 日文平假名
                        code in 0x30A0..0x30FF ||    // 日文片假名
                        code in 0xAC00..0xD7AF -> 2  // 韩文字母

                // 其他情况（表情符号、特殊符号等）
                else -> 2
            }
        }
        return bytes
    }
}