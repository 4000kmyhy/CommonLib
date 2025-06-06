package com.lib.lib_lyric.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import androidx.core.content.ContextCompat
import com.lib.lib_lyric.R
import com.lib.lib_lyric.entity.LyricLayout
import com.lib.lib_lyric.utils.LyricUtils
import kotlin.math.roundToInt


/**
 * desc:
 **
 * user: xujj
 * time: 2024/2/26 14:41
 **/
class LyricView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseLyricView(context, attrs, defStyleAttr) {

    companion object {
        private const val ANIM_DURATION = 1000L //动画时长
        private const val TIME_LINE_KEEP_DURATION = 3000L //时间线显示时长

        const val CENTER_LINE_NONE = -1
        const val CENTER_LINE_CENTER = 0
        const val CENTER_LINE_START = 1
        const val CENTER_LINE_END = 2
    }

    private val mLyricLayouts = ArrayList<LyricLayout>()
    private val mPaint = TextPaint()
    private val mRect = RectF()

    private var timePaint = TextPaint()
    private var mPlayRect = RectF()
    private var mPlayDrawable = ContextCompat.getDrawable(context, R.drawable.lyric_ic_play)

    private val tapPaint = Paint()
    private val tapRect = RectF()
    private val tapPadding = LyricUtils.dp2px(context, 8f).toFloat()

    private var emptyLayout: StaticLayout? = null
    private var loadingLayout: StaticLayout? = null
    private var currentLayout: StaticLayout? = null

    private var mCurrentSize = LyricUtils.sp2px(context, 16f).toFloat()

    private var mCurrentLineBias = 0.5f //当前行位置
    private var mCenterLineType = CENTER_LINE_CENTER
    private var isFadeEdge = true

    private var mOffset = 0f
    private var mAnimator: ValueAnimator? = null

    private var isTouching = false
    private var isFling = false
    private var isShowTimeline = false

    private var isTapLyric = false
    private var tapTime = 0L

    private var isCurrentTextFake = true

