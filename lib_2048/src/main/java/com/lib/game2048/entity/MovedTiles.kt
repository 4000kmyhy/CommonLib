package com.lib.game2048.entity

/**
 * desc:
 **
 * user: xujj
 * time: 2025/9/29 10:20
 **/
data class MovedTiles(val direction: Int, val tiles: MutableList<Tile> = ArrayList())