import kotlin.math.abs
import kotlin.math.min

class Game2048AI {

    companion object {
        const val UP = 0
        const val DOWN = 1
        const val LEFT = 2
        const val RIGHT = 3

        // 4 bit/格，16 格 = 64 bit
        // 格子编号：
        //  0  1  2  3
        //  4  5  6  7
        //  8  9 10 11
        // 12 13 14 15

        // 蛇形权重（用于评估），按格子编号 0..15
        // 引导大数字固定在某一角（这里以左上角为目标）
        private val SNAKE_WEIGHTS = intArrayOf(
            15, 14, 13, 12,
            8, 9, 10, 11,
            7, 6, 5, 4,
            0, 1, 2, 3
        )

        // 预计算表
        private val rowLeftResult = IntArray(65536)
        private val rowRightResult = IntArray(65536)
        private val rowLeftScore = IntArray(65536)
        private val rowRightScore = IntArray(65536)

        init {
            buildTables()
        }

        /** 构建行移动预计算表 */
        private fun buildTables() {
            for (row in 0 until 65536) {
                val c0 = (row) and 0xF
                val c1 = (row shr 4) and 0xF
                val c2 = (row shr 8) and 0xF
                val c3 = (row shr 12) and 0xF
                val cells = intArrayOf(c0, c1, c2, c3)

                // 向左合并
                val (leftMerged, leftScore) = mergeRow(cells)
                rowLeftResult[row] = leftMerged[0] or
                        (leftMerged[1] shl 4) or
                        (leftMerged[2] shl 8) or
                        (leftMerged[3] shl 12)
                rowLeftScore[row] = leftScore

                // 向右合并 = 反转后向左合并，再反转回来
                val reversed = intArrayOf(c3, c2, c1, c0)
                val (rightMerged, rightScore) = mergeRow(reversed)
                rowRightResult[row] = rightMerged[3] or
                        (rightMerged[2] shl 4) or
                        (rightMerged[1] shl 8) or
                        (rightMerged[0] shl 12)
                rowRightScore[row] = rightScore
            }
        }

        /** 合并一行（向左），返回合并后的格子和得分 */
        private fun mergeRow(cells: IntArray): Pair<IntArray, Int> {
            val filtered = IntArray(4)
            var n = 0
            for (v in cells) if (v != 0) filtered[n++] = v

            val merged = IntArray(4)
            var idx = 0
            var score = 0
            var i = 0
            while (i < n) {
                if (i + 1 < n && filtered[i] == filtered[i + 1]) {
                    val newVal = filtered[i] + 1   // 指数 +1
                    merged[idx++] = newVal
                    // 得分 = 实际数值，即 2^newVal
                    score += 1 shl newVal
                    i += 2
                } else {
                    merged[idx++] = filtered[i]
                    i++
                }
            }
            return Pair(merged, score)
        }
    }

    // ============ 成员变量 ============
    private var addTileNum = 1
    private var pog2 = 0.9f
    private val transposition = HashMap<Long, Float>()

    /**
     * 获取最佳移动方向
     * @param tiles n*n 的格子，0 表示空格子（这里只支持 4x4）
     * @param addTileNum 每次移动新增方块数量
     * @param pog2 生成价值为 2 的格子概率
     * @return 最佳移动方向（0=上，1=下，2=左，3=右）
     */
    fun getBestMove(
        tiles: List<List<Int>>,
        addTileNum: Int = 1,
        pog2: Float = 0.9f,
        maxDepth: Int = 6
    ): Int {
//        require(tiles.size == 4 && tiles.all { it.size == 4 }) {
//            "Bitboard 版仅支持 4x4 棋盘"
//        }

        this.addTileNum = addTileNum.coerceAtLeast(1)
        this.pog2 = pog2
        this.transposition.clear()

        val board = toBitboard(tiles)

        var bestMove = -1
        var bestScore = Float.NEGATIVE_INFINITY
        var stableCount = 0

        for (depth in 2..maxDepth) {
            var curBestMove = -1
            var curBestScore = Float.NEGATIVE_INFINITY

            for (dir in 0..3) {
                val (nb, moved) = move(board, dir)
                if (!moved) continue

                val score = expectimax(
                    board = nb,
                    depth = depth,
                    isPlayerTurn = false,
                    pendingTiles = this.addTileNum
                )

                if (score > curBestScore) {
                    curBestScore = score
                    curBestMove = dir
                }
            }

            if (curBestMove == -1) break

            if (curBestScore > bestScore) {
                bestScore = curBestScore
                bestMove = curBestMove
            }

            if (curBestMove == bestMove) {
                stableCount++
                if (stableCount >= 2) break
            } else {
                stableCount = 0
            }
        }

        return if (bestMove == -1) 0 else bestMove
    }

