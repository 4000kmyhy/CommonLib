package com.yhy.commonlib.ui.theme

import androidx.compose.runtime.Composable
import com.yhy.lib_compose.theme.BaseTheme

@Composable
fun CommonLibTheme(
    content: @Composable () -> Unit
) {
    BaseTheme(content = content)
}