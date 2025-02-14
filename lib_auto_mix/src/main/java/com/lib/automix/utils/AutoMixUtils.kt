package com.lib.automix.utils

import android.app.ActivityManager
import android.content.Context
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.lib.automix.R

/**
 * desc:
 **
 * user: xujj
 * time: 2024/7/23 10:37
 **/

fun isServiceRunning(context: Context?, serviceName: String?): Boolean {
    if (context == null) return false
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (TextUtils.equals(context.packageName, service.service.packageName) &&
            TextUtils.equals(serviceName, service.service.className)
        ) {
            return true
        }
    }
    return false
}

fun stringForTime(millisecond: Long): String {
    val second = millisecond / 1000
    val hh = second / 3600
    val mm = second % 3600 / 60
    val ss = second % 60
    return if (hh != 0L) {
        try {
            String.format("%02d:%02d:%02d", hh, mm, ss)
        } catch (e: OutOfMemoryError) {
            "00:00:00"
        }
    } else {
        try {
            String.format("%02d:%02d", mm, ss)
        } catch (e: OutOfMemoryError) {
            "00:00"
        }
    }
}

fun Lifecycle.isResumed() = currentState == Lifecycle.State.RESUMED

@ColorInt
fun getAccentColor(context: Context, isDeckA: Boolean): Int {
    return ContextCompat.getColor(
        context,
        if (isDeckA) R.color.colorAccentA else R.color.colorAccentB
    )
}