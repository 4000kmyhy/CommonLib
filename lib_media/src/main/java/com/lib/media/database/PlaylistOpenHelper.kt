package com.lib.media.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.lib.media.entity.Playlist

class PlaylistOpenHelper(context: Context?, dbName: String = "playlist.db") :

    SQLiteOpenHelper(context, dbName, null, VERSION_CODE) {

    companion object {
        private const val VERSION_CODE = 1

        const val TABLE_PLAYLIST = "playlist"
        const val PLAYLIST_ID = "pid"
        const val PLAYLIST_NAME = "pname"

        const val TABLE_MUSIC = "music"
        const val MUSIC_ID = "mid"
        const val MUSIC_PLAYLIST_ID = "music_pid"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val sql = "create table IF NOT EXISTS $TABLE_PLAYLIST (" +
                "$PLAYLIST_ID integer PRIMARY KEY AUTOINCREMENT," +
                "$PLAYLIST_NAME varchar(100) not null unique" +
                ")"
        db?.execSQL(sql)

        val sql2 = "create table IF NOT EXISTS $TABLE_MUSIC (" +
                "$MUSIC_ID integer not null," +
                "$MUSIC_PLAYLIST_ID integer not null," +
                "foreign key($MUSIC_PLAYLIST_ID) references $TABLE_PLAYLIST($PLAYLIST_ID)" +
                ",primary key($MUSIC_ID,$MUSIC_PLAYLIST_ID)" +
                ")"
        db?.execSQL(sql2)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    private var db: SQLiteDatabase? = null

    fun addDefaultPlaylist(pid: Long, pname: String) {
        try {
            db = writableDatabase
            val sql =
                "insert into $TABLE_PLAYLIST($PLAYLIST_ID,$PLAYLIST_NAME) values($pid,'$pname')"
            db?.execSQL(sql)
        } catch (e: Exception) {//名称已存在
            e.printStackTrace()
        } finally {
            db?.close()
        }
    }

    fun addPlaylist(pname: String): Long {
        var pid = -1L
        try {
            db = writableDatabase
            val sql = "insert into $TABLE_PLAYLIST($PLAYLIST_NAME) values('$pname')"
            db?.execSQL(sql)

            val sql2 = "select $PLAYLIST_ID from $TABLE_PLAYLIST where $PLAYLIST_NAME = '$pname'"
            db?.rawQuery(sql2, null)?.let {
                if (it.moveToFirst()) {
                    pid = it.getLong(it.getColumnIndexOrThrow(PLAYLIST_ID))
                }
                it.close()
            }
        } catch (e: Exception) {//名称已存在
            e.printStackTrace()
        } finally {
            db?.close()
        }
        return pid
    }

    fun createPlaylist(pname: String): Playlist? {
        var cursor: Cursor? = null
        try {
            db = writableDatabase
            val sql = "insert into $TABLE_PLAYLIST($PLAYLIST_NAME) values('$pname')"
            db?.execSQL(sql)

            val sql2 = "select $PLAYLIST_ID from $TABLE_PLAYLIST where $PLAYLIST_NAME = '$pname'"
            cursor = db?.rawQuery(sql2, null)
            if (cursor != null && cursor.moveToFirst()) {
                val pid = cursor.getLong(cursor.getColumnIndexOrThrow(PLAYLIST_ID))
                return Playlist(pid, pname, 0)
            }
        } catch (e: Exception) {//名称已存在
            e.printStackTrace()
        } finally {
            cursor?.close()
            db?.close()
        }
        return null
    }

    fun deletePlaylist(pid: Long) {
        try {
            db = writableDatabase
            //删除歌单表中数据
            val sql = "delete from $TABLE_PLAYLIST where $PLAYLIST_ID = $pid"
            db?.execSQL(sql)
            //删除歌曲表中数据
            val sq2 = "delete from $TABLE_MUSIC where $MUSIC_PLAYLIST_ID = $pid"
            db?.execSQL(sq2)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
    }

    fun updatePlaylist(pid: Long, pname: String): Boolean {
        try {
            db = writableDatabase
            val sql =
                "update $TABLE_PLAYLIST set $PLAYLIST_NAME = '$pname' where $PLAYLIST_ID = $pid"
            db?.execSQL(sql)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
        return false
    }

    fun isPlaylistExist(pname: String): Boolean {
        var exist = false
        try {
            db = readableDatabase
            val sql2 = "select $PLAYLIST_ID from $TABLE_PLAYLIST where $PLAYLIST_NAME = '$pname'"
            db?.rawQuery(sql2, null)?.let {
                if (it.moveToFirst()) {
                    exist = true
                }
                it.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
        return exist
    }

    fun queryPlaylists(): MutableList<Playlist> {
        val playlists = ArrayList<Playlist>()
        var cursor: Cursor? = null
        try {
            db = readableDatabase
            val sql =
                "select $TABLE_PLAYLIST.$PLAYLIST_ID,$TABLE_PLAYLIST.$PLAYLIST_NAME,count(distinct $TABLE_MUSIC.$MUSIC_ID) as song_count" +
                        " from $TABLE_PLAYLIST" +
                        " left join $TABLE_MUSIC on $TABLE_PLAYLIST.$PLAYLIST_ID = $TABLE_MUSIC.$MUSIC_PLAYLIST_ID" +
                        " group by $TABLE_PLAYLIST.$PLAYLIST_ID" +
                        " order by $TABLE_PLAYLIST.$PLAYLIST_ID" +
                        ""
            cursor = db!!.rawQuery(sql, null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(PLAYLIST_ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(PLAYLIST_NAME))
                    val songCount = cursor.getInt(cursor.getColumnIndexOrThrow("song_count"))
                    playlists.add(Playlist(id, name, songCount))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db?.close()
        }
        return playlists
    }

    fun queryPlaylists2(context: Context): MutableList<Playlist> {
        val playlists = ArrayList<Playlist>()
        var cursor: Cursor? = null
        try {
            db = readableDatabase
            val sql =
                "select $TABLE_PLAYLIST.$PLAYLIST_ID,$TABLE_PLAYLIST.$PLAYLIST_NAME,$TABLE_MUSIC.$MUSIC_ID" +
                        " from $TABLE_PLAYLIST" +
                        " left join $TABLE_MUSIC on $TABLE_PLAYLIST.$PLAYLIST_ID = $TABLE_MUSIC.$MUSIC_PLAYLIST_ID" +
                        ""
            cursor = db!!.rawQuery(sql, null)
            if (cursor != null) {
                val playlistMap = LinkedHashMap<Long, Playlist>()
                while (cursor.moveToNext()) {
                    val pid = cursor.getLong(cursor.getColumnIndexOrThrow(PLAYLIST_ID))
                    val pname = cursor.getString(cursor.getColumnIndexOrThrow(PLAYLIST_NAME))
                    val musicId = cursor.getLong(cursor.getColumnIndexOrThrow(MUSIC_ID))
                    var playlist = playlistMap[pid]
                    if (playlist == null) {
                        playlist = Playlist(pid, pname, 0)
                    }
//                    //判断歌曲是否存在
//                    if (MusicLoader.isExists(context, musicId)) {
//                        playlist.musicCount += 1
//                    }
                    playlist.addMusic(musicId)
                    playlistMap[pid] = playlist
                }

                val set: Set<Long> = playlistMap.keys
                for (playlistId in set) {
                    val playlist = playlistMap.get(playlistId)
                    if (playlist != null) {
                        playlist.countMusic(context)
                        playlists.add(playlist)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db?.close()
        }
        return playlists
    }

    /**
     * 添加歌曲到歌单，返回是否成功
     */
    fun addMusicToPlaylist(mid: Long, pid: Long): Boolean {
        var result = false
        try {
            db = writableDatabase
            val sql = "insert into $TABLE_MUSIC values($mid,$pid)"
            db?.execSQL(sql)
            result = true
        } catch (e: Exception) {//已存在
            e.printStackTrace()
        } finally {
            db?.close()
        }
        return result
    }

    /**
     * 批量添加歌曲到歌单，返回成功个数
     */
    fun addMusicsToPlaylist(musicIds: List<Long>, pid: Long): Int {
        var count = 0
        for (id in musicIds) {
            if (addMusicToPlaylist(id, pid)) {
                count++
            }
        }
        return count
    }

    /**
     * 移除歌单歌曲，返回是否成功
     */
    fun removeMusicFromPlaylist(mid: Long, pid: Long): Boolean {
        var result = false
        try {
            db = writableDatabase
            val sql =
                "delete from $TABLE_MUSIC where $MUSIC_ID = $mid and $MUSIC_PLAYLIST_ID = $pid"
            db?.execSQL(sql)
            result = true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
        return result
    }

    /**
     * 批量移除歌单歌曲，返回成功个数
     */
    fun removeMusicsFromPlaylist(musicIds: List<Long>, pid: Long): Int {
        var count = 0
        for (id in musicIds) {
            if (removeMusicFromPlaylist(id, pid)) {
                count++
            }
        }
        return count
    }

    fun queryMusicIds(pid: Long): MutableList<Long> {
        val musicIds = ArrayList<Long>()
        var cursor: Cursor? = null
        try {
            db = readableDatabase
            val sql = "select $MUSIC_ID" +
                    " from $TABLE_MUSIC" +
                    " where $MUSIC_PLAYLIST_ID = $pid" +
                    " group by $MUSIC_ID"
            cursor = db!!.rawQuery(sql, null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val mid = cursor.getLong(cursor.getColumnIndexOrThrow(MUSIC_ID))
                    musicIds.add(mid)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db?.close()
        }
        return musicIds
    }

    fun isInPlaylist(musicId: Long, pid: Long): Boolean {
        var cursor: Cursor? = null
        try {
            db = readableDatabase
            val sql = "select $MUSIC_ID,$PLAYLIST_ID" +
                    " from $TABLE_PLAYLIST" +
                    " left join $TABLE_MUSIC on $TABLE_PLAYLIST.$PLAYLIST_ID = $TABLE_MUSIC.$MUSIC_PLAYLIST_ID" +
                    " where $MUSIC_ID = '$musicId' and $PLAYLIST_ID = '$pid'" +
                    ""
            cursor = db?.rawQuery(sql, null)
            if (cursor != null && cursor.moveToFirst()) {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
            db?.close()
        }
        return false
    }
}