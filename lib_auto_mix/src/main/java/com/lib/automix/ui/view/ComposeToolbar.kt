package com.lib.automix.ui.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import com.lib.automix.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeToolbar(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    navigationIconColor: Color = Color.Unspecified,
    actionsIconColor: Color = Color.Unspecified,
    action1IconColor: Color = actionsIconColor,
    action2IconColor: Color = actionsIconColor,
    action3IconColor: Color = actionsIconColor,
    titleColor: Color = Color.Unspecified,
    subTitleColor: Color = titleColor,
    titleResource: Int = -1,
    title: String? = if (titleResource >= 0) stringResource(id = titleResource) else null,
    titleSize: TextUnit = TextUnit.Unspecified,
    titleWeight: FontWeight? = null,
    subtitleResource: Int = -1,
    subtitle: String? = if (subtitleResource >= 0) stringResource(id = subtitleResource) else null,
    subtitleSize: TextUnit = TextUnit.Unspecified,
    navigationPainter: Painter? = null,
    action1Painter: Painter? = null,
    action2Painter: Painter? = null,
    action3Painter: Painter? = null,
    onNavigationClick: () -> Unit = {},
    onAction1Click: () -> Unit = {},
    onAction2Click: () -> Unit = {},
    onAction3Click: () -> Unit = {},
    adContent: @Composable () -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                if (!title.isNullOrEmpty()) {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = titleColor,
                        fontSize = titleSize,
                        fontWeight = titleWeight
                    )
                }
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = subTitleColor,
                        fontSize = subtitleSize
                    )
                }
            }
        },
        navigationIcon = {
            ToolbarIcon(
                painter = navigationPainter,
                tint = navigationIconColor,
                onClick = onNavigationClick
            )
        },
        actions = {
            adContent()
            ToolbarIcon(
                painter = action1Painter,
                tint = action1IconColor,
                onClick = onAction1Click
            )
            ToolbarIcon(
                painter = action2Painter,
                tint = action2IconColor,
                onClick = onAction2Click
            )
            ToolbarIcon(
                painter = action3Painter,
                tint = action3IconColor,
                onClick = onAction3Click
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}

@Composable
fun ToolbarIcon(
    painter: Painter? = null,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit = {}
) {
    painter?.let {
        IconButton(onClick = onClick) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}

@Composable
fun getPainter(
    resource: Int = -1,
    imageVector: ImageVector? = null,
    bitmap: ImageBitmap? = null
): Painter? {
    return if (resource >= 0) {
        painterResource(id = resource)
    } else if (imageVector != null) {
        rememberVectorPainter(imageVector)
    } else if (bitmap != null) {
        remember(bitmap) { BitmapPainter(bitmap) }
    } else {
        null
    }
}

@Preview
@Composable
private fun ComposeToolbarPreview() {
    ComposeToolbar(
        titleResource = R.string.stop_auto_mix,
        navigationPainter = getPainter(imageVector = Icons.Default.Menu),
        action1Painter = getPainter(imageVector = Icons.Default.MoreVert),
    )
}