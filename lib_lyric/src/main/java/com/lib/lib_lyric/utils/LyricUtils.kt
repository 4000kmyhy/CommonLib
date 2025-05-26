package com.lib.lib_lyric.utils

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.text.TextUtils
import android.text.format.DateUtils
import com.lib.lib_lyric.entity.LyricBean
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * desc:
 **
 * user: xujj
 * time: 2024/2/22 11:58
 **/
object LyricUtils {

    fun stringForTime(millisecond: Long): String {
        val second = millisecond / 1000
        val hh = second / 3600
        val mm = second % 3600 / 60
        val ss = second % 60
        var str = "00:00"
        str = if (hh != 0L) {
            String.format("%02d:%02d:%02d", hh, mm, ss)
        } else {
            String.format("%02d:%02d", mm, ss)
        }
        return str
    }

    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun px2dp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun sp2px(context: Context, spValue: Float): Int {
        val scale = context.resources.displayMetrics.scaledDensity
        return (spValue * scale + 0.5f).toInt()
    }

    fun px2sp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.scaledDensity
        return (pxValue / scale + 0.5f).toInt()
    }

    fun getAlphaColor(color: Int, alpha: Float): Int {
        val a = Math.min(255, Math.max(0, (alpha * 255).toInt())) shl 24
        val rgb = 0x00ffffff and color
        return a + rgb
    }

    fun getDisplayNameNoEx(displayName: String): String {
        if (!TextUtils.isEmpty(displayName)) {
            val dot = displayName.lastIndexOf('.')
            if (dot > -1) {
                return displayName.substring(0, dot)
            }
        }
        return displayName
    }

    private val PATTERN_LINE = Pattern.compile("((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\])+)(.+)")
    private val PATTERN_TIME = Pattern.compile("\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]")

    /**
     * 通过歌词文件路径解析歌词
     */
    fun parseLyricByPath(path: String): List<LyricBean> {
        return parseLyricByFile(File(path))
    }

    /**
     * 通过歌词文件解析歌词
     */
    fun parseLyricByFile(file: File): List<LyricBean> {
        val lyricList = ArrayList<LyricBean>()
        try {
            var str = ""
            val code = getCharset(file)
            val `in` = BufferedReader(InputStreamReader(FileInputStream(file), code))
            while (`in`.readLine().also { str = it } != null) {
                val list = parseLine(str)
                if (!list.isNullOrEmpty()) {
                    lyricList.addAll(list)
                }
            }
            `in`.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return parseLyricExEmpty(lyricList)
    }

    fun getCharset(file: File?): String {
        var charset = "GBK"
        val first3Bytes = ByteArray(3)
        try {
            var checked = false
            val bis = BufferedInputStream(FileInputStream(file))
            bis.mark(0)
            var read = bis.read(first3Bytes, 0, 3)
            if (read == -1) return charset
            if (first3Bytes[0] == 0xFF.toByte() &&
                first3Bytes[1] == 0xFE.toByte()
            ) {
                charset = "UTF-16LE"
                checked = true
            } else if (first3Bytes[0] == 0xFE.toByte() &&
                first3Bytes[1] == 0xFF.toByte()
            ) {
                charset = "UTF-16BE"
                checked = true
            } else if (first3Bytes[0] == 0xEF.toByte() && first3Bytes[1] == 0xBB.toByte() && first3Bytes[2] == 0xBF.toByte()) {
                charset = "UTF-8"
                checked = true
            }
            bis.reset()
            if (!checked) {
                while (bis.read().also { read = it } != -1) {
                    if (read >= 0xF0) break
                    if (read in 0x80..0xBF) //单独出现BF以下的，也算是GBK
                        break
                    if (read in 0xC0..0xDF) {
                        read = bis.read()
                        if (read in 0x80..0xBF) // 双字节 (0xC0 - 0xDF)
                        // (0x80 -0xBF),也可能在GB编码内
                            continue else break
                    } else if (read in 0xE0..0xEF) { // 也有可能出错，但是几率较小
                        read = bis.read()
                        if (read in 0x80..0xBF) {
                            read = bis.read()
                            if (read in 0x80..0xBF) {
                                charset = "UTF-8"
                                break
                            } else break
                        } else break
                    }
                }
            }
            bis.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return charset
    }

    /**
     * 通过歌词字符串解析歌词
     */
    fun parseLyricByString(lyric: String?): MutableList<LyricBean> {
        val lyricList = ArrayList<LyricBean>()
        if (lyric.isNullOrEmpty()) return lyricList
        val array = lyric.split("\\n".toRegex())
        for (line in array) {
            val list = parseLine(line)
            if (!list.isNullOrEmpty()) {
                lyricList.addAll(list)
            }
        }
        return parseLyricExEmpty(lyricList)
    }

    /**
     * 去掉空歌词行，如果有空歌词行时，获取当前空歌词行的开始时间，设置为上一行歌词的结束时间
     */
    private fun parseLyricExEmpty(list: MutableList<LyricBean>?): MutableList<LyricBean> {
        val lyricList = ArrayList<LyricBean>()
        if (!list.isNullOrEmpty()) {
            list.sort()
            for (lyricBean in list) {
                if (TextUtils.isEmpty(lyricBean.text.trim())) {
                    if (lyricList.size > 0 && lyricList[lyricList.size - 1].endTime == 0L) {
                        lyricList[lyricList.size - 1].endTime = lyricBean.time
                    }
                } else {
                    lyricList.add(lyricBean)
                }
            }
        }
        return lyricList
    }

    private fun parseLine(line: String): MutableList<LyricBean>? {
        var line = line
        if (TextUtils.isEmpty(line)) {
            return null
        }
        line = line.trim()
        //[00:06.51]可惜我们终于来到 一个句号
        var lineMatcher: Matcher = PATTERN_LINE.matcher(line)
        if (!lineMatcher.matches()) {
            line = "$line "
        }
        lineMatcher = PATTERN_LINE.matcher(line)
        if (!lineMatcher.matches()) {
            return null
        }

        val times = lineMatcher.group(1)
        var text = lineMatcher.group(3)
        text = text?.replace("&apos;", "'") //单引号转义

        val list = ArrayList<LyricBean>()
        //[00:06.51]
        val timeMatcher = PATTERN_TIME.matcher(times)
        while (timeMatcher.find()) {
            val min = timeMatcher.group(1).toLong()
            val sec = timeMatcher.group(2).toLong()
            val milString = timeMatcher.group(3)
            var mil = milString.toLong()
            // 如果毫秒是两位数，需要乘以10
            if (milString.length == 2) {
                mil *= 10
            }
            val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
            list.add(LyricBean(time, text))
        }
        return list
    }

    fun findShowLine(lyric: String?, time: Long): Int {
        return findShowLine(parseLyricByString(lyric), time)
    }

    /**
     * 二分法查找当前时间应该显示的行数（最后一个 <= time 的行数）
     */
    fun findShowLine(list: List<LyricBean>?, time: Long): Int {
        if (list.isNullOrEmpty()) return 0
        var left = 0
        var right = list.size
        while (left <= right) {
            val middle = (left + right) / 2
            val middleTime: Long = list[middle].time
            if (time < middleTime) {
                right = middle - 1
            } else {
                if (middle + 1 >= list.size || time < list[middle + 1].time) {
                    return middle
                }
                left = middle + 1
            }
        }
        return 0
    }

    fun getCurrentLyric(lyric: String?, time: Long): String {
        return getCurrentLyric(parseLyricByString(lyric), time)
    }

    fun getCurrentLyric(list: List<LyricBean>?, time: Long): String {
        val position = findShowLine(list, time)
        return if (list == null || position >= list.size) "" else list[position].text
    }

    fun getLrcFiles(context: Context, name: String = ""): List<File> {
        val lyricFiles = ArrayList<File>()
        var cursor: Cursor? = null
        try {
            val selection =
                MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE '%$name%$LRC_EXTENSION'"
            cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.Files.FileColumns.DATA),
                selection,
                null,
                MediaStore.Files.FileColumns.DISPLAY_NAME
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val data =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
                    lyricFiles.add(File(data))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return lyricFiles
    }

    fun findLyricPath(
        context: Context,
        title: String,
        artist: String
    ): String? {
        var cursor: Cursor? = null
        try {
            //搜索与歌曲同名的歌词文件
            var selection =
                MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE '$title$LRC_EXTENSION'"
            selection += " OR " + MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE '%$title%$artist%$LRC_EXTENSION'"
            selection += " OR " + MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE '%$artist%$title%$LRC_EXTENSION'"
            cursor = context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.Files.FileColumns.DATA),
                selection,
                null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * 外部歌词文件写入到私有目录
     */
    fun writeLrcFileByPath(context: Context, lrcPath: String, musicId: Long): Boolean {
        try {
            val srcFile = File(lrcPath)
            val targetFile = getInternalLrcFile(context, musicId)
            if (srcFile.exists()) {
                srcFile.copyTo(targetFile, true)
                return targetFile.exists()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 字符串写入文件
     */
    fun writeToFile(targetFile: File, lyric: String) {
        targetFile.mkdirs()
        if (targetFile.exists()) {
            targetFile.delete()
        }
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(targetFile)
            fos.write(lyric.toByteArray())
            fos.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 字符串写入到私有目录
     */
    fun writeLrcFileByString(context: Context, lyric: String, musicId: Long): Boolean {
        try {
            val targetFile = getInternalLrcFile(context, musicId)
            writeToFile(targetFile, lyric)
            return targetFile.exists()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}