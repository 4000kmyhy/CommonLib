package com.lib.lib_lyric.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.lib.lib_lyric.R
import com.lib.lib_lyric.entity.LyricLayout

/**
 * desc:
 **
 * user: xujj
 * time: 2024/2/22 15:46
 **/
class TwoLyricView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseLyricView(context, attrs, defStyleAttr) {

    private val mLyricLayouts = ArrayList<LyricLayout>()
    private val mPaint = TextPaint()
    private val mRect = RectF()

    private var emptyLayout: StaticLayout? = null
    private var loadingLayout: StaticLayout? = null

    private var mLines = 1
    private var isShowStroke = false
    private var isWrapHeight = false

    private var mStrokeTextColor = ContextCompat.getColor(context, R.color.lyric_stroke) //描边颜色
    private var contentHeight = 0f

    init {
        var isTextBold = false
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TwoLyricView)
            mLines = typedArray.getInteger(
                R.styleable.TwoLyricView_lrcLines,
                mLines
            )
            isShowStroke = typedArray.getBoolean(
                R.styleable.TwoLyricView_lrcShowStroke,
                isShowStroke
            )
            isTextBold = typedArray.getBoolean(
                R.styleable.TwoLyricView_lrcTextBold,
                isTextBold
            )
            isWrapHeight = typedArray.getBoolean(
                R.styleable.TwoLyricView_lrcWrapHeight,
                isWrapHeight
            )
            typedArray.recycle()
        }

        mPaint.textSize = mTextSize
        //字体加粗
        mPaint.isFakeBoldText = isTextBold

        setOnClickListener {
            mLyricListener?.onViewClick(hasLrc())
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (isWrapHeight) {
            setMeasuredDimension(measuredWidth, contentHeight.toInt())
        }
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        if (isWrapHeight) {
            calculateContentHeight()
        }
    }

    private fun calculateContentHeight() {
        mPaint.textSize = mTextSize
        val staticLayout = createStaticLayout("A", mPaint)
        contentHeight = if (mLines == 1) {
            staticLayout.height.toFloat()
        } else {
            staticLayout.height * 2 + mDividerHeight
        }
        requestLayout()
    }

    override fun initLyricLayouts() {
        if (!hasLrc() || width == 0) return

        emptyLayout = null
        loadingLayout = null

        mLyricLayouts.clear()
        mPaint.textSize = mTextSize
        for (lyric in mLyricList) {
            mLyricLayouts.add(LyricLayout(createStaticLayout(lyric.text, mPaint)))
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //加载中
        if (isLoading) {
            mPaint.textSize = mTextSize
            if (loadingLayout == null) {
                loadingLayout = createStaticLayout(mLoadingText, mPaint)
            }
            if (isShowStroke) {//描边
                mPaint.color = mStrokeTextColor
                mPaint.setStroke(true)
                drawText(canvas, loadingLayout, 0.5f * (height - loadingLayout!!.height))
            }
            mPaint.color = mEmptyColor
            mPaint.setStroke(false)
            drawText(canvas, loadingLayout, 0.5f * (height - loadingLayout!!.height))
            return
        }

        //无歌词文件
        if (!hasLrc()) {
            mPaint.textSize = mTextSize
            if (emptyLayout == null) {
                emptyLayout = createStaticLayout(mEmptyText, mPaint)
            }
            if (isShowStroke) {//描边
                mPaint.color = mStrokeTextColor
                mPaint.setStroke(true)
                drawText(canvas, emptyLayout, 0.5f * (height - emptyLayout!!.height))
            }
            mPaint.color = mEmptyColor
            mPaint.setStroke(false)
            drawText(canvas, emptyLayout, 0.5f * (height - emptyLayout!!.height))
            return
        }

        if (mLines == 1) { //一行歌词
            if (mCurrentLine in mLyricLayouts.indices) {
                val staticLayout = mLyricLayouts[mCurrentLine].staticLayout
                mPaint.textSize = mTextSize
                val descent = mPaint.descent()

                val lineCount = staticLayout.lineCount
                var staticLine = 0
                var staticWidth = 0f
                val textWidth: Float = mPaint.measureText(getCurrentText()) * mProgress / 100
                while (staticLine < lineCount) {
                    staticWidth += staticLayout.getLineWidth(staticLine)
                    if (staticWidth >= textWidth || staticLine == lineCount - 1) {
                        break
                    }
                    staticLine++
                }
                val lineHeight = 1.0f * staticLayout.height / lineCount
                mRect.set(
                    mPadding,
                    0.5f * (height - lineHeight),
                    width - mPadding,
                    0.5f * (height + lineHeight)
                )
                mRect.offset(0f, descent / 2)//往下偏移一点
                val dy = 0.5f * height - (staticLine + 0.5f) * lineHeight
                if (isShowStroke) {//描边
                    mPaint.color = mStrokeTextColor
                    mPaint.setStroke(true)
                    drawText(canvas, staticLayout, mRect, dy)
                }
                mPaint.color = mCurrentColor
                mPaint.setStroke(false)
                drawText(canvas, staticLayout, mRect, dy)

                //绘制高亮歌词
                mPaint.color = mLightColor
                mRect.set(
                    getRectLeft(staticLayout.getLineWidth(staticLine)),
                    0.5f * (height - lineHeight),
                    getRectRight(
                        staticLayout.getLineWidth(staticLine),
                        staticLayout.getLineWidth(staticLine) - (staticWidth - textWidth),
                        false
                    ),
                    0.5f * (height + lineHeight)
                )
                mRect.offset(0f, descent / 2)//往下偏移一点
                drawText(canvas, staticLayout, mRect, dy)
            }
        } else { //两行歌词

            //当前一句歌词
            if (mCurrentLine in mLyricLayouts.indices) {
                val staticLayout = mLyricLayouts[mCurrentLine].staticLayout
                mPaint.textSize = mTextSize
                val descent = mPaint.descent()

                val lineCount = staticLayout.lineCount
                var staticLine = 0
                var staticWidth = 0f
                val textWidth: Float = mPaint.measureText(getCurrentText()) * mProgress / 100
                while (staticLine < lineCount) {
                    staticWidth += staticLayout.getLineWidth(staticLine)
                    if (staticWidth >= textWidth || staticLine == lineCount - 1) {
                        break
                    }
                    staticLine++
                }
                val lineHeight = 1.0f * staticLayout.height / lineCount
                val lineY = if (mCurrentLine % 2 == 0) { //第一行
                    0.5f * (height - lineHeight - mDividerHeight)
                } else { //第二行
                    0.5f * (height + lineHeight + mDividerHeight)
                }
                mRect.set(
                    mPadding,
                    lineY - 0.5f * lineHeight,
                    width - mPadding,
                    lineY + 0.5f * lineHeight
                )
                mRect.offset(0f, descent / 2)//往下偏移一点
                val dy = lineY - (staticLine + 0.5f) * lineHeight
                if (isShowStroke) {
                    mPaint.color = mStrokeTextColor
                    mPaint.setStroke(true)
                    drawText(canvas, staticLayout, mRect, dy)
                }
                mPaint.color = mCurrentColor
                mPaint.setStroke(false)
                drawText(canvas, staticLayout, mRect, dy)

                //绘制高亮歌词
                mPaint.color = mLightColor
                mRect.set(
                    getRectLeft(staticLayout.getLineWidth(staticLine)),
                    lineY - 0.5f * lineHeight,
                    getRectRight(
                        staticLayout.getLineWidth(staticLine),
                        staticLayout.getLineWidth(staticLine) - (staticWidth - textWidth),
                        false
                    ),
                    lineY + 0.5f * lineHeight
                )
                mRect.offset(0f, descent / 2)//往下偏移一点
                drawText(canvas, staticLayout, mRect, dy)
            }

            //下一句歌词
            if (mCurrentLine + 1 in mLyricLayouts.indices) {
                val staticLayout = mLyricLayouts[mCurrentLine + 1].staticLayout
                mPaint.textSize = mTextSize
                val descent = mPaint.descent()

                val lineCount = staticLayout.lineCount
                val lineHeight = 1.0f * staticLayout.height / lineCount
                val lineY = if (mCurrentLine % 2 == 1) { //第一行
                    0.5f * (height - lineHeight - mDividerHeight)
                } else { //第二行
                    0.5f * (height + lineHeight + mDividerHeight)
                }
                mRect.set(
                    mPadding,
                    lineY - 0.5f * lineHeight,
                    width - mPadding,
                    lineY + 0.5f * lineHeight
                )
                mRect.offset(0f, descent / 2)//往下偏移一点
                val dy = lineY - 0.5f * lineHeight
                if (isShowStroke) {
                    mPaint.color = mStrokeTextColor
                    mPaint.setStroke(true)
                    drawText(canvas, staticLayout, mRect, dy)
                }
                mPaint.color = mDefaultColor
                mPaint.setStroke(false)
                drawText(canvas, staticLayout, mRect, dy)
            }
        }
    }

    private fun Paint.setStroke(isStroke: Boolean) {
        if (isStroke) {
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        } else {
            strokeWidth = 0f
            style = Paint.Style.FILL
        }
    }
}