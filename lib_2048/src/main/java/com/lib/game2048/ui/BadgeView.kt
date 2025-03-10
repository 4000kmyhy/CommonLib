package com.lib.game2048.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * desc:
 **
 * user: xujj
 * time: 2023/12/22 15:05
 **/
class BadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()

    private var bgColor = Color.GRAY
    private var textColor = Color.WHITE
    private var valueText = "0"

    init {
        paint.isAntiAlias = true
        paint.textAlign = Paint.Align.CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val size = Math.min(widthSize, heightSize)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        paint.textSize = 0.9f * w
        val textSize = paint.textSize * 0.9f * w / Math.max(paint.measureText("99+"), paint.measureText("AD"))
        paint.textSize = textSize
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = bgColor
        canvas.drawCircle(0.5f * width, 0.5f * height, 0.5f * width, paint)

        if (valueText.isNotEmpty()) {
            paint.color = textColor
            canvas.drawText(
                valueText,
                0.5f * width,
                0.5f * (height - (paint.descent() + paint.ascent())),
                paint
            )
        }
    }

    fun setValue(value: Int) {
        if (value >= 0) {
            valueText = if (value <= 99) {
                "$value"
            } else {
                "99+"
            }
            invalidate()
        }
    }

    fun setValue(value: String) {
        valueText = value
        invalidate()
    }
}