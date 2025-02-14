package com.lib.media.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import com.lib.media.database.PlaylistOpenHelper
import com.lib.media.entity.Music
import org.json.JSONObject
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/20 14:54
 **/
object MusicLoader {

    private val mediaColumns = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATE_ADDED,
    )

    /**
     * 获取音乐集合
     */
    fun getMusicListBase(
        context: Context?,
        selection: String? = null,
        sortOrder: String? = MediaConfig.getInstance().musicSortOrder,
        parentDir: String? = null
    ): MutableList<Music> {
        val musicList = ArrayList<Music>()
        if (context == null) return musicList

        val sb = StringBuilder()
        sb.append(MediaStore.Audio.Media.DURATION + " > 0")
        sb.append(" AND " + MediaStore.Audio.Media.ARTIST_ID + " > 0")
        sb.append(" AND " + MediaStore.Audio.Media.ALBUM_ID + " > 0")
        sb.append(" AND " + MediaStore.Audio.Media.TITLE + " != ''")
        sb.append(" AND " + MediaStore.Audio.Media.ARTIST + " != 'null'")
        sb.append(" AND " + MediaStore.Audio.Media.ALBUM + " != 'null'")
        sb.append(" AND " + MediaStore.Audio.Media.DISPLAY_NAME + " != 'null'")
        if (!TextUtils.isEmpty(selection)) {
            sb.append(" AND $selection")
        }
        if (!TextUtils.isEmpty(parentDir)) {
            sb.append(" AND " + MediaStore.Audio.Media.DATA + " LIKE '" + parentDir + "%'")
        }
        var mSortOrder = sortOrder
        if (TextUtils.isEmpty(mSortOrder)) {
            mSortOrder = MediaStore.Audio.Media.TITLE
        }

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                mediaColumns,
                sb.toString(),
                null,
                mSortOrder
            )
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val artistId =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID))
                    val albumId =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                    val title =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                    val artist =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    val album =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                    val data =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                    val displayName =
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                    val duration =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                    val dateAdded =
                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))

                    //文件不存在
                    if (!File(data).exists()) {
                        continue
                    }
                    if (!TextUtils.isEmpty(parentDir)) {
                        //有文件夹路径时过滤文件夹
                        if (!TextUtils.equals(data, "$parentDir/$displayName")) {
                            continue
                        }
                    }

                    val music = Music(
                        id,
                        artistId,
                        albumId,
                        title,
                        artist,
                        album,
                        displayName,
                        data,
                        duration,
                        dateAdded
                    )
                    musicList.add(music)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return musicList
    }

    fun getMusicList(context: Context?): MutableList<Music> {
        return getMusicListBase(context)
    }

    /**
     * 通过歌名搜索音乐
     */
    fun getMusicListByTitle(context: Context?, name: String?): MutableList<Music> {
        var selection: String? = null
        if (!name.isNullOrEmpty()) {
            selection = MediaStore.Audio.Media.TITLE + " LIKE '%" + name.lowercase() + "%'"
        }
        return getMusicListBase(context, selection = selection)
    }

    /**
     * 通过歌名、歌手名搜索音乐
     */
    fun getMusicListByTitleOrArtist(context: Context?, name: String?): MutableList<Music> {
        var selection: String? = null
        if (!name.isNullOrEmpty()) {
            selection =
                MediaStore.Audio.Media.TITLE + " LIKE '%" + name.lowercase() + "%'" + " OR " +
                        MediaStore.Audio.Media.ARTIST + " LIKE '%" + name.lowercase() + "%'"
        }
        return getMusicListBase(context, selection = selection)
    }

    fun getMusicListByArtistId(context: Context?, artistId: Long): MutableList<Music> {
        val selection = MediaStore.Audio.Media.ARTIST_ID + " = " + artistId
        return getMusicListBase(context, selection = selection)
    }

    fun getMusicListByAlbumId(context: Context?, albumId: Long): MutableList<Music> {
        val selection = MediaStore.Audio.Media.ALBUM_ID + " = " + albumId
        return getMusicListBase(context, selection = selection)
    }

    fun getMusicListByDir(
        context: Context?,
        parentDir: String?,
        sortOrder: String? = MediaConfig.getInstance().musicSortOrder
    ): MutableList<Music> {
        return getMusicListBase(context, sortOrder = sortOrder, parentDir = parentDir)
    }

    fun getMusicListByPlaylistId(
        context: Context?,
        playlistId: Long
    ): MutableList<Music> {
        val musicIdsList = PlaylistOpenHelper(context).queryMusicIds(playlistId)
        return getMusicListByIds(context, musicIdsList)
    }

    /**
     * 通过id集合获取音乐集合
     */
    fun getMusicListByIds(context: Context?, ids: List<Long>?): MutableList<Music> {
        if (ids == null) return ArrayList()
        val sb = StringBuilder()
        sb.append("_id IN (")
        for (i in ids.indices) {
            sb.append(ids[i])
            if (i < ids.size - 1) {
                sb.append(",")
            }
        }
        sb.append(")")
        return getMusicListBase(context, selection = sb.toString())
    }

    /**
     * 获取按id集合顺序的音乐集合
     */
    fun getMusicListOrderByIds(context: Context?, ids: List<Long>?): MutableList<Music> {
        if (ids == null) return ArrayList()
        val newList = ArrayList<Music>()
        val musicList = getMusicListByIds(context, ids)
        val map: Map<Long, Music> = musicList.associateBy { it.id }
        for (id in ids) {
            map.get(id)?.let { newList.add(it) }
        }
        return newList
    }

    /**
     * 获取音乐集合的id集合
     */
    fun getMusicIdsList(musicList: List<Music?>?): ArrayList<Long> {
        val ids = ArrayList<Long>()
        if (musicList == null) return ids
        for (music in musicList) {
            if (music != null) {
                ids.add(music.id)
            } else {
                ids.add(-1L)
            }
        }
        return ids
    }

    /**
     * 获取音乐集合的id数组
     */
    fun getMusicIdsArray(musicList: List<Music?>?): LongArray? {
        if (musicList == null) return null
        val ids = LongArray(musicList.size)
        for (i in musicList.indices) {
            if (musicList[i] != null) {
                ids[i] = musicList[i]!!.id
            } else {
                ids[i] = -1
            }
        }
        return ids
    }

    fun sortMusicList(
        musicList: MutableList<Music>?,
        sortOrder: String? = MediaConfig.getInstance().musicSortOrder
    ) {
        when (sortOrder) {
            SortOrder.MusicSortOrder.TITLE_A_Z -> {
                musicList?.sortBy {
                    it.title
                }
            }

            SortOrder.MusicSortOrder.TITLE_Z_A -> {
                musicList?.sortByDescending {
                    it.title
                }
            }

            SortOrder.MusicSortOrder.DATE_ADDED -> {
                musicList?.sortBy {
                    it.dateAdded
                }
            }

            SortOrder.MusicSortOrder.DATE_ADDED_DESC -> {
                musicList?.sortByDescending {
                    it.dateAdded
                }
            }

            SortOrder.MusicSortOrder.DURATION -> {
                musicList?.sortBy {
                    it.duration
                }
            }

            SortOrder.MusicSortOrder.DURATION_DESC -> {
                musicList?.sortByDescending {
                    it.duration
                }
            }
        }
    }

    fun getIdsMusicList(
        musicList: List<Music>?,
        idList: List<Long>?
    ): List<Music>? {
        return musicList?.filter {
            idList?.contains(it.id) == true
        }
    }

    fun getOrderIdsMusicList(
        musicList: List<Music>?,
        idList: List<Long>?
    ): List<Music>? {
        if (musicList != null && idList != null) {
            val newList = ArrayList<Music>()
            val map: Map<Long, Music> = musicList.associateBy { it.id }
            for (id in idList) {
                map[id]?.let { newList.add(it) }
            }
            return newList
        }
        return null
    }

    fun searchMusicList(musicList: List<Music>?, name: String?): MutableList<Music>? {
        return if (!name.isNullOrEmpty()) {
            musicList?.filter {
                it.name.contains(name, true) ||
                        it.artist.contains(name, true)
            }?.toMutableList()
        } else {
            musicList?.toMutableList()
        }
    }

    fun searchMusicList2(musicList: List<Music>?, name: String?): MutableList<Music>? {
        return if (!name.isNullOrEmpty()) {
            musicList?.filter {
                it.name.contains(name, true) ||
                        it.artist.contains(name, true)
            }?.toMutableList()
        } else {
            ArrayList()
        }
    }

    fun searchMusicListByTitleOrDisplayName(
        musicList: MutableList<Music>?,
        name: String?,
        searchTitleOrDisplayName: Boolean
    ): MutableList<Music>? {
        return if (!name.isNullOrEmpty()) {
            musicList?.filter {
                if (searchTitleOrDisplayName) {
                    it.title.contains(name, true)
                } else {
                    it.displayName.contains(name, true)
                }
            }?.toMutableList()
        } else {
            musicList
        }
    }

    fun isInSearch(music: Music?, name: String?, searchTitleOrDisplayName: Boolean): Boolean {
        return if (music != null && !name.isNullOrEmpty()) {
            if (searchTitleOrDisplayName) {
                music.title.contains(name, true)
            } else {
                music.displayName.contains(name, true)
            }
        } else {
            false
        }
    }

    fun getArtistMusicList(musicList: List<Music>?, artistId: Long): MutableList<Music>? {
        return musicList?.filter {
            artistId == it.artistId
        }?.toMutableList()
    }

    fun getAlbumMusicList(musicList: List<Music>?, albumId: Long): MutableList<Music>? {
        return musicList?.filter {
            albumId == it.albumId
        }?.toMutableList()
    }

    fun getFolderMusicList(musicList: List<Music>?, dir: String?): MutableList<Music>? {
        if (dir.isNullOrEmpty()) return ArrayList()
        return musicList?.filter {
            TextUtils.equals(it.data, "$dir/${it.displayName}")
        }?.toMutableList()
    }

    fun getPlaylistMusicList(
        context: Context,
        musicList: List<Music>?,
        playlistId: Long
    ): MutableList<Music>? {
        if (musicList != null) {
            val musicIds = PlaylistOpenHelper(context).queryMusicIds(playlistId)
            val newList = ArrayList<Music>()
            for (music in musicList) {
                if (musicIds.contains(music.id)) {
                    newList.add(music)
                }
            }
            return newList
        }
        return null
    }

    fun getNotPlaylistMusicList(
        context: Context,
        musicList: List<Music>?,
        playlistId: Long
    ): MutableList<Music>? {
        if (musicList != null) {
            val musicIds = PlaylistOpenHelper(context).queryMusicIds(playlistId)
            val newList = ArrayList<Music>()
            newList.addAll(musicList)
            for (music in musicList) {
                if (musicIds.contains(music.id)) {
                    newList.remove(music)
                }
            }
            return newList
        }
        return null
    }

    fun getMusicByUri(context: Context?, uri: Uri): Music? {
        if (context == null) return null
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                mediaColumns,
                null,
                null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val id =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val artistId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID))
                val albumId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                val title =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val album =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                val data =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val displayName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                val duration =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val dateAdded =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))

                return Music(
                    id,
                    artistId,
                    albumId,
                    title,
                    artist,
                    album,
                    displayName,
                    data,
                    duration,
                    dateAdded
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }

    fun getMusicById(context: Context?, id: Long): Music? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        return getMusicByUri(context, uri)
    }

    fun isExists(context: Context?, musicId: Long): Boolean {
        if (context == null) return false
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri, null, null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return false
    }

    fun getMusicInfo(context: Context?, musicId: Long) {
        if (context == null) return
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.BITRATE,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri, projection, null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val title =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val album =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                val data =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val displayName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
                val duration =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                val dateAdded =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))
                val size =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                val bitrate =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE))
                val mimeType =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))

                val fileName = MediaUtils.getDisplayNameNoEx(displayName)
                val durationString = MediaUtils.stringForTime(duration)
                val dateAddedString = MediaUtils.stringForDate(dateAdded)
                val sizeString = if (size > 0) {
                    MediaUtils.stringForSize(size)
                } else {
                    MediaUtils.stringForSize(File(data).length())
                }
                val bitrateString = MediaUtils.stringForBitrate(bitrate)
                val mimeTypeEx = MediaUtils.getMimeTypeEx(mimeType)

                val jsonObject = JSONObject()
                jsonObject.put("fileName", fileName)
                jsonObject.put("title", title)
                jsonObject.put("artist", artist)
                jsonObject.put("album", album)
                jsonObject.put("data", data)
                jsonObject.put("duration", durationString)
                jsonObject.put("dateAdded", dateAddedString)
                jsonObject.put("size", sizeString)
                jsonObject.put("bitrate", bitrateString)
                jsonObject.put("mimeType", mimeTypeEx)

                Log.d("xxx", "getMusicInfo: " + jsonObject)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
    }
}