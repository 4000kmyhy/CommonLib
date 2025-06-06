package com.yhy.lib_compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * desc:
 **
 * user: xujj
 * time: 2025/5/7 15:50
 **/
@Composable
fun rememberDialogState() = remember { DialogState() }

open class DialogState {

    companion object {
        @Composable
        fun build(content: @Composable (onDismiss: () -> Unit) -> Unit): DialogState {
            val dialogState = rememberDialogState()
            if (dialogState.isShow) {
                content(dialogState::dismiss)
            }
            return dialogState
        }
    }

    private var isShowDialog by mutableStateOf(false)

    fun show() {
        isShowDialog = true
    }

    fun dismiss() {
        isShowDialog = false
    }

    val isShow: Boolean
        get() = isShowDialog
}