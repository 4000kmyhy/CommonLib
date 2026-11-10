package com.yhy.commonlib

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.yhy.commonlib.utils.FileUtils
import java.io.File

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/7 10:21
 **/
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val sf2File = File(cacheDir.path, "FluidR3_GM.sf2")
        if (!sf2File.exists()) {
            FileUtils.copyAssetFile(this,"FluidR3_GM.sf2",sf2File)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}