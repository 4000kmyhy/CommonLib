package com.yhy.commonlib.ui.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yhy.lib_compose.view.SimpleSlider
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2025/9/28 10:08
 **/
@Composable
fun PlayerSlider(
    modifier: Modifier = Modifier,
    currentPosition: Int,
    duration: Int,
    seekTo: (Int) -> Unit,
    onProgressChange: (Float) -> Unit = {},
    colors: SliderColors = SliderDefaults.colors()
) {
//    var value by remember { mutableFloatStateOf(0f) }
//    var isPressed by remember { mutableStateOf(false) }
//    SimpleSlider(
//        modifier = modifier.padding(horizontal = 7.dp),
//        value = if (isPressed) {
//            value
//        } else {
//            if (duration > 0) {
//                1f * currentPosition / duration
//            } else {
//                0f
//            }
//        },
//        onValueChange = {
//            isPressed = true
//            value = it
//        },
//        onValueChangeFinished = {
//            val position = (duration * value).roundToInt()
//            seekTo(position)
//            isPressed = false
//        },
//        colors = SliderDefaults.colors(
//            thumbColor = White,
//            activeTrackColor = White,
//            inactiveTrackColor = White30
//        ),
//        thumbSize = 12.dp
//    )
    PlayerSlider(
        modifier = modifier,
        progress = if (duration > 0) {
            1f * currentPosition / duration
        } else {
            0f
        },
        seekTo = {
            val position = (duration * it).roundToInt()
            seekTo(position)
        },
        onProgressChange = onProgressChange,
        colors = colors
    )
}

@Composable
fun PlayerSlider(
    modifier: Modifier = Modifier,
    progress: Float,
    seekTo: (Float) -> Unit,
    onProgressChange: (Float) -> Unit = {},
    colors: SliderColors = SliderDefaults.colors()
) {
    var value by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }
    SimpleSlider(
        modifier = modifier.padding(horizontal = 7.dp),
        value = if (isPressed) {
            value
        } else {
            progress
        },
        onValueChange = {
            isPressed = true
            value = it
            onProgressChange(it)
        },
        onValueChangeFinished = {
            seekTo(value)
            isPressed = false
        },
        colors = colors,
        thumbSize = 12.dp
    )
}