package com.lib.lib_lyric.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2024/2/22 11:23
 **/
class LyricBean(var time: Long, var text: String) : Comparable<LyricBean> {

    var endTime: Long = 0

    override fun compareTo(other: LyricBean): Int {
        return (time - other.time).toInt()
    }

    override fun toString(): String {
        return "LyricBean(text='$text', time=$time, endTime=$endTime)"
    }
}
