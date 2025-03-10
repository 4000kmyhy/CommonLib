package com.lib.game2048.entity

class AnimationGrid(x: Int, y: Int) {

    private var field: Array<Array<ArrayList<AnimationCell>?>>
    private var activeAnimations = 0
    private var oneMoreFrame = false
    var globalAnimation = ArrayList<AnimationCell>()
        private set

    init {
        field = Array(x) { arrayOfNulls(y) }
        for (xx in 0 until x) {
            for (yy in 0 until y) {
                field[xx][yy] = ArrayList()
            }
        }
    }

    fun startAnimation(x: Int, y: Int, animationType: Int, length: Long, delay: Long, extras: IntArray?) {
        try {
            val animationToAdd = AnimationCell(x, y, animationType, length, delay, extras)
            if (x == -1 && y == -1) {
                globalAnimation.add(animationToAdd)
            } else {
                field[x][y]!!.add(animationToAdd)
            }
            activeAnimations = activeAnimations + 1
        } catch (ignored: ArrayIndexOutOfBoundsException) {
        }
    }

    fun tickAll(timeElapsed: Long) {
        try {
            val cancelledAnimations = ArrayList<AnimationCell>()
            for (animation in globalAnimation) {
                animation.tick(timeElapsed)
                if (animation.animationDone()) {
                    cancelledAnimations.add(animation)
                    activeAnimations = activeAnimations - 1
                }
            }
            for (array in field) {
                for (list in array) {
                    for (animation in list!!) {
                        animation.tick(timeElapsed)
                        if (animation.animationDone()) {
                            cancelledAnimations.add(animation)
                            activeAnimations = activeAnimations - 1
                        }
                    }
                }
            }
            for (animation in cancelledAnimations) {
                cancelAnimation(animation)
            }
        } catch (ignored: Exception) {
        }
    }

    fun isAnimationActive(): Boolean = if (activeAnimations != 0) {
        oneMoreFrame = true
        true
    } else if (oneMoreFrame) {
        oneMoreFrame = false
        true
    } else {
        false
    }

    fun getAnimationCell(x: Int, y: Int): ArrayList<AnimationCell> {
        return field[x][y]!!
    }

    fun cancelAnimations() {
        try {
            for (array in field) {
                for (list in array) {
                    list!!.clear()
                }
            }
            globalAnimation.clear()
            activeAnimations = 0
        } catch (ignored: Exception) {
        }
    }

    fun cancelAnimation(animation: AnimationCell) {
        try {
            if (animation.x == -1 && animation.y == -1) {
                globalAnimation.remove(animation)
            } else {
                field[animation.x][animation.y]!!.remove(animation)
            }
        } catch (ignored: Exception) {
        }
    }
}