package com.yhy.commonlib.piano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yhy.lib_compose.theme.BaseTheme
import com.yhy.lib_compose.theme.setFullScreen

/**
 * desc:
 **
 * user: xujj
 * time: 2025/6/13 11:35
 **/
class PianoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullScreen(window, true)
        enableEdgeToEdge()
        setContent {
            BaseTheme(darkTheme = true) {
                PianoScreen()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setFullScreen(window, true)
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.keepScreenOn = true
    }

    override fun onPause() {
        super.onPause()
        window.decorView.keepScreenOn = false
    }
}

