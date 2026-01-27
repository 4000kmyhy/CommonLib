package com.yhy.lib_compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * desc:
 **
 * user: xujj
 * time: 2025/5/7 15:50
 **/
@Composable
fun rememberDialogState(
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current
) = remember(keyboardController) {
    DialogState(keyboardController)
}

open class DialogState(
    private val keyboardController: SoftwareKeyboardController?
) {

    companion object {
        @Composable
        fun build(content: @Composable (onDismiss: () -> Unit) -> Unit): DialogState {
            val dialogState = rememberDialogState()
            if (dialogState.isShow) {
                content(dialogState::dismiss)
            }
            return dialogState
        }

        suspend fun DialogState.doAfter(func: suspend () -> Unit) {
            show()
            func()
            dismiss()
        }
    }

    constructor() : this(null)

    private var isShowDialog by mutableStateOf(false)

    open fun show() {
        keyboardController?.hide()
        isShowDialog = true
    }

    open fun dismiss() {
        isShowDialog = false
    }

    val isShow: Boolean
        get() = isShowDialog
}

@Composable
fun rememberListDialogState() = remember { ListDialogState() }

open class ListDialogState {

    companion object {
        @Composable
        fun ListDialogState.Content(
            index: Int,
            content: @Composable (onDismiss: () -> Unit) -> Unit
        ) {
            if (isShow(index)) {
                content({ dismiss() })
            }
        }
    }

    private var showIndex by mutableIntStateOf(-1)

    open fun show(index: Int) {
        showIndex = index
    }

    open fun dismiss() {
        showIndex = -1
    }

    open fun isShow(index: Int) = showIndex == index
}