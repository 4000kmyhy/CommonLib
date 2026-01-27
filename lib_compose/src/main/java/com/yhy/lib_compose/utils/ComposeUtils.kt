package com.yhy.lib_compose.utils

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yhy.lib_compose.theme.ComposeTheme
import kotlin.math.max
import kotlin.math.min

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

fun Modifier.isInvisible(value: Boolean) = if (value) this.then(Invisible) else this

private object Invisible : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {}
    }
}

fun Modifier.isGone(value: Boolean) = if (value) this.then(Gone) else this

private object Gone : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        return layout(0, 0) {}
    }
}

@Composable
fun rememberImeVisibleState(): State<Boolean> {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime

    return remember(density, imeInsets) {
        derivedStateOf {
            val imeBottom = imeInsets.getBottom(density)
            imeBottom > 0
        }
    }
}

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
        ComposeTheme {
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
        ComposeTheme {
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

fun String.toTextFieldValue() = let {
    TextFieldValue(it, TextRange(it.length))
}

fun processValueChange(
    value: TextFieldValue,
    newValue: TextFieldValue,
    maxLength: Int = Int.MAX_VALUE,
    onValueChange: (TextFieldValue) -> Unit
) {
    val start = min(value.selection.start, value.selection.end)
    val end = max(value.selection.start, value.selection.end)
    val newStart = min(newValue.selection.start, newValue.selection.end)
    val newEnd = max(newValue.selection.start, newValue.selection.end)
    if (maxLength == Int.MAX_VALUE ||
        newValue.text.length <= maxLength ||
        value.text.length >= newValue.text.length || //内容长度没有增加
        start >= newStart || //光标往前走，应该是删除内容
        newStart != newEnd //不是输入内容，应该是选中内容
    ) {
        onValueChange(newValue)
    } else {
        val str1 = value.text.substring(0, start)
        val str2 = value.text.substring(end, value.text.length)
        val inputValue = newValue.text.substring(start, newStart)

        val remainingLength = maxLength - str1.length - str2.length
        val text = if (remainingLength > 0) {
            inputValue.take(remainingLength)
        } else {
            ""
        }

        onValueChange(
            TextFieldValue(
                text = str1 + text + str2,
                selection = TextRange(start + text.length)
            )
        )
    }
}