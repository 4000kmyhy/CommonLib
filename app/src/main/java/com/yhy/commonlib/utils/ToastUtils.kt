package com.yhy.commonlib.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * desc:
 **
 * user: xujj
 * time: 2022/8/10 17:16
 **/
object ToastUtils {

    @JvmStatic
    fun showToast(
        context: Context,
        text: CharSequence,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        runOnUi {
            Toast.makeText(context, text, duration).show()
        }
    }

    @JvmStatic
    fun showToast(
        context: Context,
        @StringRes resId: Int,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        runOnUi {
            Toast.makeText(context, resId, duration).show()
        }
    }

    @JvmStatic
    fun showToast(
        context: Context,
        @StringRes resId: Int,
        vararg formatArgs: Any,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        runOnUi {
            Toast.makeText(context, context.getString(resId, *formatArgs), duration).show()
        }
    }

    suspend fun suspendToast(
        context: Context,
        text: CharSequence,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, text, duration).show()
        }
    }

    suspend fun suspendToast(
        context: Context,
        @StringRes resId: Int,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, resId, duration).show()
        }
    }

    suspend fun suspendToast(
        context: Context,
        @StringRes resId: Int,
        vararg formatArgs: Any,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(resId, *formatArgs), duration).show()
        }
    }

    private fun runOnUi(runnable: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            Handler(Looper.getMainLooper()).post(runnable)
        }
    }
}