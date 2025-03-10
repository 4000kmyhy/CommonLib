package com.lib.game2048.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.lib.game2048.GameController
import com.lib.game2048.R
import com.lib.game2048.entity.AnimationCell
import com.lib.game2048.entity.Tile

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val BASE_ANIMATION_TIME: Long = 100000000
        const val MERGING_ACCELERATION = -0.5f
        const val INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4
    }

    private val paint = Paint()
    private val numCellTypes = 18

    private var squareNum = 4
    private var cellSize = 0f
    private var gridWidth = 0f
    private var cellTextSize = 0f
    private var gameOverTextSize = 0f
    private val loseText: String = "Game Over!"
    private val textBlack: Int
    private val textWhite: Int

    private val backgroundRectangle: Drawable?
    private val cellRectangle = arrayOfNulls<Drawable>(numCellTypes)
    private val lightUpRectangle: Drawable?

    private var lastFPSTime = System.nanoTime()
    private var currentTime = System.nanoTime()
    var refreshLastTime = true

    val controller = GameController(context, this)

    init {
        backgroundRectangle = ContextCompat.getDrawable(context, R.drawable.background_rectangle)
        cellRectangle[0] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle)
        cellRectangle[1] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_2)
        cellRectangle[2] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_4)
        cellRectangle[3] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_8)
        cellRectangle[4] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_16)
        cellRectangle[5] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_32)
        cellRectangle[6] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_64)
        cellRectangle[7] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_128)
        cellRectangle[8] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_256)
        cellRectangle[9] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_512)
        cellRectangle[10] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_1024)
        cellRectangle[11] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_2048)
        cellRectangle[12] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_4096)
        cellRectangle[13] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_8192)
        cellRectangle[14] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_16384)
        cellRectangle[15] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_32768)
        cellRectangle[16] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_65536)
        cellRectangle[17] = ContextCompat.getDrawable(context, R.drawable.cell_rectangle_131072)
        lightUpRectangle = ContextCompat.getDrawable(context, R.drawable.light_up_rectangle)

        textWhite = ContextCompat.getColor(context, R.color.text_white_2048)
        textBlack = ContextCompat.getColor(context, R.color.text_black_2048)

        paint.isAntiAlias = true
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val size = Math.min(widthSize, heightSize)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)
        updateLayout(squareNum)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //背景
        drawDrawable(canvas, backgroundRectangle, 0f, 0f, width.toFloat(), height.toFloat())
        //空方块
        drawBackgroundGrid(canvas)
        //有值方块
        drawCells(canvas)
        //游戏结束
        drawEndGameState(canvas)

        // Refresh the screen if there is still an animation running
        if (controller.aGrid.isAnimationActive()) {
            invalidate()
            tick()
            // Refresh one last time on game end.
        } else if (!controller.isGameActive() && refreshLastTime) {
            invalidate()
            refreshLastTime = false
        }
    }

    private fun drawBackgroundGrid(canvas: Canvas) {
        // Outputting the game grid
        for (xx in 0 until squareNum) {
            for (yy in 0 until squareNum) {
                val sX = gridWidth + (cellSize + gridWidth) * xx
                val eX = sX + cellSize
                val sY = gridWidth + (cellSize + gridWidth) * yy
                val eY = sY + cellSize
                drawDrawable(canvas, cellRectangle[0], sX, sY, eX, eY)
            }
        }
    }

    private fun drawCells(canvas: Canvas) {
        try {
            // Outputting the individual cells
            for (xx in 0 until squareNum) {
                for (yy in 0 until squareNum) {
                    val sX = gridWidth + (cellSize + gridWidth) * xx
                    val eX = sX + cellSize
                    val sY = gridWidth + (cellSize + gridWidth) * yy
                    val eY = sY + cellSize
                    val currentTile: Tile? = controller.grid.getCellContent(xx, yy)
                    if (currentTile != null) {
                        // Get and represent the value of the tile
                        val value: Int = currentTile.getValue()
                        val index = log2(value)

                        // Check for any active animations
                        val aArray: ArrayList<AnimationCell> = controller.aGrid.getAnimationCell(xx, yy)
                        var animated = false
                        for (i in aArray.indices.reversed()) {
                            val aCell: AnimationCell = aArray[i]
                            // If this animation is not active, skip it
                            if (aCell.animationType == GameController.SPAWN_ANIMATION) {
                                animated = true
                            }
                            if (!aCell.isActive()) {
                                continue
                            }
                            if (aCell.animationType == GameController.SPAWN_ANIMATION) { // Spawning
                                // animation
                                val percentDone: Double = aCell.getPercentageDone()
                                val textScaleSize = percentDone.toFloat()
                                val cellScaleSize = cellSize / 2 * (1 - textScaleSize)
                                drawCell(
                                    canvas, index,
                                    sX + cellScaleSize,
                                    sY + cellScaleSize,
                                    eX - cellScaleSize,
                                    eY - cellScaleSize
                                )
                            } else if (aCell.animationType == GameController.MERGE_ANIMATION) { // Merging
                                // Animation
                                val percentDone: Double = aCell.getPercentageDone()
                                val textScaleSize =
                                    (1 + INITIAL_VELOCITY * percentDone + MERGING_ACCELERATION * percentDone * percentDone / 2).toFloat()
                                val cellScaleSize = cellSize / 2 * (1 - textScaleSize)
                                drawCell(
                                    canvas, index,
                                    sX + cellScaleSize,
                                    sY + cellScaleSize,
                                    eX - cellScaleSize,
                                    eY - cellScaleSize
                                )
                            } else if (aCell.animationType == GameController.MOVE_ANIMATION) { // Moving
                                // animation
                                val percentDone: Double = aCell.getPercentageDone()
                                var tempIndex = index
                                if (aArray.size >= 2) {
                                    tempIndex = tempIndex - 1
                                }
                                val previousX: Int = aCell.extras?.get(0) ?: 0
                                val previousY: Int = aCell.extras?.get(1) ?: 0
                                val currentX: Int = currentTile.x
                                val currentY: Int = currentTile.y
                                val dX = (currentX - previousX) * (cellSize + gridWidth) * (percentDone - 1)
                                val dY = (currentY - previousY) * (cellSize + gridWidth) * (percentDone - 1)
                                drawCell(
                                    canvas, tempIndex,
                                    (sX + dX).toFloat(),
                                    (sY + dY).toFloat(),
                                    (eX + dX).toFloat(),
                                    (eY + dY).toFloat()
                                )
                            }
                            animated = true
                        }

                        // No active animations? Just draw the cell
                        if (!animated) {
                            drawCell(canvas, index, sX, sY, eX, eY)
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
        }
    }

    private fun drawCell(
        canvas: Canvas,
        index: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        if (index in cellRectangle.indices) {
            drawDrawable(canvas, cellRectangle[index], left, top, right, bottom)
        } else {
            drawDrawable(canvas, cellRectangle[cellRectangle.size - 1], left, top, right, bottom)
        }
        //text
        val width = right - left
        val length = bottom - top
        val middleX = width / 2f
        val middleY = length / 2f
        paint.color = if (index >= 3) textWhite else textBlack
        paint.textSize = if (index >= 14) cellTextSize * 0.8f else cellTextSize
        val valueStr = if (index >= 17) "2^$index" else "${Math.pow(2.0, index.toDouble()).toInt()}"
        canvas.drawText(valueStr, left + middleX, top + middleY - centerText(), paint)
    }

    private fun drawEndGameState(canvas: Canvas) {
        if (controller.gameLost()) {
            var alphaChange = 1.0
            val globalAnimation = controller.aGrid.globalAnimation
            for (animation in globalAnimation) {
                if (animation.animationType == GameController.FADE_GLOBAL_ANIMATION) {
                    alphaChange = animation.getPercentageDone()
                }
            }
            lightUpRectangle?.alpha = (255 * alphaChange).toInt()
            drawDrawable(canvas, lightUpRectangle, 0f, 0f, width.toFloat(), height.toFloat())
            paint.color = textBlack
            paint.alpha = (255 * alphaChange).toInt()
            paint.textSize = gameOverTextSize
            canvas.drawText(loseText, 0.5f * width, 0.5f * height - centerText(), paint)
        }
    }

    private fun drawDrawable(
        canvas: Canvas,
        draw: Drawable?,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        draw?.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        draw?.draw(canvas)
    }

    private fun tick() {
        currentTime = System.nanoTime()
        controller.aGrid.tickAll(currentTime - lastFPSTime)
        lastFPSTime = currentTime
    }

    fun resyncTime() {
        lastFPSTime = System.nanoTime()
    }

    fun updateLayout(num: Int) {
        squareNum = num
        if (width == 0 || height == 0) return

        val size = Math.min(width, height)
        //n个方块 +（n+1）条边
        cellSize = 1f * size / (squareNum + (squareNum + 1) / 9f)
        gridWidth = cellSize / 9

        paint.textSize = cellSize
        cellTextSize = paint.textSize * 0.9f * cellSize / paint.measureText("0000")
        gameOverTextSize = paint.textSize * 0.8f * width / paint.measureText(loseText)

        resyncTime()
        invalidate()
    }

    private fun centerText(): Float {
        return (paint.descent() + paint.ascent()) / 2f
    }

    private fun log2(n: Int): Int {
        return if (n <= 0) 0 else 31 - Integer.numberOfLeadingZeros(n)
    }
}