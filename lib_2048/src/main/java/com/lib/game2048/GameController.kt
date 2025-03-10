package com.lib.game2048

import android.content.Context
import android.widget.Toast
import com.lib.game2048.entity.AnimationGrid
import com.lib.game2048.entity.Cell
import com.lib.game2048.entity.Grid
import com.lib.game2048.entity.Tile
import com.lib.game2048.ui.GameView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class GameController(private val context: Context, private val gameView: GameView) {

    companion object {
        const val SPAWN_ANIMATION = -1
        const val MOVE_ANIMATION = 0
        const val MERGE_ANIMATION = 1
        const val FADE_GLOBAL_ANIMATION = 0
        const val MOVE_ANIMATION_TIME: Long = GameView.BASE_ANIMATION_TIME
        const val SPAWN_ANIMATION_TIME: Long = GameView.BASE_ANIMATION_TIME
        const val NOTIFICATION_ANIMATION_TIME: Long = GameView.BASE_ANIMATION_TIME * 5
        const val NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME
        private const val GAME_LOST = -1
        private const val GAME_NORMAL = 0

        private const val KEY_HIGH_SCORE_ = "KEY_HIGH_SCORE_" //最高分
        private const val KEY_SCORE_ = "KEY_SCORE_"
        private const val KEY_GAME_STATE_ = "KEY_GAME_STATE_"
        private const val KEY_UNDO_TIMES_ = "KEY_UNDO_TIMES_" //撤回次数

        fun getHighScore(context: Context, num: Int): Long {
            return GameSetting.getSharedPreferences(context).getLong(KEY_HIGH_SCORE_ + "$num", 0)
        }

        init {
            System.loadLibrary("2048")
        }

        private external fun nativeGetBestMove(grid: Array<IntArray>): Int
        private external fun nativeSetMaxDepth(depth: Int)
    }

    interface OnScoreUpdatedListener {
        fun onScoreUpdated(score: Long)
        fun onHighScoreUpdated(score: Long)
        fun onUndoTimesUpdated(times: Int)

        fun onAiVisibleUpdated()

        fun onCheatVisibleUpdated()
    }

    private var onScoreUpdatedListener: OnScoreUpdatedListener? = null

    fun setOnScoreUpdatedListener(listener: OnScoreUpdatedListener) {
        onScoreUpdatedListener = listener
    }

    var isAIRunning = false
        private set

    private var squareNum = 4
    lateinit var grid: Grid
    lateinit var aGrid: AnimationGrid

    private var gameState = 0
    private var score: Long = 0
    private var highScore: Long = 0
    private val lastScore: MutableList<Long> = ArrayList()
    private var bufferScore: Long = 0
    private var timeMoved = 0
    private var hasMoved = false
    var undoTimes = 3
        private set

    private val soundPoolPlayer = SoundPoolPlayer(context, R.raw.sound)

    private var aiJob: Job? = null
    private val aiScope: CoroutineScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext
            get() = SupervisorJob() + Dispatchers.Main
    }

    init {
        GameSetting.getInstance().init(context, this)
        newGame()
    }

    fun newGame(num: Int = GameSetting.getInstance().squareNum) {
        stopAi()

        squareNum = num

        grid = Grid(squareNum, squareNum)
        aGrid = AnimationGrid(squareNum, squareNum)

        prepareUndoState()
        saveUndoState()
        grid.clearGrid()
        grid.clearUndoList()

        score = 0
        gameState = GAME_NORMAL

        addStartTiles()

        gameView.refreshLastTime = true
        gameView.updateLayout(squareNum)
        lastScore.clear()
        timeMoved = 0
        hasMoved = false

        onScoreUpdatedListener?.onScoreUpdated(score)
        resetUndoTimes()
    }

    private fun addStartTiles(num: Int = 2) {
        for (xx in 0 until num) {
            addRandomTile()
        }
    }

    private fun addRandomTile() {
        if (grid.randomAvailableCell() != null) {
            if (grid.isCellsAvailable()) {
                val value = if (Math.random() < GameSetting.getInstance().pog2 / 100f) 2 else 4
                val tile = Tile(grid.randomAvailableCell()!!, value)
                spawnTile(tile)
            }
        }
    }

    private fun spawnTile(tile: Tile) {
        grid.insertTile(tile)
        aGrid.startAnimation(tile.x, tile.y, SPAWN_ANIMATION, SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null)
    }

    private fun prepareTiles() {
        for (array in grid.field) {
            for (tile in array) {
                if (grid.isCellOccupied(tile)) {
                    tile?.setMergedFrom(null)
                }
            }
        }
    }

    private fun moveTile(tile: Tile, cell: Cell) {
        grid.field[tile.x][tile.y] = null
        grid.field[cell.x][cell.y] = tile
        tile.updatePosition(cell)
    }

    private fun saveUndoState() {
        grid.saveTiles()
        lastScore.add(bufferScore)
        timeMoved++
    }

    // cheat remove 2
    fun cheat() {
        var tile: Tile?
        prepareUndoState()
        for (cell in grid.getNotAvailableCells()) {
            tile = grid.getCellContent(cell)
            if (tile != null && tile.getValue() == 2) {
                grid.removeTile(tile)
                gameState = GAME_NORMAL
            }
        }
        if (grid.getNotAvailableCells().size == 0) {
            addStartTiles()
        }
        saveUndoState()
        gameView.resyncTime()
        gameView.invalidate()
    }

    private fun prepareUndoState() {
        grid.prepareSaveTiles()
        bufferScore = score
    }

    fun revertUndoState() {
        if (timeMoved > 0 &&
            (!GameSetting.getInstance().undoOnce || hasMoved)
        ) {
            hasMoved = false
            aGrid.cancelAnimations()
            grid.revertTiles()
            timeMoved--
            score = lastScore[lastScore.size - 1]
            lastScore.removeAt(lastScore.size - 1)
            gameState = GAME_NORMAL
            gameView.refreshLastTime = true
            gameView.invalidate()

            onScoreUpdatedListener?.onScoreUpdated(score)
            if (undoTimes > 0) {
                undoTimes--
                onScoreUpdatedListener?.onUndoTimesUpdated(undoTimes)
            }
        }
    }

    /**
     * 重置撤回次数
     */
    fun resetUndoTimes() {
        undoTimes = 3
        onScoreUpdatedListener?.onUndoTimesUpdated(undoTimes)
    }

    fun gameLost(): Boolean {
        return gameState == GAME_LOST
    }

    fun isGameActive(): Boolean = !gameLost()

    fun move(direction: Int) {
        aGrid.cancelAnimations()
        // 0: up, 1: right, 2: down, 3: left
        if (!isGameActive()) {
            return
        }
        prepareUndoState()
        val vector: Cell = getVector(direction)
        val traversalsX = buildTraversalsX(vector)
        val traversalsY = buildTraversalsY(vector)
        var moved = false
        prepareTiles()
        for (xx in traversalsX) {
            for (yy in traversalsY) {
                val cell = Cell(xx, yy)
                val tile: Tile? = grid.getCellContent(cell)
                if (tile != null) {
                    val positions: Array<Cell> = findFarthestPosition(cell, vector)
                    val next: Tile? = grid.getCellContent(positions[1])
                    if (next != null &&
                        next.getValue() == tile.getValue() &&
                        next.getMergedFrom() == null
                    ) {
                        val merged = Tile(positions[1], tile.getValue() * 2)
                        val temp: Array<Tile> = arrayOf(tile, next)
                        merged.setMergedFrom(temp)
                        grid.insertTile(merged)
                        grid.removeTile(tile)

                        // Converge the two tiles' positions
                        tile.updatePosition(positions[1])
                        val extras = intArrayOf(xx, yy)
                        aGrid.startAnimation(
                            merged.x,
                            merged.y,
                            MOVE_ANIMATION,
                            MOVE_ANIMATION_TIME,
                            0,
                            extras
                        ) // Direction: 0 = MOVING MERGED
                        aGrid.startAnimation(merged.x, merged.y, MERGE_ANIMATION, SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null)

                        // Update the score
                        score += merged.getValue()
                        highScore = Math.max(score, highScore)

                        // The mighty 2048 tile
                    } else {
                        moveTile(tile, positions[0])
                        val extras = intArrayOf(xx, yy, 0)
                        aGrid.startAnimation(
                            positions[0].x,
                            positions[0].y,
                            MOVE_ANIMATION,
                            MOVE_ANIMATION_TIME,
                            0,
                            extras
                        ) // Direction: 1 = MOVING NO MERGE
                    }
                    if (!positionsEqual(cell, tile)) {
                        moved = true
                    }
                }
            }
        }
        if (moved) {
            hasMoved = true
            if (GameSetting.getInstance().isSoundOn) {
                soundPoolPlayer.replay()
            }
//            if (!isAIRunning)
            saveUndoState()
            for (i in 0 until GameSetting.getInstance().addTileNum) {
                addRandomTile()
            }
            checkLose()
        }
        gameView.resyncTime()
        gameView.invalidate()

        onScoreUpdatedListener?.onScoreUpdated(score)
        onScoreUpdatedListener?.onHighScoreUpdated(highScore)
    }

    private fun checkLose() {
        if (!movesAvailable()) {
            gameState = GAME_LOST
            endGame()
        }
    }

    private fun endGame() {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null)
        if (score >= highScore) {
            highScore = score
            GameSetting.getSharedPreferences(context).edit()
                .putLong(KEY_HIGH_SCORE_ + "$squareNum", highScore)
                .apply()
            onScoreUpdatedListener?.onHighScoreUpdated(highScore)
        }
    }

    private fun getVector(direction: Int): Cell {
        val map: Array<Cell> = arrayOf(
            Cell(0, -1),  // up
            Cell(1, 0),  // right
            Cell(0, 1),  // down
            Cell(-1, 0) // left
        )
        return map[direction]
    }

    private fun buildTraversalsX(vector: Cell): List<Int> {
        val traversals: MutableList<Int> = ArrayList()
        for (xx in 0 until squareNum) {
            traversals.add(xx)
        }
        if (vector.x == 1) {
            traversals.reverse()
        }
        return traversals
    }

    private fun buildTraversalsY(vector: Cell): List<Int> {
        val traversals: MutableList<Int> = ArrayList()
        for (xx in 0 until squareNum) {
            traversals.add(xx)
        }
        if (vector.y == 1) {
            traversals.reverse()
        }
        return traversals
    }

    private fun findFarthestPosition(cell: Cell, vector: Cell): Array<Cell> {
        var previous: Cell
        var nextCell = Cell(cell.x, cell.y)
        do {
            previous = nextCell
            nextCell = Cell(previous.x + vector.x, previous.y + vector.y)
        } while (
            grid.isCellWithinBounds(nextCell) && grid.isCellAvailable(nextCell)
        )
        return arrayOf(previous, nextCell)
    }

    private fun movesAvailable(): Boolean {
        return grid.isCellsAvailable() || tileMatchesAvailable()
    }

    private fun tileMatchesAvailable(): Boolean {
        var tile: Tile?
        for (xx in 0 until squareNum) {
            for (yy in 0 until squareNum) {
                tile = grid.getCellContent(Cell(xx, yy))
                if (tile != null) {
                    for (direction in 0..3) {
                        val vector: Cell = getVector(direction)
                        val cell = Cell(xx + vector.x, yy + vector.y)
                        val other: Tile? = grid.getCellContent(cell)
                        if (other != null && other.getValue() == tile.getValue()) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun positionsEqual(first: Cell, second: Cell): Boolean {
        return first.x == second.x && first.y == second.y
    }

    fun runAi() {
        if (gameLost()) return //游戏结束
        if (squareNum < 4) return //4x4以上才能使用ai

        isAIRunning = true
        Toast.makeText(context, R.string.press_anywhere_to_stop, Toast.LENGTH_SHORT).show()

        val intervalTime = GameSetting.getInstance().intervalTime//考虑时间
        nativeSetMaxDepth(GameSetting.getInstance().depthLevel)//考虑深度

        aiJob?.cancel()
        aiJob = aiScope.launch(Dispatchers.Default) {
            while (isAIRunning) {
                if (!isActive) return@launch
                if (isGameActive()) {
                    val direction = nativeGetBestMove(grid.getCellMatrix())
                    withContext(Dispatchers.Main) {
                        move(direction)
                    }
                    delay(intervalTime) //思考时间（延迟时间）
                } else {
                    withContext(Dispatchers.Main) {
                        stopAi()
                    }
                    break
                }
            }
        }
    }

    fun stopAi() {
        if (isAIRunning) {
            isAIRunning = false
            Toast.makeText(context, R.string.stopped, Toast.LENGTH_SHORT).show()
            aiJob?.cancel()
        }
    }

    fun save() {
        if (score > 0) {//有分数才保存
            val settings = GameSetting.getSharedPreferences(context)
            val editor = settings.edit()
            val field: Array<Array<Tile?>> = grid.field
            for (xx in field.indices) {
                for (yy in field[0].indices) {
                    if (field[xx][yy] != null) {
                        editor.putInt("Cell$squareNum($xx,$yy)", field[xx][yy]!!.getValue())
                    } else {
                        editor.putInt("Cell$squareNum($xx,$yy)", 0)
                    }
                }
            }
            editor.putLong(KEY_SCORE_ + "$squareNum", score)
            editor.putInt(KEY_GAME_STATE_ + "$squareNum", gameState)
            editor.putLong(KEY_HIGH_SCORE_ + "$squareNum", highScore)
            editor.putInt(KEY_UNDO_TIMES_ + "$squareNum", undoTimes)
            editor.apply()
        }
    }

    fun load() {
        aGrid.cancelAnimations()

        val settings = GameSetting.getSharedPreferences(context)

        score = settings.getLong(KEY_SCORE_ + "$squareNum", 0)
        onScoreUpdatedListener?.onScoreUpdated(score)

        if (score > 0) { //有分数才加载
            lastScore.add(score)
            gameState = settings.getInt(KEY_GAME_STATE_ + "$squareNum", GAME_NORMAL)
            for (xx in 0 until grid.field.size) {
                for (yy in 0 until grid.field.get(0).size) {
                    val value = settings.getInt("Cell$squareNum($xx,$yy)", -1)
                    if (value > 0) {
                        grid.field[xx][yy] = Tile(xx, yy, value)
                    } else if (value == 0) {
                        grid.field[xx][yy] = null
                    }
                }
            }
        }

        highScore = settings.getLong(KEY_HIGH_SCORE_ + "$squareNum", 0)
        onScoreUpdatedListener?.onHighScoreUpdated(highScore)

        undoTimes = settings.getInt(KEY_UNDO_TIMES_ + "$squareNum", 3)
        onScoreUpdatedListener?.onUndoTimesUpdated(undoTimes)
    }

    fun updateUndoTimes() {
        onScoreUpdatedListener?.onUndoTimesUpdated(undoTimes)
    }

    fun updateAiVisible() {
        onScoreUpdatedListener?.onAiVisibleUpdated()
    }

    fun updateCheatVisible() {
        onScoreUpdatedListener?.onCheatVisibleUpdated()
    }
}