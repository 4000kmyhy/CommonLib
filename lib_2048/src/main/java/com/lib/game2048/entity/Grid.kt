package com.lib.game2048.entity

import com.lib.game2048.ui.GameView

class Grid(sizeX: Int, sizeY: Int) {

    var field: Array<Array<Tile?>>
        private set
    private val undoList = ArrayList<Array<Array<Tile?>>>()
    private val bufferField: Array<Array<Tile?>>

    init {
        field = Array(sizeX) { arrayOfNulls(sizeY) }
        bufferField = Array(sizeX) { arrayOfNulls(sizeY) }
        clearGrid()
        clearUndoList()
    }

    fun getCellMatrix(): Array<LongArray> {
        val tmp = Array(field.size) { LongArray(field[0].size) }
        for (xx in field.indices) {
            for (yy in field[0].indices) {
                tmp[xx][yy] = if (field[xx][yy] == null) 0 else field[xx][yy]!!.getValue()
            }
        }
        return tmp
    }

    fun getCellLog2(): Array<IntArray> {
        val tmp = Array(field.size) { IntArray(field[0].size) }
        for (xx in field.indices) {
            for (yy in field[0].indices) {
                tmp[xx][yy] =
                    if (field[xx][yy] == null) 0 else GameView.log2(field[xx][yy]!!.getValue())
            }
        }
        return tmp
    }

    fun randomAvailableCell(): Cell? {
        val availableCells = getAvailableCells()
        return if (availableCells.isNotEmpty()) {
            availableCells.random()
        } else null
    }

    fun getAvailableCells(): ArrayList<Cell> {
        val availableCells = ArrayList<Cell>()
        for (xx in field.indices) {
            for (yy in field[0].indices) {
                if (field[xx].get(yy) == null) {
                    availableCells.add(Cell(xx, yy))
                }
            }
        }
        return availableCells
    }

    fun getNotAvailableCells(): ArrayList<Cell> {
        val notAvailableCells = ArrayList<Cell>()
        for (xx in field.indices) {
            for (yy in field[0].indices) {
                if (field[xx].get(yy) != null) {
                    notAvailableCells.add(Cell(xx, yy))
                }
            }
        }
        return notAvailableCells
    }

    fun isCellsAvailable(): Boolean = getAvailableCells().size >= 1

    fun isCellAvailable(cell: Cell?): Boolean {
        return !isCellOccupied(cell)
    }

    fun isCellOccupied(cell: Cell?): Boolean {
        return getCellContent(cell) != null
    }

    fun getCellContent(cell: Cell?): Tile? {
        return if (cell != null && isCellWithinBounds(cell)) {
            field[cell.x][cell.y]
        } else {
            null
        }
    }

    fun getCellContent(x: Int, y: Int): Tile? {
        return if (isCellWithinBounds(x, y)) {
            field[x][y]
        } else {
            null
        }
    }

    fun isCellWithinBounds(cell: Cell): Boolean {
        return 0 <= cell.x && cell.x < field.size && 0 <= cell.y && cell.y < field[0].size
    }

    fun isCellWithinBounds(x: Int, y: Int): Boolean {
        return 0 <= x && x < field.size && 0 <= y && y < field[0].size
    }

    fun insertTile(tile: Tile) {
        field[tile.x][tile.y] = tile
    }

    fun removeTile(tile: Tile) {
        field[tile.x][tile.y] = null
    }

    fun saveTiles() {
        val tmpField = Array(bufferField.size) {
            arrayOfNulls<Tile>(
                bufferField[0].size
            )
        }
        for (xx in bufferField.indices) {
            for (yy in bufferField[0].indices) {
                if (bufferField[xx][yy] == null) {
                    tmpField[xx][yy] = null
                } else {
                    tmpField[xx][yy] = Tile(xx, yy, bufferField[xx][yy]!!.getValue())
                }
            }
        }
        undoList.add(tmpField)
    }

    fun prepareSaveTiles() {
        for (xx in field.indices) {
            for (yy in field[0].indices) {
                if (field[xx][yy] == null) {
                    bufferField[xx][yy] = null
                } else {
                    bufferField[xx][yy] = Tile(
                        xx, yy,
                        field[xx][yy]!!.getValue()
                    )
                }
            }
        }
    }

    fun revertTiles() {
        if (undoList.size <= 0) return
        val tmpField = undoList[undoList.size - 1]
        for (xx in tmpField.indices) {
            for (yy in tmpField[0].indices) {
                if (tmpField[xx][yy] == null) {
                    field[xx][yy] = null
                } else {
                    field[xx][yy] = Tile(xx, yy, tmpField[xx][yy]!!.getValue())
                }
            }
        }
        undoList.removeAt(undoList.size - 1)
    }

    fun clearGrid() {
        for (xx in field.indices) {
            for (yy in field[0].indices) {
                field[xx][yy] = null
            }
        }
    }

    fun clearUndoList() {
        undoList.clear()
    }

    fun revertTiles0() {
        if (undoList.size <= 0) return
        val tmpField = undoList[0]
        for (xx in tmpField.indices) {
            for (yy in tmpField[0].indices) {
                if (tmpField[xx][yy] == null) {
                    field[xx][yy] = null
                } else {
                    field[xx][yy] = Tile(xx, yy, tmpField[xx][yy]!!.getValue())
                }
            }
        }
    }
}