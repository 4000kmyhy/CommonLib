package com.lib.media.utils

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/20 16:23
 **/
object MediaUtils {

    /**
     * 获取音频专辑图
     */
    fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }

    fun shareMusic(context: Context, id: Long) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "audio/*"
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(Intent.createChooser(intent, ""))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareMusics(context: Context, ids: ArrayList<Long>) {
        if (ids.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (id in ids) {
            uris.add(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id))
        }
        try {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "audio/*"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(Intent.createChooser(intent, ""))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFileNameNoEx(file: File): String {
        return getDisplayNameNoEx(file.name)
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

    fun getMimeTypeEx(mimeType: String): String {
        if (!TextUtils.isEmpty(mimeType)) {
            val index = mimeType.lastIndexOf('/')
            if (index > -1 && index < mimeType.lastIndex) {
                return mimeType.substring(index + 1)
            }
        }
        return mimeType
    }

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

    fun stringForSize(size: Long): String {
        val showFloatFormat = DecimalFormat("0.00")
        return if (size >= 1024.0 * 1024.0 * 1024.0) {
            showFloatFormat.format(size / (1024.0 * 1024.0 * 1024.0)) + " GB"
        } else if (size >= 1024.0 * 1024.0) {
            showFloatFormat.format(size / (1024.0 * 1024.0)) + " MB"
        } else if (size >= 1024.0) {
            showFloatFormat.format(size / 1024.0) + " KB"
        } else {
            showFloatFormat.format(size) + " B"
        }
    }

    fun stringForDate(date: Long): String {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdfDate.format(date)
    }

    fun stringForBitrate(bitrate: Long): String {
        val df = DecimalFormat("0.00")
        return if (bitrate > 1000.0) {
            df.format(bitrate / 1000.0) + " Kbps"
        } else {
            df.format(bitrate) + " bps"
        }
    }
}