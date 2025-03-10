package com.lib.game2048.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import com.lib.game2048.GameController

/**
 * desc:
 **
 * user: xujj
 * time: 2023/12/19 17:27
 **/
class GameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val SWIPE_MIN_DISTANCE = 0
        private const val SWIPE_THRESHOLD_VELOCITY = 25
        private const val MOVE_THRESHOLD = 250
        private const val RESET_STARTING = 10
    }

    private var gameController: GameController? = null

    fun bindController(controller: GameController) {
        gameController = controller
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        gameController?.let {
            if (it.isAIRunning) {
                it.stopAi()
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private var x = 0f
    private var y = 0f
    private var lastdx = 0f
    private var lastdy = 0f
    private var previousX = 0f
    private var previousY = 0f
    private var startingX = 0f
    private var startingY = 0f
    private var previousDirection = 1
    private var veryLastDirection = 1
    private var hasMoved = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gameController?.let {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.x
                    y = event.y
                    startingX = x
                    startingY = y
                    previousX = x
                    previousY = y
                    lastdx = 0f
                    lastdy = 0f
                    hasMoved = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    x = event.x
                    y = event.y
                    if (it.isGameActive()) {
                        val dx = x - previousX
                        if (Math.abs(lastdx + dx) < Math.abs(lastdx) + Math.abs(dx) &&
                            Math.abs(dx) > RESET_STARTING &&
                            Math.abs(x - startingX) > SWIPE_MIN_DISTANCE
                        ) {
                            startingX = x
                            startingY = y
                            lastdx = dx
                            previousDirection = veryLastDirection
                        }
                        if (lastdx == 0f) {
                            lastdx = dx
                        }
                        val dy = y - previousY
                        if (Math.abs(lastdy + dy) < Math.abs(lastdy) + Math.abs(dy) &&
                            Math.abs(dy) > RESET_STARTING &&
                            Math.abs(y - startingY) > SWIPE_MIN_DISTANCE
                        ) {
                            startingX = x
                            startingY = y
                            lastdy = dy
                            previousDirection = veryLastDirection
                        }
                        if (lastdy == 0f) {
                            lastdy = dy
                        }
                        if (pathMoved() > SWIPE_MIN_DISTANCE * SWIPE_MIN_DISTANCE) {
                            var moved = false
                            if ((dy >= SWIPE_THRESHOLD_VELOCITY && previousDirection == 1 || y - startingY >= MOVE_THRESHOLD)
                                && previousDirection % 2 != 0
                            ) {
                                moved = true
                                previousDirection = previousDirection * 2
                                veryLastDirection = 2
                                it.move(2)
                            } else if ((dy <= -SWIPE_THRESHOLD_VELOCITY && previousDirection == 1 || y - startingY <= -MOVE_THRESHOLD)
                                && previousDirection % 3 != 0
                            ) {
                                moved = true
                                previousDirection = previousDirection * 3
                                veryLastDirection = 3
                                it.move(0)
                            } else if ((dx >= SWIPE_THRESHOLD_VELOCITY && previousDirection == 1 || x - startingX >= MOVE_THRESHOLD)
                                && previousDirection % 5 != 0
                            ) {
                                moved = true
                                previousDirection = previousDirection * 5
                                veryLastDirection = 5
                                it.move(1)
                            } else if ((dx <= -SWIPE_THRESHOLD_VELOCITY && previousDirection == 1 || x - startingX <= -MOVE_THRESHOLD)
                                && previousDirection % 7 != 0
                            ) {
                                moved = true
                                previousDirection = previousDirection * 7
                                veryLastDirection = 7
                                it.move(3)
                            }
                            if (moved) {
                                hasMoved = true
                                startingX = x
                                startingY = y
                            }
                        }
                    }
                    previousX = x
                    previousY = y
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    x = event.x
                    y = event.y
                    previousDirection = 1
                    veryLastDirection = 1
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }

    private fun pathMoved(): Float {
        return (x - startingX) * (x - startingX) + (y - startingY) * (y - startingY)
    }
}