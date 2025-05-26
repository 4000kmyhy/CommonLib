package com.yhy.commonlib

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/7 10:21
 **/
class MyApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}