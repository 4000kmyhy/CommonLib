package com.yhy.lib_retrofit.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/3/18 11:30
 **/
data class SmartBox(val data: Data) {

    data class Data(
        val album: DataTab,
        val singer: DataTab
    )

    data class DataTab(val itemlist: List<ItemList>)

    data class ItemList(
        val mid: String,
        val name: String,
        val pic: String,
        val singer: String
    )

    fun getAlbumList(): List<ItemList> = data.album.itemlist

    fun getArtistList(): List<ItemList> = data.singer.itemlist
}