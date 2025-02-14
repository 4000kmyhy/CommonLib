package com.lib.media.utils

import android.provider.MediaStore

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/20 15:51
 **/
object SortOrder {
    object MusicSortOrder {
        const val DATE_ADDED = MediaStore.Audio.Media.DATE_ADDED
        const val DATE_ADDED_DESC = MediaStore.Audio.Media.DATE_ADDED + " desc"

        const val TITLE_A_Z = MediaStore.Audio.Media.TITLE
        const val TITLE_Z_A = MediaStore.Audio.Media.TITLE + " desc"

        const val DURATION = MediaStore.Audio.Media.DURATION
        const val DURATION_DESC = MediaStore.Audio.Media.DURATION + " desc"
    }

    object ArtistSortOrder {
        const val ARTIST_A_Z = "ARTIST_A_Z"
        const val ARTIST_Z_A = "ARTIST_Z_A"

        const val ARTIST_MUSIC_COUNT = "ARTIST_MUSIC_COUNT"
        const val ARTIST_MUSIC_COUNT_DESC = "ARTIST_MUSIC_COUNT_DESC"
    }

    object AlbumSortOrder {
        const val ALBUM_A_Z = "ALBUM_A_Z"
        const val ALBUM_Z_A = "ALBUM_Z_A"

        const val ALBUM_MUSIC_COUNT = "ALBUM_MUSIC_COUNT"
        const val ALBUM_MUSIC_COUNT_DESC = "ALBUM_MUSIC_COUNT_DESC"
    }

    object FolderSortOrder {
        const val FOLDER_A_Z = "FOLDER_A_Z"
        const val FOLDER_Z_A = "FOLDER_Z_A"

        const val FOLDER_MUSIC_COUNT = "FOLDER_MUSIC_COUNT"
        const val FOLDER_MUSIC_COUNT_DESC = "FOLDER_MUSIC_COUNT_DESC"
    }
}