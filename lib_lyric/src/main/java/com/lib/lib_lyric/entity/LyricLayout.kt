package com.lib.lib_lyric.entity

import android.text.StaticLayout

/**
 * desc:
 **
 * user: xujj
 * time: 2024/2/22 11:36
 **/
class LyricLayout(var staticLayout: StaticLayout) {

    var offset: Float = Float.MIN_VALUE

    fun getHeight() = staticLayout.height
}