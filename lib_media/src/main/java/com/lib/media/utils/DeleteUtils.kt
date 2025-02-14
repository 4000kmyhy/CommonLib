package com.lib.media.utils

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.lib.media.entity.Music
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/20 15:31
 **/
object DeleteUtils {

    private const val DELETE_MUSIC_CODE = 0x02

    interface OnDeleteCallback {
        fun successOne(id: Long)
        fun success()
        fun failed()
        fun request()
    }

    private var onDeleteCallback: OnDeleteCallback? = null

    fun deleteMedia(activity: Activity?, music: Music, callback: OnDeleteCallback?) {
        val musics: MutableList<Music> = ArrayList()
        musics.add(music)
        deleteMedias(activity, musics, callback)
    }

    fun deleteMedias(activity: Activity?, musics: List<Music>, callback: OnDeleteCallback?) {
        onDeleteCallback = callback
        createDeleteRequest(activity, musics)
    }

    private fun createDeleteRequest(activity: Activity?, musics: List<Music>) {
        if (activity == null) return
        val failedMusics: List<Music> = getFailedMusics(activity, musics)
        if (!failedMusics.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                onResultCallback = object : OnResultCallback {
                    override fun grant() {
                        for (music in failedMusics) {
                            onDeleteCallback?.successOne(music.id)
                            val file = File(music.data)
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                        onDeleteCallback?.success()
                    }

                    override fun refuse() {
                        onDeleteCallback?.failed()
                    }
                }
                try {
                    val uris: MutableList<Uri> = ArrayList()
                    for (music in failedMusics) {
                        val targetUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, music.id)
                        uris.add(targetUri)
                    }
                    val pendingIntent = MediaStore.createDeleteRequest(activity.contentResolver, uris)
                    activity.startIntentSenderForResult(
                        pendingIntent.intentSender, DELETE_MUSIC_CODE,
                        null, 0, 0, 0
                    )
                    onDeleteCallback?.request()
                } catch (e: Exception) {
                    e.printStackTrace()
                    onDeleteCallback?.failed()
                }
            } else {
                onDeleteCallback?.failed()
            }
        }
    }

    private fun getFailedMusics(activity: Activity?, musics: List<Music>): List<Music> {
        val failedMusics: MutableList<Music> = ArrayList()
        if (activity == null) return failedMusics
        var deleteCount = 0
        for (music in musics) {
            val targetUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, music.id)
            try {
                val result = activity.contentResolver.delete(targetUri, null, null)
                if (result >= 1) {
                    onDeleteCallback?.successOne(music.id)
                    val file = File(music.data)
                    if (file.exists()) {
                        file.delete()
                    }
                    deleteCount++
                    if (deleteCount >= musics.size) {
                        onDeleteCallback?.success()
                    }
                } else {
                    onDeleteCallback?.failed()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                failedMusics.add(music)
            }
        }
        return failedMusics
    }

    private interface OnResultCallback {
        fun grant()
        fun refuse()
    }

    private var onResultCallback: OnResultCallback? = null

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == DELETE_MUSIC_CODE) {
            if (resultCode == -1) {
                onResultCallback?.grant()
            } else {
                onResultCallback?.refuse()
            }
        }
    }
}