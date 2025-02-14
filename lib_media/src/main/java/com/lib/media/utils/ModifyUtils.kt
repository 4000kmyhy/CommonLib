package com.lib.media.utils

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/20 15:20
 **/
object ModifyUtils {

    private const val MODIFY_INFO_CODE = 0x01

    interface OnModifyCallback {
        fun success()
        fun failed()
        fun request()
    }

    private var onModifyCallback: OnModifyCallback? = null

    fun modifyInfo(
        activity: Activity?,
        id: Long,
        oldPath: String,
        newName: String,
        displayName: String,
        newPath: String,
        callback: OnModifyCallback?
    ) {
        if (activity == null) return
        onModifyCallback = callback
        try {
            modifyInfoInternal(activity, id, oldPath, newName, displayName, newPath)
        } catch (e: Exception) {
            e.printStackTrace()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                onResultCallback = object : OnResultCallback {
                    override fun grant() {
                        try {
                            modifyInfoInternal(activity, id, oldPath, newName, displayName, newPath)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            onModifyCallback?.failed()
                        }
                    }

                    override fun refuse() {
                        onModifyCallback?.failed()
                    }
                }
                try {
                    val uris: MutableList<Uri> = ArrayList()
                    val targetUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    uris.add(targetUri)
                    val pendingIntent = MediaStore.createWriteRequest(activity.contentResolver, uris)
                    activity.startIntentSenderForResult(
                        pendingIntent.intentSender, MODIFY_INFO_CODE,
                        null, 0, 0, 0
                    )
                    onModifyCallback?.request()
                } catch (e1: Exception) {
                    e1.printStackTrace()
                    onModifyCallback?.failed()
                }
            } else {
                onModifyCallback?.failed()
            }
        }
    }

    private fun modifyInfoInternal(activity: Activity, id: Long, oldPath: String, newName: String, displayName: String, newPath: String) {
        val targetUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        val values = ContentValues()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put("is_pending", 1)
            activity.contentResolver.update(targetUri, values, null, null)
            values.clear()
            values.put("is_pending", 0)
        } else {
            values.put(MediaStore.Audio.Media.DATA, newPath)
        }
        values.put(MediaStore.Audio.Media.TITLE, newName)
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
        val result = activity.contentResolver.update(targetUri, values, null, null)
        if (result >= 1) {
            val from = File(oldPath)
            val to = File(newPath)
            //原文件存在并且新文件不存在时重命名文件，Android10以上修改媒体库会自动重命名文件了
            if (from.exists() && !to.exists()) {
                if (from.renameTo(to)) {
                    onModifyCallback?.success()
                } else {
                    onModifyCallback?.failed()
                }
            } else {
                onModifyCallback?.success()
            }
        } else {
            onModifyCallback?.failed()
        }
    }

    private interface OnResultCallback {
        fun grant()
        fun refuse()
    }

    private var onResultCallback: OnResultCallback? = null

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == MODIFY_INFO_CODE) {
            if (resultCode == -1) {
                onResultCallback?.grant()
            } else {
                onResultCallback?.refuse()
            }
        }
    }
}