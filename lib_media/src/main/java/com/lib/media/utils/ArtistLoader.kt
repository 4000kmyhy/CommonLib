package com.lib.media.utils

import android.content.Context
import com.lib.media.entity.Artist
import com.lib.media.entity.Music

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/20 16:58
 **/
object ArtistLoader {

    fun getArtistListBase(
        musics: List<Music>?,
        name: String? = null,
        sortOrder: String? = MediaConfig.getInstance().artistSortOrder
    ): MutableList<Artist> {
        if (musics.isNullOrEmpty()) return ArrayList()

        val artistList = ArrayList<Artist>()
        val artistMap = LinkedHashMap<Long, Artist>()
        try {
            for (music in musics) {
                val artistId = music.artistId
                val artistName = music.artist
                var artist: Artist? = artistMap.get(artistId)
                if (artist == null) {
                    artist = Artist(artistId, artistName, 1)
                    artistMap.put(artistId, artist)
                } else {
                    artist.musicCount = artist.musicCount + 1
                    artistMap.put(artistId, artist)
                }
            }
            val set: Set<Long> = artistMap.keys
            for (artistId in set) {
                val artist: Artist? = artistMap.get(artistId)
                if (artist != null) {
                    if (name.isNullOrEmpty()) {
                        artistList.add(artist)
                    } else {
                        if (artist.name.lowercase().contains(name.lowercase())) {
                            artistList.add(artist)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        when (sortOrder) {
            SortOrder.ArtistSortOrder.ARTIST_A_Z -> {
                artistList.sortBy {
                    it.name
                }
            }

            SortOrder.ArtistSortOrder.ARTIST_Z_A -> {
                artistList.sortByDescending {
                    it.name
                }
            }

            SortOrder.ArtistSortOrder.ARTIST_MUSIC_COUNT -> {
                artistList.sortBy {
                    it.musicCount
                }
            }

            SortOrder.ArtistSortOrder.ARTIST_MUSIC_COUNT_DESC -> {
                artistList.sortByDescending {
                    it.musicCount
                }
            }
        }
        return artistList
    }

    fun getArtistList(
        context: Context?,
        name: String? = null,
        sortOrder: String? = MediaConfig.getInstance().artistSortOrder
    ): MutableList<Artist> {
        val musicList = MusicLoader.getMusicList(context)
        return getArtistListBase(musicList, name, sortOrder)
    }
}