    /**
     * Expectimax 搜索
     * @param board 当前位棋盘（每格 4 bit 存指数）
     * @param depth 剩余深度（只在玩家回合递减）
     * @param isPlayerTurn true=玩家回合（MAX），false=随机回合（EXPECT）
     * @param pendingTiles 随机回合还需放置的方块数
     */
    private fun expectimax(
        board: Long,
        depth: Int,
        isPlayerTurn: Boolean,
        pendingTiles: Int
    ): Float {
        if (depth == 0 || isGameOver(board)) {
            return evaluate(board)
        }

        // 置换表 key：board 本身 + 回合 + 深度 + 待放数
        // 用字段拼接，避免魔数
        val key = board xor
                (depth.toLong() shl 60) xor
                ((pendingTiles.toLong() and 0xF) shl 56) xor
                (if (isPlayerTurn) (1L shl 55) else 0L)

        transposition[key]?.let { return it }

        val result = if (isPlayerTurn) {
            // ===== 玩家回合：取最大 =====
            var best = Float.NEGATIVE_INFINITY
            for (dir in 0..3) {
                val (nb, moved) = move(board, dir)
                if (!moved) continue
                val s = expectimax(nb, depth - 1, false, addTileNum)
                if (s > best) best = s
            }
            if (best == Float.NEGATIVE_INFINITY) evaluate(board) else best
        } else {
            // ===== 随机回合：取期望 =====
            val emptyCells = getEmptyCells(board)
            if (emptyCells.isEmpty()) return evaluate(board)

            // 空格少时全遍历；多时采样前 6 个
            val cells = if (emptyCells.size <= 6) {
                emptyCells
            } else {
                emptyCells.sortedByDescending { neighborWeight(board, it) }
                    .take(6)
                    .toIntArray()
            }

            var expected = 0f
            val p2 = pog2
            val p4 = 1f - pog2
            val invSize = 1f / cells.size

            for (cellIdx in cells) {
                // 生成 2（指数 1）
                val b2 = setCell(board, cellIdx, 1)
                val s2 = if (pendingTiles > 1) {
                    expectimax(b2, depth, false, pendingTiles - 1)
                } else {
                    expectimax(b2, depth - 1, true, 0)
                }
                expected += p2 * s2

                // 生成 4（指数 2）
                val b4 = setCell(board, cellIdx, 2)
                val s4 = if (pendingTiles > 1) {
                    expectimax(b4, depth, false, pendingTiles - 1)
                } else {
                    expectimax(b4, depth - 1, true, 0)
                }
                expected += p4 * s4
            }
            expected * invSize
        }

        transposition[key] = result
        return result
    }

    // ============ Bitboard 基础操作 ============

