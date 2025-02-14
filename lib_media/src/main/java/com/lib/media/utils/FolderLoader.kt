package com.lib.media.utils

import android.content.Context
import com.lib.media.entity.Folder
import com.lib.media.entity.Music
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2023/10/26 14:53
 **/
object FolderLoader {

    fun getFolderListBase(
        musics: List<Music>?,
        name: String? = null,
        sortOrder: String? = MediaConfig.getInstance().folderSortOrder
    ): MutableList<Folder> {
        if (musics.isNullOrEmpty()) return ArrayList()

        val folderList = ArrayList<Folder>()
        val folderMap = LinkedHashMap<String, Folder>()
        try {
            for (music in musics) {
                val file: File? = File(music.data).parentFile
                if (file != null) {
                    val folderName = file.name
                    val path = file.path
                    var folder: Folder? = folderMap[path]
                    if (folder == null) {
                        folder = Folder(folderName, path, 1)
                        folderMap[path] = folder
                    } else {
                        folder.musicCount = folder.musicCount + 1
                        folderMap[path] = folder
                    }
                }
            }
            val set: Set<String> = folderMap.keys
            for (path in set) {
                val folder: Folder? = folderMap[path]
                if (folder != null) {
                    if (name.isNullOrEmpty()) {
                        folderList.add(folder)
                    } else {
                        if (folder.name.lowercase().contains(name.lowercase())) {
                            folderList.add(folder)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        when (sortOrder) {
            SortOrder.FolderSortOrder.FOLDER_A_Z -> {
                folderList.sortBy {
                    it.name
                }
            }

            SortOrder.FolderSortOrder.FOLDER_Z_A -> {
                folderList.sortByDescending {
                    it.name
                }
            }

            SortOrder.FolderSortOrder.FOLDER_MUSIC_COUNT -> {
                folderList.sortBy {
                    it.musicCount
                }
            }

            SortOrder.FolderSortOrder.FOLDER_MUSIC_COUNT_DESC -> {
                folderList.sortByDescending {
                    it.musicCount
                }
            }
        }
        return folderList
    }

    fun getFolderList(
        context: Context,
        name: String? = null,
        sortOrder: String? = MediaConfig.getInstance().folderSortOrder
    ): MutableList<Folder> {
        val musicList = MusicLoader.getMusicList(context)
        return getFolderListBase(musicList, name, sortOrder)
    }
}