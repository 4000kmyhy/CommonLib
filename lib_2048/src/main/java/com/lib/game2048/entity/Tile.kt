package com.lib.game2048.entity

class Tile(x: Int, y: Int, private var value: Long) : Cell(x, y) {

    private var mergedFrom: Array<Tile>? = null

    constructor(cell: Cell, value: Long) : this(cell.x, cell.y, value)

    fun updatePosition(cell: Cell) {
        x = cell.x
        y = cell.y
    }

    fun getValue(): Long {
        return value
    }

    fun getMergedFrom(): Array<Tile>? {
        return mergedFrom
    }

    fun setMergedFrom(tile: Array<Tile>?) {
        mergedFrom = tile
    }

    override fun toString(): String {
        return "Tile(x=$x, y=$y, value=$value)"
    }

    companion object {
        fun Tile.copy(): Tile = Tile(x, y, value)
    }
}