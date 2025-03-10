package com.lib.game2048.entity

class AnimationCell(
    x: Int,
    y: Int,
    val animationType: Int,
    private val animationTime: Long,
    private val delayTime: Long,
    var extras: IntArray?
) : Cell(x, y) {

    private var timeElapsed: Long = 0

    fun tick(timeElapsed: Long) {
        this.timeElapsed = this.timeElapsed + timeElapsed
    }

    fun animationDone(): Boolean {
        return animationTime + delayTime < timeElapsed
    }

    fun getPercentageDone(): Double = Math.max(0.0, 1.0 * (timeElapsed - delayTime) / animationTime)

    fun isActive(): Boolean = timeElapsed >= delayTime
}