    init {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LyricView)
            mCurrentSize = typedArray.getDimension(
                R.styleable.LyricView_lrcCurrentTextSize,
                mCurrentSize
            )
            mCurrentLineBias = typedArray.getFloat(
                R.styleable.LyricView_lrcCurrentLineBias,
                mCurrentLineBias
            )
            mCenterLineType = typedArray.getInteger(
                R.styleable.LyricView_lrcCenterLineType,
                mCenterLineType
            )
            isFadeEdge = typedArray.getBoolean(
                R.styleable.LyricView_lrcFadeEdge,
                isFadeEdge
            )
            isCurrentTextFake = typedArray.getBoolean(
                R.styleable.LyricView_lrcCurrentTextFake,
                true
            )
            typedArray.recycle()
        }

        tapPaint.color = Color.parseColor("#80DDDDDD")
    }

    override fun setTextSize(size: Float) {
        mCurrentSize = LyricUtils.sp2px(context, size + 2).toFloat()//当前行歌词要大一些
        super.setTextSize(size)
    }

    fun setCurrentTextSize(size: Float) {
        mCurrentSize = LyricUtils.sp2px(context, size).toFloat()
        initLyricLayouts()
    }

    fun setCurrentLineBias(bias: Float) {
        mCurrentLineBias = bias
        invalidate()
    }

    fun setCenterLineType(type: Int) {
        mCenterLineType = type
        invalidate()
    }

    override fun initLyricLayouts() {
        if (!hasLrc() || width == 0) return

        emptyLayout = null
        loadingLayout = null
        currentLayout = null

        mLyricLayouts.clear()
        mPaint.textSize = mTextSize
        for (lyric in mLyricList) {
            mLyricLayouts.add(LyricLayout(createStaticLayout(lyric.text, mPaint)))
        }

        mOffset = 0.5f * height
//        invalidate()
        smoothScrollToLine(mCurrentLine, 0L)
    }

    override fun reset() {
        endAnimation()
        mOffset = 0f
        super.reset()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //加载中
        if (isLoading) {
            mPaint.textSize = mTextSize
            if (loadingLayout == null) {
                loadingLayout = createStaticLayout(mLoadingText, mPaint)
            }
            mPaint.color = mEmptyColor
            drawText(canvas, loadingLayout, 0.5f * (height - loadingLayout!!.height))
            return
        }

        //无歌词文件
        if (!hasLrc()) {
            mPaint.textSize = mTextSize
            if (emptyLayout == null) {
                emptyLayout = createStaticLayout(mEmptyText, mPaint)
            }
            mPaint.color = mEmptyColor
            drawText(canvas, emptyLayout, 0.5f * (height - emptyLayout!!.height))
            return
        }

        val centerLine = getCenterLine()//屏幕中间的行数

        if (isFadeEdge) {
            //顶部淡出
            for (i in 0 until 10) {
                val top = height * 0.1f * i / 10
                val bottom = height * 0.1f * (i + 1) / 10
                val alpha = 0xFF * i / 10
                canvas.saveLayerAlpha(
                    0f, top, width.toFloat(), bottom, alpha
                )
                drawLyric(canvas, centerLine, top, bottom)
                canvas.restore()
            }

            canvas.saveLayerAlpha(
                0f, height * 0.1f, width.toFloat(), height * 0.9f, 0xFF
            )
            drawLyric(canvas, centerLine, height * 0.1f, height * 0.9f)
            canvas.restore()

            //底部淡出
            for (i in 0 until 10) {
                val top = height * 0.9f + height * 0.1f * i / 10
                val bottom = height * 0.9f + height * 0.1f * (i + 1) / 10
                val alpha = 0xFF * (10 - i) / 10
                canvas.saveLayerAlpha(
                    0f, top, width.toFloat(), bottom, alpha
                )
                drawLyric(canvas, centerLine, top, bottom)
                canvas.restore()
            }
        } else {
            canvas.save()
            drawLyric(canvas, centerLine)
            canvas.restore()
        }

        if (isShowTimeline) {
            val timeText = LyricUtils.stringForTime(mLyricList[centerLine].time)
            drawCenterLine(canvas, timeText)
        }
    }

    private fun drawLyric(
        canvas: Canvas,
        centerLine: Int,
        top: Float = 0f,
        bottom: Float = height.toFloat()
    ) {
        if (!isUpdateProgress) { //更新当前行进度时不用平移
            val dy = mOffset + height * (mCurrentLineBias - 0.5f)
            canvas.translate(0f, dy)
        }

        var dy = 0f
        var lastLayoutHeight = 0
        for (i in mLyricLayouts.indices) {
            var lyricLayout = mLyricLayouts[i].staticLayout
            if (i == mCurrentLine) {
                //字体加粗
                mPaint.isFakeBoldText = isCurrentTextFake
                mPaint.textSize = mCurrentSize
                mPaint.color = mCurrentColor
                //因为改变了字体大小，staticLayout高度可能会发生改变
                lyricLayout = if (currentLayout != null) {
                    currentLayout!!
                } else {
                    createStaticLayout(getCurrentText(), mPaint)
                }
                if (i > 0) {
                    dy += (lastLayoutHeight + lyricLayout.height) * 0.5f + mDividerHeight
                }
                if (dy + lyricLayout.height > top - mOffset - height * (mCurrentLineBias - 0.5f) &&
                    dy - lyricLayout.height < bottom - mOffset - height * (mCurrentLineBias - 0.5f)
                ) { //在屏幕内才绘制
                    if (isTapLyric) { //点击改变歌词行，显示背景框
                        tapRect.set(
                            mPadding - tapPadding,
                            dy - lyricLayout.height * 0.5f - tapPadding,
                            width - mPadding + tapPadding,
                            dy + lyricLayout.height * 0.5f + tapPadding
                        )
                        tapPaint.color = LyricUtils.getAlphaColor(mCurrentColor, 0.2f)
                        canvas.drawRoundRect(tapRect, tapPadding, tapPadding, tapPaint)
                    }

                    drawText(canvas, lyricLayout, dy - lyricLayout.height * 0.5f)

                    //绘制高亮歌词
                    mPaint.color = mLightColor
                    val lineCount = lyricLayout.lineCount
                    val lineHeight = 1.0f * lyricLayout.height / lineCount
                    var textWidth = mPaint.measureText(getCurrentText()) * mProgress / 100f
                    for (j in 0 until lineCount) {
                        //当前文本字体会大一些，getHeight不准确，上下各增加0.5的文本间距
                        mRect.top = j * lineHeight + dy - lyricLayout.height * 0.5f
                        mRect.bottom = (j + 1) * lineHeight + dy - lyricLayout.height * 0.5f
                        mRect.left = getRectLeft(lyricLayout.getLineWidth(j))
                        mRect.right = getRectRight(
                            lyricLayout.getLineWidth(j), textWidth,
                            textWidth > lyricLayout.getLineWidth(j)
                        )
                        drawText(canvas, lyricLayout, mRect, dy - lyricLayout.height * 0.5f)
                        textWidth -= lyricLayout.getLineWidth(j)
                    }
                }
                //关闭加粗
                mPaint.isFakeBoldText = false
            } else if (i == centerLine && isShowTimeline) {
                mPaint.textSize = mTextSize
                mPaint.color = mCurrentColor
                if (i > 0) {
                    dy += (lastLayoutHeight + lyricLayout.height) * 0.5f + mDividerHeight
                }
                if (dy + lyricLayout.height > top - mOffset - height * (mCurrentLineBias - 0.5f) &&
                    dy - lyricLayout.height < bottom - mOffset - height * (mCurrentLineBias - 0.5f)
                ) { //在屏幕内才绘制
                    drawText(canvas, lyricLayout, dy - lyricLayout.height * 0.5f)
                }
            } else {
                mPaint.textSize = mTextSize
                mPaint.color = mDefaultColor
                if (i > 0) {
                    dy += (lastLayoutHeight + lyricLayout.height) * 0.5f + mDividerHeight
                }
                if (dy + lyricLayout.height > top - mOffset - height * (mCurrentLineBias - 0.5f) &&
                    dy - lyricLayout.height < bottom - mOffset - height * (mCurrentLineBias - 0.5f)
                ) { //在屏幕内才绘制
                    drawText(canvas, lyricLayout, dy - lyricLayout.height * 0.5f)
                    mPaint.shader = null
                }
            }
            //记录上一行高度
            lastLayoutHeight = lyricLayout.height
        }
    }

    private fun drawCenterLine(canvas: Canvas, timeText: String) {
        if (mCenterLineType == CENTER_LINE_NONE) {
            mPlayRect.set(0f, 0f, 0f, 0f)
        } else {
            timePaint.textSize = LyricUtils.sp2px(context, 12f).toFloat()
            val textWidth = timePaint.measureText(timeText)
            val textHeight = timePaint.descent() + timePaint.ascent()
            var centerLineWidth = LyricUtils.dp2px(context, 50f).toFloat()
            when (mCenterLineType) {
                CENTER_LINE_CENTER -> {
                    timePaint.color = mCurrentColor
                    canvas.drawText(
                        timeText,
                        (centerLineWidth - textWidth) / 2,
                        (height - textHeight) / 2,
                        timePaint
                    )

                    val playSize = LyricUtils.dp2px(context, 24f).toFloat()
                    mPlayDrawable?.let {
                        val l = width - centerLineWidth / 2 - playSize / 2
                        val t = (height - playSize) / 2
                        it.setBounds(
                            l.roundToInt(),
                            t.roundToInt(),
                            (l + playSize).roundToInt(),
                            (t + playSize).roundToInt()
                        )
                        it.setTint(mCurrentColor)
                        it.draw(canvas)

                        mPlayRect.set(
                            l,
                            t,
                            l + playSize,
                            t + playSize
                        )
                    }
                }

                CENTER_LINE_START -> {
                    centerLineWidth = LyricUtils.dp2px(context, 80f).toFloat()
                    val playSize = LyricUtils.dp2px(context, 20f).toFloat()
                    val padding = LyricUtils.dp2px(context, 3f).toFloat()
                    val textX = width - (centerLineWidth + textWidth) / 2

                    timePaint.color = Color.parseColor("#33DDDDDD")
                    mPlayRect.set(
                        textX - playSize,
                        height / 2 - playSize / 2,
                        textX + textWidth + padding * 2,
                        height / 2 + playSize / 2
                    )
                    canvas.drawRoundRect(mPlayRect, padding, padding, timePaint)

                    mPlayDrawable?.let {
                        val l = textX - playSize
                        val t = (height - playSize) / 2
                        it.setBounds(
                            l.roundToInt(),
                            t.roundToInt(),
                            (l + playSize).roundToInt(),
                            (t + playSize).roundToInt()
                        )
                        it.setTint(mCurrentColor)
                        it.draw(canvas)
                    }

                    timePaint.color = mCurrentColor
                    canvas.drawText(
                        timeText,
                        textX,
                        (height - textHeight) / 2,
                        timePaint
                    )
                }

                CENTER_LINE_END -> {
                    centerLineWidth = LyricUtils.dp2px(context, 80f).toFloat()
                    val playSize = LyricUtils.dp2px(context, 20f).toFloat()
                    val padding = LyricUtils.dp2px(context, 3f).toFloat()
                    val textX = (centerLineWidth - textWidth) / 2 + playSize

                    timePaint.color = Color.parseColor("#33DDDDDD")
                    mPlayRect.set(
                        textX - playSize,
                        height / 2 - playSize / 2,
                        textX + textWidth + padding * 2,
                        height / 2 + playSize / 2
                    )
                    canvas.drawRoundRect(mPlayRect, padding, padding, timePaint)

                    mPlayDrawable?.let {
                        val l = textX - playSize
                        val t = (height - playSize) / 2
                        it.setBounds(
                            l.roundToInt(),
                            t.roundToInt(),
                            (l + playSize).roundToInt(),
                            (t + playSize).roundToInt()
                        )
                        it.setTint(mCurrentColor)
                        it.draw(canvas)
                    }

                    timePaint.color = mCurrentColor
                    canvas.drawText(
                        timeText,
                        textX,
                        (height - textHeight) / 2,
                        timePaint
                    )
                }
            }
        }
    }

    private fun getOffset(line: Int): Float {
        if (line in mLyricLayouts.indices) {
            if (mLyricLayouts[line].offset == Float.MIN_VALUE) {
                var offset = 0.5f * height
                for (i in 1..line) {
                    offset -= (mLyricLayouts[i - 1].getHeight() + mLyricLayouts[i].getHeight()) * 0.5f + mDividerHeight
                }
                mLyricLayouts[line].offset = offset
            }
            var offset = mLyricLayouts[line].offset
            if (currentLayout == null) {
                mPaint.isFakeBoldText = isCurrentTextFake
                mPaint.textSize = mCurrentSize
                currentLayout = createStaticLayout(getCurrentText(), mPaint)
                mPaint.isFakeBoldText = false
            }
            if (line > mCurrentLine) {
                if (mCurrentLine == 0) {
                    offset -= (currentLayout!!.height - mLyricLayouts[mCurrentLine].getHeight()) * 0.5f
                } else {
                    offset -= currentLayout!!.height - mLyricLayouts[mCurrentLine].getHeight()
                }
            } else if (line == mCurrentLine) {
                if (mCurrentLine != 0) {
                    offset -= (currentLayout!!.height - mLyricLayouts[mCurrentLine].getHeight()) * 0.5f
                }
            }
            return offset
        }
        return 0f
    }

    private fun getLayoutHeight(line: Int): Int {
        if (line in mLyricLayouts.indices) {
            if (line == mCurrentLine) {
                if (currentLayout == null) {
                    mPaint.isFakeBoldText = isCurrentTextFake
                    mPaint.textSize = mCurrentSize
                    currentLayout = createStaticLayout(getCurrentText(), mPaint)
                    mPaint.isFakeBoldText = false
                }
                return currentLayout!!.height
            } else {
                return mLyricLayouts[line].getHeight()
            }
        }
        return 0
    }

    private fun getCenterLine(): Int {
        var centerLine = 0
        var minDistance = Float.MAX_VALUE
        val dy = mOffset + height * (mCurrentLineBias - 0.5f)
        for (i in mLyricLayouts.indices) {
            if (Math.abs(dy - getOffset(i)) < minDistance) {
                minDistance = Math.abs(dy - getOffset(i))
                centerLine = i
            }
        }
        return centerLine
    }

    override fun updateTime(time: Long, duration: Long) {
        if (Math.abs(tapTime - time) >= 100) {
            tapTime = 0
            super.updateTime(time, duration)
        } else if (tapTime <= time) {//当前时间可能小于seekTo的时间
            super.updateTime(time, duration)
        }
    }

    override fun currentLineUpdated(line: Int) {
//        super.currentLineUpdated()

        mPaint.isFakeBoldText = isCurrentTextFake
        mPaint.textSize = mCurrentSize
        currentLayout = createStaticLayout(getCurrentText(), mPaint)
        mPaint.isFakeBoldText = false

        if (!isShowTimeline) {
            if (mAnimator?.isRunning == false) {
                smoothScrollToLine(line)
            }
        } else {
            invalidate()
        }
    }

    private fun setCurrentLine(line: Int, tapTime: Long) {
        this.tapTime = tapTime

        isShowTimeline = false
        mProgress = 0 //换行归0
        if (mCurrentLine != line) {
            mCurrentLine = line

            mPaint.isFakeBoldText = isCurrentTextFake
            mPaint.textSize = mCurrentSize
            currentLayout = createStaticLayout(getCurrentText(), mPaint)
            mPaint.isFakeBoldText = false
        }
        smoothScrollToLine(line)
    }

    /**
     * 滚动到某一行
     */
    private fun smoothScrollToLine(line: Int, duration: Long = ANIM_DURATION) {
        endAnimation()

        var offset = getOffset(line)
        offset = Math.min(offset, getOffset(0) - height * (mCurrentLineBias - 0.5f))
        offset =
            Math.max(offset, getOffset(mLyricLayouts.size - 1) - height * (mCurrentLineBias - 0.5f))
        mAnimator = ValueAnimator.ofFloat(mOffset, offset)
        mAnimator?.duration = duration
        mAnimator?.interpolator = LinearInterpolator()
        mAnimator?.addUpdateListener { animation ->
            mOffset = animation.animatedValue as Float
            invalidate()
        }
        mAnimator?.start()
    }

    private fun endAnimation() {
        if (mAnimator != null && mAnimator!!.isRunning) {
            mAnimator!!.end()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        endAnimation()
        removeCallbacks(hideTimelineRunnable)
    }

    /**
     * 手势监听器
     */
    private val mSimpleOnGestureListener = object : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            if (mLyricListener != null) {
                if (hasLrc()) {
                    endAnimation()
                    removeCallbacks(hideTimelineRunnable)
                    mScroller.forceFinished(true)
                    isTouching = true
                    invalidate()
                }
                return true
            }
            return super.onDown(e)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (hasLrc()) {
                isShowTimeline = true //显示时间线
                mOffset += -distanceY
                mOffset = Math.min(mOffset, getOffset(0) - height * (mCurrentLineBias - 0.5f))
                mOffset = Math.max(
                    mOffset,
                    getOffset(mLyricLayouts.size - 1) - height * (mCurrentLineBias - 0.5f)
                )
                invalidate()
                return true
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (hasLrc()) {
                mScroller.fling(
                    0, mOffset.toInt(),
                    0, velocityY.toInt(),
                    0, 0,
                    (getOffset(mLyricLayouts.size - 1) - height * (mCurrentLineBias - 0.5f)).toInt(),
                    (getOffset(0) - height * (mCurrentLineBias - 0.5f)).toInt()
                )
                isFling = true
                return true
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (mLyricListener != null) {
                if (hasLrc()) {
                    if (isShowTimeline && mPlayRect.contains(e.x, e.y)) { // 点击了播放按钮
                        val centerLine = getCenterLine()
                        if (mLyricListener!!.onPlayClick(mLyricList[centerLine].time)) {
                            setCurrentLine(centerLine, mLyricList[centerLine].time)
                            return true
                        }
                    }
                    if (e.x > mPadding && e.x < width - mPadding) { //点击了某行歌词
                        for (i in mLyricLayouts.indices) {
                            val offset = getOffset(i)
                            val layoutHeight = getLayoutHeight(i)
                            val dy = mOffset + height * mCurrentLineBias
                            if (e.y > dy - offset - 0.5f * (layoutHeight + mDividerHeight) &&
                                e.y < dy - offset + 0.5f * (layoutHeight + mDividerHeight)
                            ) {
                                if (mLyricListener!!.onPlayClick(mLyricList[i].time)) {
                                    setCurrentLine(i, mLyricList[i].time)
                                    isTapLyric = true
                                    invalidate()
                                    postDelayed({
                                        isTapLyric = false
                                        invalidate()
                                    }, 300)
                                    return true
                                }
                            }
                        }
                    }
                }
                mLyricListener!!.onViewClick(hasLrc())
                return true
            }
            return super.onSingleTapConfirmed(e)
        }
    }

    private val hideTimelineRunnable = Runnable {
        if (hasLrc() && isShowTimeline) {
            isShowTimeline = false
            smoothScrollToLine(mCurrentLine)
        }
    }

    private val mScroller = Scroller(context)
    private val mGestureDetector: GestureDetector =
        GestureDetector(context, mSimpleOnGestureListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP ||
            event.action == MotionEvent.ACTION_CANCEL
        ) {
            isTouching = false
            if (hasLrc() && !isFling) {
                postDelayed(hideTimelineRunnable, TIME_LINE_KEEP_DURATION)
            }
        }
        return mGestureDetector.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mOffset = mScroller.currY.toFloat()
            invalidate()
        }
        if (isFling && mScroller.isFinished) {
            isFling = false
            if (hasLrc() && !isTouching) {
                postDelayed(hideTimelineRunnable, TIME_LINE_KEEP_DURATION)
            }
        }
    }

    //解决滑动冲突
    private var mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var startX = 0f
    private var startY = 0f
    private var isBeginScroll = false

