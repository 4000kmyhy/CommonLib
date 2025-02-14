package com.lib.media.entity

import com.lib.media.entity.Music.Companion.new

/**
 * desc:
 **
 * user: xujj
 * time: 2023/5/5 14:34
 **/
class Queue(musicList: List<Music>, position: Int) : Cloneable{

    companion object {
        fun getQueueListWithDistinctKey(musicList: List<Music>): List<Music> {
            val newList = ArrayList<Music>()
            for (music in musicList) {
                if (newList.contains(music)) {//重复的歌曲要新建uuid
                    newList.add(music.new())
                } else {
                    newList.add(music)
                }
            }
            return newList
        }
    }

    private var queueList: MutableList<Music>? = null
    private var queuePosition = 0
    private var currentMusic: Music? = null

    init {
        queueList = ArrayList()
        queueList?.addAll(musicList)
//        for (music in musicList) {
//            if (queueList!!.contains(music)) {//重复的歌曲要新建uuid
//                queueList!!.add(music.new())
//            } else {
//                queueList!!.add(music)
//            }
//        }
        queuePosition = position

        queueList?.let {
            if (it.isNotEmpty()) {
                if (queuePosition in it.indices) {
                    currentMusic = it[queuePosition]
                } else {
                    currentMusic = it[0]
                    queuePosition = 0
                }
            }
        }
    }

    fun setQueueList(queueList: MutableList<Music>) {
        this.queueList = ArrayList()
        this.queueList?.addAll(queueList)
    }

    fun setQueuePosition(queuePosition: Int) {
        this.queuePosition = queuePosition
    }

    fun getQueueList() = queueList

    fun getQueuePosition() = queuePosition

    fun getCurrentMusic() = currentMusic

    fun getPrevMusic(): Music? {
        queueList?.let {
            if (it.isNotEmpty()) {
                return if (queuePosition - 1 >= 0) {
                    it[queuePosition - 1]
                } else {
                    it[it.lastIndex]
                }
            }
        }
        return null
    }

    fun getNextMusic(): Music? {
        queueList?.let {
            if (it.isNotEmpty()) {
                return if (queuePosition + 1 <= it.lastIndex) {
                    it[queuePosition + 1]
                } else {
                    it[0]
                }
            }
        }
        return null
    }

    fun getBottomList(): List<Music>? {
        val prevMusic = getPrevMusic()
        val nextMusic = getNextMusic()
        if (prevMusic != null &&
            currentMusic != null &&
            nextMusic != null
        ) {
            return listOf(
                prevMusic,
                currentMusic!!,
                nextMusic
            )
        }
        return null
    }

    override fun toString(): String {
        return "MusicQueue(queueList=$queueList, queuePosition=$queuePosition, currentMusic=$currentMusic)"
    }

   public override fun clone(): Any {
        return super.clone()
    }

}