package com.yhy.lib_retrofit.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/17 17:11
 **/
data class SongData(
    val songmid: String,
    val songname: String,
    val albummid: String,
    val albumname: String,
    val singer: List<Singer>,
    val pay: Pay
) {

    companion object {
        fun List<SongData>.getIdArray(): Array<String> {
            val idList = ArrayList<String>()
            this.forEach {
                idList.add(it.songmid)
            }
            return idList.toTypedArray()
        }
    }

    data class Singer(
        val mid: String,
        val name: String
    )

    data class Album(
        val mid: String,
        val title: String
    )

    data class Pay(
        val payplay: Int,
        val pay_play: Int
    )

    fun getSingerName(): String {
        val singerBuilder = StringBuilder()
        singer.forEachIndexed { index, singer ->
            if (index == 0) {
                singerBuilder.append(singer)
            } else {
                singerBuilder.append("/").append(singer.name)
            }
        }
        return singerBuilder.toString()
    }

    fun isFree(): Boolean {
        return pay.payplay == 0 && pay.pay_play == 0
    }
}