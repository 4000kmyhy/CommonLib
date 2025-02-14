package com.lib.media.utils

import android.content.Context

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/20 15:54
 **/
class MediaConfig {

    companion object {
        private var mInstance: MediaConfig? = null

        fun getInstance(): MediaConfig {
            if (mInstance == null) {
                synchronized(MediaConfig::class.java) {
                    mInstance = MediaConfig()
                }
            }
            return mInstance!!
        }

        private const val SHARE_DEF_NAME = "MediaConfig"

        private fun getSP(context: Context?, key: String, defValue: String?): String? {
            if (context == null) return defValue
            val sp = context.getSharedPreferences(SHARE_DEF_NAME, Context.MODE_PRIVATE)
            return sp.getString(key, defValue)
        }

        private fun setSP(context: Context?, key: String, value: String) {
            if (context == null) return
            val sp = context.getSharedPreferences(SHARE_DEF_NAME, Context.MODE_PRIVATE)
            sp.edit().putString(key, value).apply()
        }

        private const val KEY_MUSIC_SORT_ORDER = "music_sort_order"
        private const val KEY_ARTIST_SORT_ORDER = "artist_sort_order"
        private const val KEY_ALBUM_SORT_ORDER = "album_sort_order"
        private const val KEY_FOLDER_SORT_ORDER = "folder_sort_order"
    }

    var musicSortOrder: String? = SortOrder.MusicSortOrder.DATE_ADDED_DESC
        private set
    var artistSortOrder: String? = SortOrder.ArtistSortOrder.ARTIST_A_Z
        private set
    var albumSortOrder: String? = SortOrder.AlbumSortOrder.ALBUM_A_Z
        private set
    var folderSortOrder: String? = SortOrder.FolderSortOrder.FOLDER_A_Z
        private set

    var useDisplayNameOrTitle = false

    fun initAll(context: Context, useDisplayNameOrTitle: Boolean = false) {
        initMusicSortOrder(context)
        initArtistSortOrder(context)
        initAlbumSortOrder(context)
        initFolderSortOrder(context)
        this.useDisplayNameOrTitle = useDisplayNameOrTitle
    }

    fun initMusicSortOrder(
        context: Context,
        defSortOrder: String = SortOrder.MusicSortOrder.DATE_ADDED_DESC
    ) {
        musicSortOrder = getSP(context, KEY_MUSIC_SORT_ORDER, defSortOrder)
    }

    fun initArtistSortOrder(
        context: Context,
        defSortOrder: String = SortOrder.ArtistSortOrder.ARTIST_A_Z
    ) {
        artistSortOrder = getSP(context, KEY_ARTIST_SORT_ORDER, defSortOrder)
    }

    fun initAlbumSortOrder(
        context: Context,
        defSortOrder: String = SortOrder.AlbumSortOrder.ALBUM_A_Z
    ) {
        albumSortOrder = getSP(context, KEY_ALBUM_SORT_ORDER, defSortOrder)
    }

    fun initFolderSortOrder(
        context: Context,
        defSortOrder: String = SortOrder.FolderSortOrder.FOLDER_A_Z
    ) {
        folderSortOrder = getSP(context, KEY_FOLDER_SORT_ORDER, defSortOrder)
    }

    fun setMusicSortOrder(context: Context?, sortOrder: String) {
        if (musicSortOrder != sortOrder) {
            musicSortOrder = sortOrder
            setSP(context, KEY_MUSIC_SORT_ORDER, sortOrder)
        }
    }

    fun setArtistSortOrder(context: Context?, sortOrder: String) {
        if (artistSortOrder != sortOrder) {
            artistSortOrder = sortOrder
            setSP(context, KEY_ARTIST_SORT_ORDER, sortOrder)
        }
    }

    fun setAlbumSortOrder(context: Context?, sortOrder: String) {
        if (albumSortOrder != sortOrder) {
            albumSortOrder = sortOrder
            setSP(context, KEY_ALBUM_SORT_ORDER, sortOrder)
        }
    }

    fun setFolderSortOrder(context: Context?, sortOrder: String) {
        if (folderSortOrder != sortOrder) {
            folderSortOrder = sortOrder
            setSP(context, KEY_FOLDER_SORT_ORDER, sortOrder)
        }
    }
}