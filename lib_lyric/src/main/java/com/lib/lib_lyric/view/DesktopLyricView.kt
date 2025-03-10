package com.lib.lib_lyric.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.lib.lib_lyric.R
import com.lib.lib_lyric.databinding.DesktopLyricViewBinding
import com.lib.lib_lyric.inter.OnLyricListener
import com.lib.lib_lyric.utils.DesktopLyricUtils
import kotlin.math.abs

/**
 * desc:
 **
 * user: xujj
 * time: 2024/11/7 11:55
 **/
class DesktopLyricView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private var isLock: Boolean = false
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = DesktopLyricViewBinding.inflate(LayoutInflater.from(context), this, true)

    private val mTouchSlop: Int

    private var isBgVisible = true
    private var colorIndex = 0
    private var textSize = DesktopLyricUtils.defaultSize

    private val mHandler = Handler(Looper.getMainLooper())

    private val mService = DesktopLyricUtils.getInstance().mService

    private var mLayoutParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null

    fun setParams(layoutParams: WindowManager.LayoutParams?, windowManager: WindowManager?) {
        mLayoutParams = layoutParams
        mWindowManager = windowManager
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (mService.getUpdatePlayStateAction() == action) {
                updatePlayState()
            }
        }
    }

    private val hideBgRunnable = Runnable {
        setBgVisible(false)
    }

    private val updateLyricProgress = object : Runnable {
        override fun run() {
            binding.lyricView.updateTime(
                mService.getCurrentPosition().toLong(),
                mService.getDuration().toLong()
            )
            if (mService.isPlaying()) {
                mHandler.postDelayed(this, 100)
            }
        }
    }

    init {
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        setBgVisible(!isLock)
        binding.settingLayout.isVisible = false

        initData()
        initEvent()
        initReceiver()
    }

    private fun setBgVisible(isVisible: Boolean) {
        isBgVisible = isVisible
        binding.topLayout.isInvisible = !isVisible
        binding.bottomLayout.isInvisible = !isVisible
        if (isVisible) {
            binding.contentLayout.setBackgroundColor(
                ContextCompat.getColor(context, R.color.lyric_desktop_bg)
            )
            hideBgDelayed()
        } else {
            binding.contentLayout.setBackgroundColor(
                ContextCompat.getColor(context, R.color.transparent)
            )
            mHandler.removeCallbacks(hideBgRunnable)
            binding.settingLayout.isVisible = false
        }
    }

    private fun hideBgDelayed() {
        if (isBgVisible) {
            mHandler.removeCallbacks(hideBgRunnable)
            mHandler.postDelayed(hideBgRunnable, 5000L)
        }
    }

    private fun initData() {
        setColorIndex(DesktopLyricUtils.getColorIndex(context))
        setTextSize(DesktopLyricUtils.getTextSize(context))
        updatePlayState()
    }

    private fun setColorIndex(index: Int) {
        colorIndex = index
        val color = when (index) {
            0 -> ContextCompat.getColor(context, R.color.lyric_color_01)
            1 -> ContextCompat.getColor(context, R.color.lyric_color_02)
            2 -> ContextCompat.getColor(context, R.color.lyric_color_03)
            3 -> ContextCompat.getColor(context, R.color.lyric_color_04)
            4 -> ContextCompat.getColor(context, R.color.lyric_color_05)
            else -> ContextCompat.getColor(context, R.color.lyric_color_01)
        }
        binding.lyricView.setLightColor(color)
        binding.ivColor01.isSelected = index == 0
        binding.ivColor02.isSelected = index == 1
        binding.ivColor03.isSelected = index == 2
        binding.ivColor04.isSelected = index == 3
        binding.ivColor05.isSelected = index == 4
    }

    private fun setTextSize(size: Float) {
        textSize = size
        binding.lyricView.setTextSize(size)
        if (size == DesktopLyricUtils.minSize) {
            binding.ivTextDecrease.alpha = 0.2f
            binding.ivTextDecrease.isEnabled = false
        } else {
            binding.ivTextDecrease.alpha = 0.5f
            binding.ivTextDecrease.isEnabled = true
        }
        if (size == DesktopLyricUtils.maxSize) {
            binding.ivTextIncrease.alpha = 0.2f
            binding.ivTextIncrease.isEnabled = false
        } else {
            binding.ivTextIncrease.alpha = 0.5f
            binding.ivTextIncrease.isEnabled = true
        }
    }

    private fun updatePlayState() {
        binding.ivPlay.isSelected = mService.isPlaying()
        mHandler.removeCallbacks(updateLyricProgress)
        mHandler.postDelayed(updateLyricProgress, 100)
    }

    private fun initEvent() {
        binding.root.setOnClickListener {
            if (!isLock) {
                setBgVisible(!isBgVisible)
            }
        }

        binding.ivMusic.setOnClickListener {
            mService.getMainClass()?.let {
                val intent = Intent(context, it)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }

        binding.ivClose.setOnClickListener {
            DesktopLyricUtils.getInstance().hide()
            mService.onClosed(context)
        }

        binding.ivLock.setOnClickListener {
            lockLyric(true)
            Toast.makeText(context, R.string.lyric_lock_msg, Toast.LENGTH_SHORT).show()
            mService.onLocked(context)
        }

        binding.ivPrev.setOnClickListener {
            hideBgDelayed()
            mService.playNext(false)
        }

        binding.ivNext.setOnClickListener {
            hideBgDelayed()
            mService.playNext(true)
        }

        binding.ivPlay.setOnClickListener {
            hideBgDelayed()
            mService.playOrPause()
        }

        binding.ivSetting.setOnClickListener {
            hideBgDelayed()
            binding.settingLayout.apply {
                isVisible = !isVisible
            }
        }

        binding.lyricView.setOnLyricListener(object : OnLyricListener() {
            override fun onViewClick(hasLrc: Boolean) {
                if (!isLock) {
                    setBgVisible(!isBgVisible)
                }
            }
        })

        binding.ivColor01.setOnClickListener {
            hideBgDelayed()
            selectColorIndex(0)
        }
        binding.ivColor02.setOnClickListener {
            hideBgDelayed()
            selectColorIndex(1)
        }
        binding.ivColor03.setOnClickListener {
            hideBgDelayed()
            selectColorIndex(2)
        }
        binding.ivColor04.setOnClickListener {
            hideBgDelayed()
            selectColorIndex(3)
        }
        binding.ivColor05.setOnClickListener {
            hideBgDelayed()
            selectColorIndex(4)
        }
        binding.ivTextDecrease.setOnClickListener {
            hideBgDelayed()
            if (textSize > DesktopLyricUtils.minSize) {
                setTextSize(textSize - 1)
                DesktopLyricUtils.saveTextSize(context, textSize - 1)
            }
        }
        binding.ivTextIncrease.setOnClickListener {
            hideBgDelayed()
            if (textSize < DesktopLyricUtils.maxSize) {
                setTextSize(textSize + 1)
                DesktopLyricUtils.saveTextSize(context, textSize + 1)
            }
        }
    }

    fun lockLyric(isLock: Boolean) {
        this.isLock = isLock
        setBgVisible(!isLock)
        if (mWindowManager != null && mLayoutParams != null) {
            if (isLock) { //去掉弹窗拦截点击
                mLayoutParams!!.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            } else {
                mLayoutParams!!.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
            mWindowManager!!.updateViewLayout(this, mLayoutParams)
        }
    }

    private fun selectColorIndex(index: Int) {
        if (colorIndex != index) {
            setColorIndex(index)
            DesktopLyricUtils.saveColorIndex(context, index)
        }
    }

    private fun initReceiver() {
        val filter = IntentFilter()
        filter.addAction(mService.getUpdatePlayStateAction())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(mReceiver, filter)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.removeCallbacksAndMessages(null)
        try {
            context.unregisterReceiver(mReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var mDownY = 0f
    private var mLastY = 0

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mLayoutParams == null) {
            return false
        }
        var intercepted = false
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownY = ev.rawY
                mLastY = mLayoutParams!!.y
            }

            MotionEvent.ACTION_MOVE -> {
                val diffY = ev.rawY - mDownY
                if (abs(diffY) > mTouchSlop) {
                    intercepted = true
                }
            }
        }
        return intercepted
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mLayoutParams == null || mWindowManager == null) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val diffY = event.rawY - mDownY
                if (abs(diffY) > mTouchSlop && !isLock) {
                    hideBgDelayed()
                    mLayoutParams!!.y = (mLastY + diffY).toInt()
                    mWindowManager!!.updateViewLayout(this, mLayoutParams)
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                DesktopLyricUtils.saveWindowY(context, mLayoutParams!!.y)
            }
        }
        return super.onTouchEvent(event)
    }
}