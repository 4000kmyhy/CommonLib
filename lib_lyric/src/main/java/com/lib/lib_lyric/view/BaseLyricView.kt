package com.lib.lib_lyric.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import com.lib.lib_lyric.R
import com.lib.lib_lyric.entity.LyricBean
import com.lib.lib_lyric.inter.OnLyricListener
import com.lib.lib_lyric.utils.LyricKt
import com.lib.lib_lyric.utils.LyricUtils
import com.lib.lib_lyric.utils.getLoadingLyricAction
import com.lib.lib_lyric.utils.getUpdateLyricAction

/**
 * desc:
 **
 * user: xujj
 * time: 2024/2/22 11:48
 **/
abstract class BaseLyricView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val GRAVITY_CENTER = 0
        const val GRAVITY_START = 1
        const val GRAVITY_END = 2
    }

    protected var mPadding = LyricUtils.dp2px(context, 50f).toFloat()
    protected var mDividerHeight = LyricUtils.dp2px(context, 15f).toFloat()
    protected var mTextSize = LyricUtils.sp2px(context, 14f).toFloat()
    protected var mDefaultColor = ContextCompat.getColor(context, R.color.lyric_default)
    protected var mCurrentColor = ContextCompat.getColor(context, R.color.lyric_current)
    protected var mLightColor = ContextCompat.getColor(context, R.color.lyric_light)
    protected var mEmptyColor = mCurrentColor
    protected var mEmptyText = context.getString(R.string.lyric_no_find)
    protected var mLoadingText = context.getString(R.string.lyric_loading)
    protected var mGravity = GRAVITY_CENTER

    protected var isLoading = false

    protected val mLyricList = ArrayList<LyricBean>()
    protected var mCurrentLine = 0
    protected var mProgress = 0
    protected var isUpdateProgress = false

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (getLoadingLyricAction(context) == action) {
                loading()
            } else if (getUpdateLyricAction(context) == action) {
                setLyricList(LyricKt.getLyricList())
            }
        }
    }

    protected var mLyricListener: OnLyricListener? = null

    fun setOnLyricListener(listener: OnLyricListener?) {
        mLyricListener = listener
    }

    init {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BaseLyricView)
            mPadding = typedArray.getDimension(
                R.styleable.BaseLyricView_lrcPadding,
                mPadding
            )
            mDividerHeight = typedArray.getDimension(
                R.styleable.BaseLyricView_lrcDividerHeight,
                mDividerHeight
            )
            mTextSize = typedArray.getDimension(
                R.styleable.BaseLyricView_lrcTextSize,
                mTextSize
            )
            mDefaultColor = typedArray.getColor(
                R.styleable.BaseLyricView_lrcDefaultColor,
                mDefaultColor
            )
            mCurrentColor = typedArray.getColor(
                R.styleable.BaseLyricView_lrcCurrentColor,
                mCurrentColor
            )
            mLightColor = typedArray.getColor(
                R.styleable.BaseLyricView_lrcLightColor,
                mLightColor
            )
            mEmptyColor = typedArray.getColor(
                R.styleable.BaseLyricView_lrcEmptyColor,
                mCurrentColor
            )
            typedArray.getString(R.styleable.BaseLyricView_lrcEmptyText)?.let {
                mEmptyText = it
            }
            typedArray.getString(R.styleable.BaseLyricView_lrcLoadingText)?.let {
                mLoadingText = it
            }
            mGravity = typedArray.getInteger(
                R.styleable.BaseLyricView_lrcGravity,
                GRAVITY_CENTER
            )
            val autoBind = typedArray.getBoolean(R.styleable.BaseLyricView_lrcAutoBind, false)
            typedArray.recycle()

            if (autoBind) {
                autoBind()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initLyricLayouts()
    }

    open fun setDefaultColor(color: Int) {
        mDefaultColor = color
        invalidate()
    }

    open fun setCurrentColor(color: Int) {
        mCurrentColor = color
        invalidate()
    }

    /**
     * 设置歌词的高亮字体颜色
     */
    open fun setLightColor(color: Int) {
        mLightColor = color
        invalidate()
    }

    /**
     * 设置无歌词字体颜色
     */
    open fun setEmptyColor(color: Int) {
        mEmptyColor = color
        invalidate()
    }

    /**
     * 设置行间距
     */
    fun setDividerHeight(height: Float) {
        mDividerHeight = height
        invalidate()
    }

    /**
     * 设置歌词的字体大小，要重新构造staticLayout
     */
    open fun setTextSize(size: Float) {
        mTextSize = LyricUtils.sp2px(context, size).toFloat()
        initLyricLayouts()
    }

    /**
     * 设置歌词方向，要重新构造staticLayout
     */
    fun setGravity(@IntRange(from = 0, to = 2) gravity: Int) {
        mGravity = gravity
        initLyricLayouts()
    }

    /**
     * 设置内间距，要重新构造staticLayout
     */
    fun setPadding(padding: Float) {
        mPadding = padding
        initLyricLayouts()
    }

    fun setLyric(lyric: String?) {
        setLyricList(LyricUtils.parseLyricByString(lyric))
    }

    fun setLyricList(lyricList: List<LyricBean>?) {
        reset()

        if (lyricList != null) {
            mLyricList.addAll(lyricList)
        }

        initLyricLayouts()
    }

    /**
     * 初始化歌词行
     */
    protected abstract fun initLyricLayouts()

    fun hasLrc(): Boolean {
        return mLyricList.isNotEmpty()
    }

    open fun updateTime(time: Long, duration: Long) {
        if (!hasLrc()) return
        val line = LyricUtils.findShowLine(mLyricList, time)

        val startTime: Long = if (line in mLyricList.indices) {
            mLyricList[line].time
        } else {
            0
        }
        val endTime: Long =
            if (line in mLyricList.indices && mLyricList[line].endTime > startTime) { //当前这句歌词的结束时间
                mLyricList[line].endTime
            } else if (line + 1 in mLyricList.indices) { //下一句歌词的开始时间
                mLyricList[line + 1].time
            } else { //歌曲结束时间
                duration
            }

        if (mCurrentLine != line) {
            mCurrentLine = line
            mProgress = 0 //换行归0
            currentLineUpdated(line)
        }

        updateProgress(time, startTime, endTime)
    }

    /**
     * 更新当前行播放进度
     */
    private fun updateProgress(time: Long, startTime: Long, endTime: Long) {
        if (endTime > startTime) {
//            val progress = ((time - startTime) * 100 / (endTime - startTime)).toInt()
//                .coerceAtMost(100)
//            if (progress > mProgress) {
//                mProgress = progress
//                isUpdateProgress = true
//                invalidate()
//                isUpdateProgress = false
//            }
            mProgress = ((time - startTime) * 100 / (endTime - startTime)).toInt()
                .coerceAtMost(100)
            isUpdateProgress = true
            invalidate()
            isUpdateProgress = false
        }
    }

    protected open fun currentLineUpdated(line: Int) {
        invalidate()
    }

    fun getCurrentText(): String {
        return if (mCurrentLine in mLyricList.indices) {
            mLyricList[mCurrentLine].text
        } else {
            ""
        }
    }

    open fun reset() {
        isLoading = false
        mLyricList.clear()
        mCurrentLine = 0
        invalidate()
    }

    fun loading() {
        isLoading = true
        invalidate()
    }

    /**
     * 自动绑定
     */
    fun autoBind() {
        setLyricList(LyricKt.getLyricList())
        val filter = IntentFilter()
        filter.addAction(getLoadingLyricAction(context))
        filter.addAction(getUpdateLyricAction(context))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(mReceiver, filter)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            context.unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**************************  分割线  ***************************/

    /**
     * 画一行歌词
     */
    protected fun drawText(canvas: Canvas, staticLayout: StaticLayout?, dy: Float) {
        if (staticLayout == null) return
        canvas.let {
            it.save()
            it.translate(mPadding, dy)
            staticLayout.draw(it)
            it.restore()
        }
    }

    protected fun drawText(canvas: Canvas, staticLayout: StaticLayout?, rect: RectF, dy: Float) {
        if (staticLayout == null) return
        canvas.let {
            it.save()
            it.clipRect(rect)
            it.translate(mPadding, dy)
            staticLayout.draw(it)
            it.restore()
        }
    }

    protected fun createStaticLayout(source: CharSequence, paint: TextPaint): StaticLayout {
        var align = Layout.Alignment.ALIGN_CENTER
        if (mGravity == GRAVITY_CENTER) {
            align = Layout.Alignment.ALIGN_CENTER
        } else if (mGravity == GRAVITY_START) {
            align = Layout.Alignment.ALIGN_NORMAL
        } else if (mGravity == GRAVITY_END) {
            align = Layout.Alignment.ALIGN_OPPOSITE
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder =
                StaticLayout.Builder.obtain(source, 0, source.length, paint, getLrcWidth().toInt())
            builder.setAlignment(align)
            builder.setLineSpacing(1f,1f)
            builder.build()
        } else {
            StaticLayout(
                source,
                paint,
                getLrcWidth().toInt(),
                align,
                1f,
                0f,
                true
            )
        }
    }

    /**
     * 获取歌词宽度
     */
    private fun getLrcWidth(): Float {
        val lrcWidth = width - mPadding * 2
        return if (lrcWidth > 0) {
            lrcWidth
        } else {
            width.toFloat()
        }
    }

    /**
     * 计算高亮色起点
     */
    protected fun getRectLeft(lineWidth: Float): Float {
        var left = (width - lineWidth) / 2
        if (mGravity == GRAVITY_CENTER) {
            left = (width - lineWidth) / 2
        } else if (mGravity == GRAVITY_START) {
            left = mPadding
        } else if (mGravity == GRAVITY_END) {
            left = width - lineWidth - mPadding
        }
        return left
    }

    /**
     * 计算高亮色终点
     */
    protected fun getRectRight(lineWidth: Float, textWidth: Float, isFull: Boolean): Float {
        var right = if (isFull) (width + lineWidth) / 2 else (width - lineWidth) / 2 + textWidth
        if (mGravity == GRAVITY_CENTER) {
            right = if (isFull) (width + lineWidth) / 2 else (width - lineWidth) / 2 + textWidth
        } else if (mGravity == GRAVITY_START) {
            right = if (isFull) mPadding + lineWidth else mPadding + textWidth
        } else if (mGravity == GRAVITY_END) {
            right =
                if (isFull) width - mPadding else width - lineWidth - mPadding + textWidth
        }
        return right
    }
}