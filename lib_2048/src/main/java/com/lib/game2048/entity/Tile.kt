package com.lib.game2048.entity

class Tile(x: Int, y: Int, private var value: Int) : Cell(x, y) {

    private var mergedFrom: Array<Tile>? = null

    constructor(cell: Cell, value: Int) : this(cell.x, cell.y, value)

    fun updatePosition(cell: Cell) {
        x = cell.x
        y = cell.y
    }

    fun getValue(): Int {
        return value
    }

    fun getMergedFrom(): Array<Tile>? {
        return mergedFrom
    }

    fun setMergedFrom(tile: Array<Tile>?) {
        mergedFrom = tile
    }
}