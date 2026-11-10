package com.yhy.lib_piano.view

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.yhy.lib_piano.BlackKeyColor
import com.yhy.lib_piano.BlackKeyPressedColor
import com.yhy.lib_piano.C4Color
import com.yhy.lib_piano.PianoConstant
import com.yhy.lib_piano.WhiteKeyColor
import com.yhy.lib_piano.WhiteKeyPressedColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * desc:
 **
 * user: xujj
 * time: 2025/6/13 14:39
 **/

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PianoSeekBar(
    modifier: Modifier = Modifier,
    keyNum: Int,
    startProgress: Float,
    onStartProgressChange: (Float) -> Unit,
    playPositions: List<Int> = listOf(),
    tipPositions: List<Int> = listOf(),
) {
    val density = LocalDensity.current

    var mWidth by remember { mutableIntStateOf(0) }
    var mHeight by remember { mutableIntStateOf(0) }
    var whiteKeyWidth by remember { mutableFloatStateOf(0f) }
    var blackKeyWidth by remember { mutableFloatStateOf(0f) }

    val keyPadding = with(density) { 1.dp.toPx() }
    val keyRadius = with(density) { 2.dp.toPx() }

    var whiteKeyRect by remember { mutableStateOf(listOf<Pair<RoundRect, Int>>()) }
    var blackKeyRect by remember { mutableStateOf(listOf<Pair<RoundRect, Int>>()) }

    var keyThumbWidth by remember { mutableFloatStateOf(0f) }
    var thumbStartX by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(mWidth, mHeight, keyNum) {
        if (mWidth > 0 && mHeight > 0) {
            whiteKeyWidth = (mWidth - keyPadding * (PianoConstant.WhiteKeyNum - 1)) /
                    PianoConstant.WhiteKeyNum
            blackKeyWidth = whiteKeyWidth * 2 / 3

            keyThumbWidth = whiteKeyWidth * keyNum + keyPadding * (keyNum - 1)
            thumbStartX = ((whiteKeyWidth + keyPadding) * startProgress).coerceIn(
                0f,
                mWidth - keyThumbWidth
            )

            withContext(Dispatchers.Default) {
                val whiteKeyRectTemp = ArrayList<Pair<RoundRect, Int>>()
                val blackKeyRectTemp = ArrayList<Pair<RoundRect, Int>>()

                repeat(PianoConstant.WhiteKeyNum) { position ->
                    val realPosition = PianoConstant.getRealPosition(position)
                    val whiteKeyX = (whiteKeyWidth + keyPadding) * position
                    whiteKeyRectTemp.add(
                        RoundRect(
                            left = whiteKeyX,
                            top = 0f,
                            right = whiteKeyX + whiteKeyWidth,
                            bottom = mHeight.toFloat(),
                            bottomLeftCornerRadius = CornerRadius(keyRadius),
                            bottomRightCornerRadius = CornerRadius(keyRadius)
                        ) to realPosition
                    )

                    if (PianoConstant.hasBlackKey(position)) {
                        val blackKeyX =
                            whiteKeyX + whiteKeyWidth + keyPadding / 2 - blackKeyWidth / 2
                        blackKeyRectTemp.add(
                            RoundRect(
                                left = blackKeyX,
                                top = 0f,
                                right = blackKeyX + blackKeyWidth,
                                bottom = mHeight / 2f,
                                bottomLeftCornerRadius = CornerRadius(keyRadius),
                                bottomRightCornerRadius = CornerRadius(keyRadius)
                            ) to realPosition + 1
                        )
                    }
                }

                whiteKeyRect = whiteKeyRectTemp
                blackKeyRect = blackKeyRectTemp
            }
        }
    }

    var downX by remember { mutableFloatStateOf(0f) }
    val pressOnPanel: (Float) -> Unit = {
        thumbStartX = (thumbStartX + it).coerceIn(
            0f,
            mWidth - keyThumbWidth
        )
        val newProgress = thumbStartX / (whiteKeyWidth + keyPadding)
        onStartProgressChange(newProgress)
    }

    Canvas(
        modifier = modifier
            .onSizeChanged {
                mWidth = it.width
                mHeight = it.height
            }
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = it.x
                    }

                    MotionEvent.ACTION_MOVE -> {
                        pressOnPanel(it.x - downX)
                        downX = it.x
                    }
                }
                return@pointerInteropFilter true
            }
    ) {
        val whiteKeyPath = Path()
        val whiteKeyPressedPath = Path()
        val whiteKeyTipPath = Path()
        val blackKeyPath = Path()
        val blackKeyPressedPath = Path()
        val blackKeyTipPath = Path()

        whiteKeyRect.forEach {
            whiteKeyPath.addRoundRect(it.first)
            if (playPositions.contains(it.second)) {
                whiteKeyPressedPath.addRoundRect(it.first)
            }
            if (tipPositions.contains(it.second)) {
                whiteKeyTipPath.addRoundRect(it.first)
            }
        }
        blackKeyRect.forEach {
            blackKeyPath.addRoundRect(it.first)
            if (playPositions.contains(it.second)) {
                blackKeyPressedPath.addRoundRect(it.first)
            }
            if (tipPositions.contains(it.second)) {
                blackKeyTipPath.addRoundRect(it.first)
            }
        }

        drawPath(
            path = whiteKeyPath,
            color = WhiteKeyColor
        )
        drawPath(
            path = whiteKeyTipPath,
            color = C4Color.copy(0.5f)
        )
        drawPath(
            path = whiteKeyPressedPath,
            color = WhiteKeyPressedColor
        )
        drawPath(
            path = blackKeyPath,
            color = BlackKeyColor
        )
        drawPath(
            path = blackKeyTipPath,
            color = C4Color.copy(0.5f)
        )
        drawPath(
            path = blackKeyPressedPath,
            color = BlackKeyPressedColor
        )

        val c4KeyX = (whiteKeyWidth + keyPadding) * PianoConstant.getCPosition(group = 4)
        drawCircle(
            color = C4Color,
            radius = whiteKeyWidth / 4,
            center = Offset(c4KeyX + whiteKeyWidth / 2, size.height - whiteKeyWidth / 2)
        )

        clipRect(
            left = thumbStartX,
            right = thumbStartX + keyThumbWidth,
            clipOp = ClipOp.Difference
        ) {
            drawRect(color = Color.Black.copy(0.5f))
        }
    }
}