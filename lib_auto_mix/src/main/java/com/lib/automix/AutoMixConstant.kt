package com.lib.automix

import android.content.Context

/**
 * desc:
 **
 * user: xujj
 * time: 2024/7/23 9:54
 **/
object AutoMixConstant {

    private const val KEY_SHUFFLE_MODE = "KEY_SHUFFLE_MODE"
    private const val KEY_FADE_TIME = "KEY_FADE_TIME"
    private const val KEY_FADE_END_TIME = "KEY_FADE_END_TIME"
    private const val KEY_SYNC_MODE = "KEY_SYNC_MODE"

    fun setShuffleModeSP(context: Context, mode: Boolean) {
        setSP(context, KEY_SHUFFLE_MODE, mode)
    }

    fun getShuffleModeSP(context: Context): Boolean {
        return getSP(context, KEY_SHUFFLE_MODE, false) as Boolean
    }

    fun setFadeTimeSP(context: Context, time: Long) {
        setSP(context, KEY_FADE_TIME, time)
    }

    fun getFadeTimeSP(context: Context): Long {
        return getSP(context, KEY_FADE_TIME, 10 * 1000L) as Long
    }

    fun setFadeEndTimeSP(context: Context, time: Long) {
        setSP(context, KEY_FADE_END_TIME, time)
    }

    fun getFadeEndTimeSP(context: Context): Long {
        return getSP(context, KEY_FADE_END_TIME, 30 * 1000L) as Long
    }

    fun setSyncModeSP(context: Context, mode: Boolean) {
        setSP(context, KEY_SYNC_MODE, mode)
    }

    fun getSyncModeSP(context: Context): Boolean {
        return getSP(context, KEY_SYNC_MODE, true) as Boolean
    }

    private const val SHARE_DEF_NAME = "AutoMix"

    private fun getSP(context: Context?, key: String, defValue: Any): Any {
        if (context == null) return defValue
        val sp = context.getSharedPreferences(SHARE_DEF_NAME, Context.MODE_PRIVATE)
        when (defValue) {
            is String -> {
                return sp.getString(key, defValue) ?: defValue
            }

            is Boolean -> {
                return sp.getBoolean(key, defValue)
            }

            is Int -> {
                return sp.getInt(key, defValue)
            }

            is Long -> {
                return sp.getLong(key, defValue)
            }

            is Float -> {
                return sp.getFloat(key, defValue)
            }

            else -> return defValue
        }
    }

    private fun setSP(context: Context?, key: String, value: Any) {
        if (context == null) return
        val sp = context.getSharedPreferences(SHARE_DEF_NAME, Context.MODE_PRIVATE)
        when (value) {
            is String -> {
                sp.edit().putString(key, value).apply()
            }

            is Boolean -> {
                sp.edit().putBoolean(key, value).apply()
            }

            is Int -> {
                sp.edit().putInt(key, value).apply()
            }

            is Long -> {
                sp.edit().putLong(key, value).apply()
            }

            is Float -> {
                sp.edit().putFloat(key, value).apply()
            }
        }
    }
}