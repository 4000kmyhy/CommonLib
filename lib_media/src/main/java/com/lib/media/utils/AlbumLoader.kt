package com.lib.media.utils

import android.content.Context
import com.lib.media.entity.Album
import com.lib.media.entity.Music

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/26 14:07
 **/
object AlbumLoader {

    fun getAlbumListBase(
        musics: List<Music>?,
        name: String? = null,
        sortOrder: String? = MediaConfig.getInstance().albumSortOrder
    ): MutableList<Album> {
        if (musics.isNullOrEmpty()) return ArrayList()

        val albumList = ArrayList<Album>()
        val albumMap = LinkedHashMap<Long, Album>()
        try {
            for (music in musics) {
                val albumId = music.albumId
                val artistId = music.artistId
                val albumName = music.album
                val artistName = music.artist
                var album: Album? = albumMap[albumId]
                if (album == null) {
                    album = Album(albumId, albumName, artistId, artistName, 1)
                    albumMap[albumId] = album
                } else {
                    album.musicCount = album.musicCount + 1
                    albumMap[albumId] = album
                }
            }
            val set: Set<Long> = albumMap.keys
            for (albumId in set) {
                val album: Album? = albumMap[albumId]
                if (album != null) {
                    if (name.isNullOrEmpty()) {
                        albumList.add(album)
                    } else {
                        if (album.name.lowercase().contains(name.lowercase())) {
                            albumList.add(album)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        when (sortOrder) {
            SortOrder.AlbumSortOrder.ALBUM_A_Z -> {
                albumList.sortBy {
                    it.name
                }
            }

            SortOrder.AlbumSortOrder.ALBUM_Z_A -> {
                albumList.sortByDescending {
                    it.name
                }
            }

            SortOrder.AlbumSortOrder.ALBUM_MUSIC_COUNT -> {
                albumList.sortBy {
                    it.musicCount
                }
            }

            SortOrder.AlbumSortOrder.ALBUM_MUSIC_COUNT_DESC -> {
                albumList.sortByDescending {
                    it.musicCount
                }
            }
        }
        return albumList
    }

    fun getAlbumList(
        context: Context,
        name: String? = null,
        sortOrder: String? = MediaConfig.getInstance().albumSortOrder
    ): MutableList<Album> {
        val musicList = MusicLoader.getMusicList(context)
        return getAlbumListBase(musicList, name, sortOrder)
    }
}