//    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
//        when (ev.action) {
//            MotionEvent.ACTION_DOWN -> {
//                isBeginScroll = false
//                startX = ev.x
//                startY = ev.y
//                parent.requestDisallowInterceptTouchEvent(true)
//            }
//
//            MotionEvent.ACTION_MOVE -> {
//                val disX = Math.abs(ev.x - startX)
//                val disY = Math.abs(ev.y - startY)
//                if (disY > mTouchSlop) {
//                    isBeginScroll = true
//                    parent.requestDisallowInterceptTouchEvent(true)
//                } else {
//                    if (disX > mTouchSlop) {
//                        if (!isBeginScroll) {
//                            parent.requestDisallowInterceptTouchEvent(false)
//                        }
//                    }
//                }
//            }
//
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                parent.requestDisallowInterceptTouchEvent(false)
//            }
//        }
//        return super.dispatchTouchEvent(ev)
//    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isBeginScroll = false
                startX = ev.x
                startY = ev.y
//                parent.requestDisallowInterceptTouchEvent(true)
                if (startX > width / 3f &&
                    startX < width - width / 3f
                ) {//直接拦截的话compose父类无法接收了
                    parent.requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val disX = Math.abs(ev.x - startX)
                val disY = Math.abs(ev.y - startY)
                if (disY > disX) {
                    isBeginScroll = true
                    parent.requestDisallowInterceptTouchEvent(true)
                } else {
                    if (disX > mTouchSlop) {
                        if (!isBeginScroll) {
                            parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}