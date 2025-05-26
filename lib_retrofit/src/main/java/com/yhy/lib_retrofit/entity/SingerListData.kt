package com.yhy.lib_retrofit.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/21 17:18
 **/
data class SingerListData(val result: Result){

    data class Result(val data: Data)

    data class Data(
        val singerlist: List<Singer>,
        val tags: Tag
    )

    data class Singer(
        val singer_mid: String,
        val singer_name: String,
        val singer_pic: String
    )

    data class Tag(
        val area: List<SingerTag>,
        val sex: List<SingerTag>,
        val genre: List<SingerTag>,
        val index: List<SingerTag>
    )

    data class SingerTag(
        val id: Int,
        val name: String
    )

    fun getSingerList(): List<Singer> = result.data.singerlist

    fun getAreaTag(): List<SingerTag> = result.data.tags.area
    fun getSexTag(): List<SingerTag> = result.data.tags.sex
    fun getGenreTag(): List<SingerTag> = result.data.tags.genre
    fun getIndexTag(): List<SingerTag> = result.data.tags.index
}