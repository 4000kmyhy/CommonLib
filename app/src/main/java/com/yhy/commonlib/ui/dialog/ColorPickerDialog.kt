package com.yhy.commonlib.ui.dialog

import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.Shader
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yhy.commonlib.ui.theme.Purple80
import kotlin.math.ceil


/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/29 11:15
 **/

@Composable
fun rememberDialogState() = remember { DialogState() }

class DialogState {
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

@Composable
fun buildColorPicker(): DialogState {
    val dialogState = rememberDialogState()
    ColorPickerDialog(
        dialogState,
        Purple80.copy(0.5f),
        onColorChanged = {}
    )
    return dialogState
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColorPickerDialog(
    dialogState: DialogState = rememberDialogState(),
    oldColor: Color,
    onColorChanged: (Color) -> Unit,
    heightScale: Float = 2f
) {
    if (dialogState.isShow) {
        val hsv by remember(oldColor) {
            derivedStateOf {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(oldColor.toArgb(), hsv)
                hsv
            }
        }

        var mHue by remember { mutableFloatStateOf(hsv[0]) }
        var mSat by remember { mutableFloatStateOf(hsv[1]) }
        var mVal by remember { mutableFloatStateOf(hsv[2]) }
        var mAlpha by remember { mutableFloatStateOf(oldColor.alpha) }

        var newColor by remember { mutableStateOf(oldColor) }

        val updateColor: () -> Unit = {
            val color = Color.hsv(mHue, mSat, mVal, mAlpha)
            if (newColor != color) {
                newColor = color
                onColorChanged(color)
            }
        }

        var satValWidth by remember { mutableIntStateOf(0) }
        var satValHeight by remember { mutableIntStateOf(0) }
        val pressOnSatValPanel: (Float, Float) -> Unit = { x, y ->
            if (satValWidth > 0 && satValHeight > 0) {
                mSat = x.coerceIn(0f, satValWidth.toFloat()) / satValWidth
                mVal = 1f - y.coerceIn(0f, satValHeight.toFloat()) / (satValHeight * heightScale)
                updateColor()
            }
        }

        var hueSize by remember { mutableIntStateOf(0) }
        val pressOnHuePanel: (Float) -> Unit = {
            if (hueSize > 0) {
                mHue = 360f * (1 - it / hueSize).coerceIn(0f, 1f)
                updateColor()
            }
        }

        var alphaSize by remember { mutableIntStateOf(0) }
        val pressOnAlphaPanel: (Float) -> Unit = {
            if (alphaSize > 0) {
                mAlpha = (1 - it / alphaSize).coerceIn(0f, 1f)
                updateColor()
            }
        }

        Dialog(onDismissRequest = dialogState::dismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF303030),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(15.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(heightScale)
                        .onSizeChanged {
                            satValWidth = it.width
                            satValHeight = it.height
                        }
                        .pointerInteropFilter {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    pressOnSatValPanel(it.x, it.y)
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    pressOnSatValPanel(it.x, it.y)
                                }
                            }
                            return@pointerInteropFilter true
                        }
                ) {
                    val rgb = Color.hsv(mHue, 1.0f, 1.0f)
                    val horizontalShader = LinearGradient(
                        0f, 0f, size.width, 0f,
                        Color(0xFFFFFFFF).toArgb(),
                        rgb.toArgb(),
                        Shader.TileMode.CLAMP
                    )
                    val verticalShader = LinearGradient(
                        0f, 0f, 0f, size.height * heightScale,
                        Color(0xFFFFFFFF).toArgb(),
                        Color(0xFF000000).toArgb(),
                        Shader.TileMode.CLAMP
                    )
                    val composeShader = ComposeShader(
                        horizontalShader,
                        verticalShader,
                        PorterDuff.Mode.MULTIPLY
                    )
                    drawRect(brush = ShaderBrush(composeShader))

//                    val px = mSat * size.width
//                    val py = (1f - mVal) * size.height
                    val px = mSat * size.width
                    val py = (1f - mVal) * size.height * heightScale
                    drawCircle(
                        color = Color.hsv(mHue, mSat, mVal),
                        radius = 10.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 9.dp.toPx(),
                        center = Offset(px, py),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                Canvas(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(30.dp)
                        .onSizeChanged {
                            hueSize = it.width
                        }
                        .pointerInteropFilter {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    pressOnHuePanel(it.x)
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    pressOnHuePanel(it.x)
                                }
                            }
                            return@pointerInteropFilter true
                        }
                ) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = getHueColors()
                        )
                    )
                    val px = size.width * (1 - mHue / 360f)
                    drawLine(
                        color = Color.White,
                        start = Offset(px, 0f),
                        end = Offset(px, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                Canvas(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(30.dp)
                        .onSizeChanged {
                            alphaSize = it.width
                        }
                        .pointerInteropFilter {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    pressOnAlphaPanel(it.x)
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    pressOnAlphaPanel(it.x)
                                }
                            }
                            return@pointerInteropFilter true
                        }
                ) {
                    drawAlphaPattern(size.height / 4)
                    val color = Color.hsv(mHue, mSat, mVal)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(color, Color.Transparent)
                        )
                    )
                    val px = size.width * (1 - mAlpha)
                    drawLine(
                        color = Color.Black,
                        start = Offset(px, 0f),
                        end = Offset(px, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    ) {
                        if (oldColor.alpha < 1) {
                            drawAlphaPattern(size.height / 4)
                        }
                        drawRect(color = oldColor)
                    }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    ) {
                        if (newColor.alpha < 1) {
                            drawAlphaPattern(size.height / 4)
                        }
                        drawRect(color = newColor)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = oldColor.toHex(),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = newColor.toHex(),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun getHueColors(): List<Color> {
    val hueColors = ArrayList<Color>()
    for (i in 0..360) {
        val color = Color.hsv(360f - i, 1.0f, 1.0f)
        hueColors.add(color)
    }
    return hueColors
}

private fun DrawScope.drawAlphaPattern(rectangleSize: Float) {
    clipRect {
        val numRectanglesHorizontal = ceil((size.width / rectangleSize)).toInt()
        val numRectanglesVertical = ceil(size.height / rectangleSize).toInt()
        val white = Color(0xFFFFFFFF)
        val gray = Color(0xFFCBCBCB)

        var verticalStartWhite = true
        for (i in 0..numRectanglesVertical) {
            var isWhite = verticalStartWhite
            for (j in 0..numRectanglesHorizontal) {
                drawRect(
                    color = if (isWhite) white else gray,
                    topLeft = Offset(rectangleSize * j, rectangleSize * i),
                    size = Size(rectangleSize, rectangleSize)
                )
                isWhite = !isWhite
            }
            verticalStartWhite = !verticalStartWhite
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun Color.toHex(): String {
    return this.toArgb().toHexString().uppercase()
}