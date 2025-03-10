package com.lib.lib_lyric.utils

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.lib.lib_lyric.view.DesktopLyricView

/**
 * desc:
 **
 * user: xujj
 * time: 2024/11/7 11:07
 **/
class DesktopLyricUtils {

    companion object {

        private const val TAG = "DesktopLyricUtils"

        @Volatile
        private var instance: DesktopLyricUtils? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: DesktopLyricUtils().also { instance = it }
        }

        var isBackground = false

        fun init(callback: (Boolean) -> Unit) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_START) {
                        isBackground = false
                        callback(false)
                    } else if (event == Lifecycle.Event.ON_STOP) {
                        isBackground = true
                        callback(true)
                    }
                }
            })
        }

        private fun Context.getSp() = getSharedPreferences(TAG, Context.MODE_PRIVATE)

        private const val KEY_WINDOW_Y = "window_y"
        private const val KEY_COLOR_INDEX = "color_index"
        private const val KEY_TEXT_SIZE = "text_size"

        const val defaultSize = 18f
        const val minSize = defaultSize - 2
        const val maxSize = defaultSize + 2

        fun saveWindowY(context: Context, y: Int) {
            context.getSp().edit().putInt(KEY_WINDOW_Y, y).apply()
        }

        private fun getWindowY(context: Context): Int {
            return context.getSp().getInt(KEY_WINDOW_Y, 0)
        }

        fun saveColorIndex(context: Context, index: Int) {
            context.getSp().edit().putInt(KEY_COLOR_INDEX, index).apply()
        }

        fun getColorIndex(context: Context): Int {
            return context.getSp().getInt(KEY_COLOR_INDEX, 0)
        }

        fun saveTextSize(context: Context, textSize: Float) {
            if (textSize in minSize..maxSize) {
                context.getSp().edit().putFloat(KEY_TEXT_SIZE, textSize).apply()
            }
        }

        fun getTextSize(context: Context): Float {
            return context.getSp().getFloat(KEY_TEXT_SIZE, defaultSize)
        }
    }

    private var mWindowManager: WindowManager? = null
    private var mLayoutParams: WindowManager.LayoutParams? = null
    private var mDesktopLyricView: DesktopLyricView? = null

    var mService = object : DesktopLyricInterface {
        override fun isPlaying() = false
        override fun getCurrentPosition() = 0
        override fun getDuration() = 0

        override fun playOrPause() {}
        override fun playNext(isNext: Boolean) {}
        override fun onClosed(context: Context) {}
        override fun onLocked(context: Context) {}

        override fun getMainClass() = null
        override fun getUpdatePlayStateAction() = ""
    }

    fun show(context: Context, isLock: Boolean) {
        if (!isBackground) return
        if (mWindowManager == null) {
            mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }
        if (mDesktopLyricView == null) {
            mDesktopLyricView = DesktopLyricView(context, isLock = isLock)
            if (mLayoutParams == null) {
                mLayoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mLayoutParams!!.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    mLayoutParams!!.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
                mLayoutParams!!.format = PixelFormat.RGBA_8888
                if (isLock) { //去掉弹窗拦截点击
                    mLayoutParams!!.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                } else {
                    mLayoutParams!!.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                }
                mLayoutParams!!.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                mLayoutParams!!.width = WindowManager.LayoutParams.MATCH_PARENT
                mLayoutParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT
                mLayoutParams!!.y = getWindowY(context)
            }
            mDesktopLyricView?.setParams(mLayoutParams, mWindowManager)
            addDesktopView()
        }
    }

    private fun addDesktopView() {
        try {
            if (mDesktopLyricView != null && mWindowManager != null && mLayoutParams != null) {
                mWindowManager!!.addView(mDesktopLyricView, mLayoutParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeDesktopView() {
        try {
            if (mDesktopLyricView != null && mWindowManager != null) {
                mWindowManager!!.removeViewImmediate(mDesktopLyricView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide() {
        removeDesktopView()
        mDesktopLyricView = null
        mLayoutParams = null
        mWindowManager = null
    }

    fun unlock() {
        mDesktopLyricView?.lockLyric(false)
    }
}