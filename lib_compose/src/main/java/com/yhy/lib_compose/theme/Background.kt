package com.yhy.lib_compose.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * desc:
 **
 * user: xujj
 * time: 2025/5/7 16:58
 **/

@Stable
fun Modifier.dialogBackground(
    color: Color = BgDialog,
    radius: Dp = 15.dp
) = this.background(
    color = color,
    shape = RoundedCornerShape(radius)
)

@Stable
fun Modifier.bottomSheetBackground(
    color: Color = BgDialog,
    radius: Dp = 15.dp
) = this.background(
    color = color,
    shape = RoundedCornerShape(topStart = radius, topEnd = radius)
)

@Stable
fun Modifier.roundButton(
    color: Color = Purple80,
    radius: Dp = 50.dp,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) = this
    .clip(RoundedCornerShape(radius))
    .background(color)
    .clickable(enabled = enabled, onClick = onClick)