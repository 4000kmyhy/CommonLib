package com.yhy.commonlib.ui.dialog

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yhy.commonlib.ui.theme.Purple80

/**
 * desc:
 **
 * user: xujj
 * time: 2025/8/6 14:33
 **/
@Composable
fun LoadingDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        CircularProgressIndicator(color = Purple80)
    }
}