    /** 从 List<List<Int>> 转成 bitboard（存指数） */
    private fun toBitboard(tiles: List<List<Int>>): Long {
        var b = 0L
        var idx = 0
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val v = tiles[i][j]
                val e = if (v == 0) 0 else (31 - Integer.numberOfLeadingZeros(v))
                b = b or (e.toLong() shl (idx * 4))
                idx++
            }
        }
        return b
    }

    /** 读取第 i 格（0..15） */
    private inline fun getCell(board: Long, i: Int): Int =
        ((board shr (i * 4)) and 0xF).toInt()

    /** 设置第 i 格 */
    private inline fun setCell(board: Long, i: Int, value: Int): Long {
        val shift = i * 4
        return (board and (0xFL shl shift).inv()) or (value.toLong() shl shift)
    }

    /** 获取所有空格编号 */
    private fun getEmptyCells(board: Long): IntArray {
        var count = 0
        var tmp = board
        // 先数一遍
        for (i in 0 until 16) {
            if (((board shr (i * 4)) and 0xF) == 0L) count++
        }
        val res = IntArray(count)
        var idx = 0
        for (i in 0 until 16) {
            if (((board shr (i * 4)) and 0xF) == 0L) res[idx++] = i
        }
        return res
    }

    /** 邻居数值之和（用于采样优先级） */
    private fun neighborWeight(board: Long, cellIdx: Int): Int {
        val r = cellIdx / 4
        val c = cellIdx % 4
        var w = 0
        if (r > 0) w += 1 shl getCell(board, cellIdx - 4)
        if (r < 3) w += 1 shl getCell(board, cellIdx + 4)
        if (c > 0) w += 1 shl getCell(board, cellIdx - 1)
        if (c < 3) w += 1 shl getCell(board, cellIdx + 1)
        return w
    }

    // ============ 移动（查表实现） ============

    /** 执行移动，返回 (新棋盘, 是否移动) */
    private fun move(board: Long, dir: Int): Pair<Long, Boolean> {
        return when (dir) {
            LEFT -> moveLeft(board)
            RIGHT -> moveRight(board)
            UP -> moveUp(board)
            DOWN -> moveDown(board)
            else -> Pair(board, false)
        }
    }

    private fun moveLeft(board: Long): Pair<Long, Boolean> {
        var result = 0L
        for (i in 0 until 4) {
            val row = ((board shr (i * 16)) and 0xFFFF).toInt()
            val newRow = rowLeftResult[row]
            result = result or (newRow.toLong() shl (i * 16))
        }
        return Pair(result, result != board)
    }

    private fun moveRight(board: Long): Pair<Long, Boolean> {
        var result = 0L
        for (i in 0 until 4) {
            val row = ((board shr (i * 16)) and 0xFFFF).toInt()
            val newRow = rowRightResult[row]
            result = result or (newRow.toLong() shl (i * 16))
        }
        return Pair(result, result != board)
    }

    /**
     * 上移 = 转置后用左移表，再转置回来
     * 转置：把 (r,c) 和 (c,r) 交换
     */
    private fun moveUp(board: Long): Pair<Long, Boolean> {
        val transposed = transpose(board)
        val moved = moveLeft(transposed)
        val result = transpose(moved.first)
        return Pair(result, result != board)
    }

    private fun moveDown(board: Long): Pair<Long, Boolean> {
        val transposed = transpose(board)
        val moved = moveRight(transposed)
        val result = transpose(moved.first)
        return Pair(result, result != board)
    }

    /** 转置 4x4 bitboard */
    private fun transpose(board: Long): Long {
        // 用位运算转置 4x4 的 4-bit 元素矩阵
        // 分三步：对角线交换，用掩码分组
        // 简化写法：逐格读取再写入（16 次操作，仍很快）
        var result = 0L
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val v = getCell(board, i * 4 + j)
                result = setCell(result, j * 4 + i, v)
            }
        }
        return result
    }

    // ============ 评估函数 ============

    private fun evaluate(board: Long): Float {
        val empty = countEmpty(board)
        val mono = monotonicity(board)
        val smooth = smoothness(board)
        val merge = mergePotential(board)
        val snake = snakeScore(board)
        val maxLg = maxTile(board)

        val emptyW = if (empty > 8) 300f else 500f
        val monoW = 80f
        val smoothW = 40f
        val mergeW = 60f
        val snakeW = 25f
        val maxW = 15f

        return emptyW * empty +
                monoW * mono +
                smoothW * smooth +
                mergeW * merge +
                snakeW * snake +
                maxW * maxLg
    }

    private fun countEmpty(board: Long): Float {
        var count = 0
        for (i in 0 until 16) {
            if (((board shr (i * 4)) and 0xF) == 0L) count++
        }
        return count.toFloat()
    }

    private fun monotonicity(board: Long): Float {
        var score = 0f
        // 行
        for (i in 0 until 4) {
            var inc = 0f;
            var dec = 0f
            for (j in 0 until 3) {
                val a = getCell(board, i * 4 + j).toFloat()
                val b = getCell(board, i * 4 + j + 1).toFloat()
                if (a > b) dec += a - b else inc += b - a
            }
            score += min(inc, dec)
        }
        // 列
        for (j in 0 until 4) {
            var inc = 0f;
            var dec = 0f
            for (i in 0 until 3) {
                val a = getCell(board, i * 4 + j).toFloat()
                val b = getCell(board, (i + 1) * 4 + j).toFloat()
                if (a > b) dec += a - b else inc += b - a
            }
            score += min(inc, dec)
        }
        return score
    }

    private fun smoothness(board: Long): Float {
        var s = 0f
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val v = getCell(board, i * 4 + j)
                if (v == 0) continue
                if (j < 3) {
                    val r = getCell(board, i * 4 + j + 1)
                    if (r != 0) s -= abs(v - r).toFloat()
                }
                if (i < 3) {
                    val d = getCell(board, (i + 1) * 4 + j)
                    if (d != 0) s -= abs(v - d).toFloat()
                }
            }
        }
        return s
    }

    private fun mergePotential(board: Long): Float {
        var s = 0f
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val v = getCell(board, i * 4 + j)
                if (v == 0) continue
                if (j < 3 && getCell(board, i * 4 + j + 1) == v) s += v
                if (i < 3 && getCell(board, (i + 1) * 4 + j) == v) s += v
            }
        }
        return s
    }

    private fun snakeScore(board: Long): Float {
        var best = Float.NEGATIVE_INFINITY
        // 4 种翻转组合
        for (flipRow in 0..1) {
            for (flipCol in 0..1) {
                var s = 0f
                for (i in 0 until 4) {
                    for (j in 0 until 4) {
                        val r = if (flipRow == 0) i else 3 - i
                        val c = if (flipCol == 0) j else 3 - j
                        val v = getCell(board, r * 4 + c)
                        if (v != 0) {
                            s += v.toFloat() * SNAKE_WEIGHTS[i * 4 + j]
                        }
                    }
                }
                if (s > best) best = s
            }
        }
        return best
    }

    private fun maxTile(board: Long): Float {
        var maxE = 0
        for (i in 0 until 16) {
            val e = getCell(board, i)
            if (e > maxE) maxE = e
        }
        return maxE.toFloat()
    }

    // ============ 游戏结束判断 ============

    private fun isGameOver(board: Long): Boolean {
        // 还有空格
        for (i in 0 until 16) {
            if (((board shr (i * 4)) and 0xF) == 0L) return false
        }
        // 检查相邻是否可合并
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                val v = getCell(board, i * 4 + j)
                if (j < 3 && getCell(board, i * 4 + j + 1) == v) return false
                if (i < 3 && getCell(board, (i + 1) * 4 + j) == v) return false
            }
        }
        return true
    }
}