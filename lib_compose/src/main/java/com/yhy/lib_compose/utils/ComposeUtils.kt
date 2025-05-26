package com.yhy.lib_compose.utils

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yhy.lib_compose.theme.BaseTheme

/**
 * desc:
 **
 * user: xujj
 * time: 2025/5/7 14:48
 **/

@Composable
fun Modifier.pureClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
) = this.clickable(
    enabled = enabled,
    indication = null,
    interactionSource = remember { MutableInteractionSource() },
    onClick = onClick
)

fun Modifier.roundClickable(
    radius: Dp = 50.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) = this
    .clip(RoundedCornerShape(radius))
    .clickable(enabled = enabled, onClick = onClick)

fun ComponentActivity.addContent(content: @Composable () -> Unit) {
    ComposeView(this).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent(content)
        addContentView(this, DefaultActivityContentLayoutParams)
    }
}

private val DefaultActivityContentLayoutParams = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.WRAP_CONTENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
)

fun ComponentActivity.showDialog(content: @Composable (onDismiss: () -> Unit) -> Unit) {
    addContent {
        BaseTheme {
            val dialogState = DialogState.build(content)
            DisposableEffect(Unit) {
                dialogState.show()
                onDispose {
                    dialogState.dismiss()
                }
            }
        }
    }
}

fun ComponentActivity.buildDialog(build: @Composable () -> DialogState) {
    addContent {
        BaseTheme {
            val dialogState = build()
            DisposableEffect(Unit) {
                dialogState.show()
                onDispose {
                    dialogState.dismiss()
                }
            }
        }
    }
}