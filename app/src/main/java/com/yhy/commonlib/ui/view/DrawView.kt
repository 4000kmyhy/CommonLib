package com.yhy.commonlib.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/16 17:36
 **/
class DrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mPath = Path()
    private var mPreX = 0f
    private var mPreY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPath.moveTo(event.x, event.y)
                mPreX = event.x
                mPreY = event.y
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val endX = (mPreX + event.x) / 2
                val endY = (mPreY + event.y) / 2
                mPath.quadTo(mPreX, mPreY, endX, endY)
                mPreX = event.x
                mPreY = event.y
                invalidate()
            }

            else -> {}
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val paint = Paint()
        paint.setStyle(Paint.Style.STROKE)
        paint.setColor(Color.GREEN)
        paint.setStrokeWidth(2f)

        canvas.drawPath(mPath, paint)
    }

    fun reset() {
        mPath.reset()
        postInvalidate()
    }
}