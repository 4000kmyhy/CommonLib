package com.lib.bass.cutter.viewModel

/**
 * desc:
 **
 * user: xujj
 * time: 2024/4/8 10:30
 **/
interface OnCutterSaveCallback {
    fun onProgressUpdated(progress: Int)
    fun onCompletion()
}