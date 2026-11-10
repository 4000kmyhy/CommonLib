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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yhy.lib_piano.BlackKeyColor
import com.yhy.lib_piano.BlackKeyPressedColor
import com.yhy.lib_piano.C4Color
import com.yhy.lib_piano.PianoConstant
import com.yhy.lib_piano.WhiteKeyColor
import com.yhy.lib_piano.WhiteKeyPressedColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * desc:
 **
 * user: xujj
 * time: 2025/6/13 16:03
 **/

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PianoView(
    modifier: Modifier = Modifier,
    keyNum: Int,
    startProgress: Float,
    playPositions: List<Int> = listOf(),
    tipPositions: List<Int> = listOf(),
    keyOn: (Int) -> Unit,
    keyOff: (Int) -> Unit,
    updatePlayPositions: (List<Int>) -> Unit,
    showKey: Boolean = true,
    showGroup: Boolean = true,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var mWidth by remember { mutableIntStateOf(0) }
    var mHeight by remember { mutableIntStateOf(0) }
    var whiteKeyWidth by remember { mutableFloatStateOf(0f) }
    var blackKeyWidth by remember { mutableFloatStateOf(0f) }
    var mOffset by remember { mutableFloatStateOf(0f) }

    val keyPadding = with(density) { 3.dp.toPx() }
    val keyRadius = with(density) { 5.dp.toPx() }

    var keyRect by remember { mutableStateOf(listOf<RoundRect>()) }
    var pressedKeyRect by remember { mutableStateOf(listOf<Rect>()) }

    LaunchedEffect(mWidth, mHeight, keyNum) {
        if (mWidth > 0 && mHeight > 0) {
            whiteKeyWidth = (mWidth - keyPadding * (keyNum - 1)) / keyNum
            blackKeyWidth = whiteKeyWidth * 2 / 3

            withContext(Dispatchers.Default) {
                val keyRectTemp = ArrayList<RoundRect>()
                val pressedKeyRectTemp = ArrayList<Rect>()

                repeat(PianoConstant.WhiteKeyNum) { position ->
                    val whiteKeyX = (whiteKeyWidth + keyPadding) * position
                    keyRectTemp.add(
                        RoundRect(
                            left = whiteKeyX,
                            top = 0f,
                            right = whiteKeyX + whiteKeyWidth,
                            bottom = mHeight.toFloat(),
                            bottomLeftCornerRadius = CornerRadius(keyRadius),
                            bottomRightCornerRadius = CornerRadius(keyRadius)
                        )
                    )

                    pressedKeyRectTemp.add(
                        Rect(
                            left = whiteKeyX,
                            top = mHeight / 2f,
                            right = whiteKeyX + whiteKeyWidth,
                            bottom = mHeight.toFloat()
                        )
                    )

                    if (PianoConstant.hasBlackKey(position)) {
                        val blackKeyX =
                            whiteKeyX + whiteKeyWidth + keyPadding / 2 - blackKeyWidth / 2
                        keyRectTemp.add(
                            RoundRect(
                                left = blackKeyX,
                                top = 0f,
                                right = blackKeyX + blackKeyWidth,
                                bottom = mHeight / 2f,
                                bottomLeftCornerRadius = CornerRadius(keyRadius),
                                bottomRightCornerRadius = CornerRadius(keyRadius)
                            )
                        )

                        pressedKeyRectTemp.add(
                            Rect(
                                left = whiteKeyX + whiteKeyWidth / 2 + keyPadding / 2,
                                top = 0f,
                                right = whiteKeyX + whiteKeyWidth / 2 + keyPadding / 2 + whiteKeyWidth,
                                bottom = mHeight / 2f
                            )
                        )
                    }
                }

                keyRect = keyRectTemp
                pressedKeyRect = pressedKeyRectTemp
            }
        }
    }

    LaunchedEffect(whiteKeyWidth, startProgress) {
        mOffset = (whiteKeyWidth + keyPadding) * startProgress
    }

    var pointers by remember { mutableStateOf<Map<Int, Int>>(mapOf()) }

    Canvas(
        modifier = modifier
            .onSizeChanged {
                mWidth = it.width
                mHeight = it.height
            }
            .pointerInteropFilter {
                when (it.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        val pointerId = it.getPointerId(it.actionIndex)
                        val position = pressedKeyRect.indexOfFirst { rect ->
                            rect.contains(
                                Offset(
                                    it.getX(it.actionIndex) + mOffset,
                                    it.getY(it.actionIndex)
                                )
                            )
                        }
                        if (position >= 0) {
                            pointers = pointers.toMutableMap().apply {
                                put(pointerId, position)
                            }
                            keyOn(position)
                            updatePlayPositions(pointers.values.toList())
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        for (i in 0 until it.pointerCount) {
                            val pointerId = it.getPointerId(i)
                            val position = pressedKeyRect.indexOfFirst { rect ->
                                rect.contains(
                                    Offset(
                                        it.getX(i) + mOffset,
                                        it.getY(i)
                                    )
                                )
                            }
                            if (position >= 0 && pointers[pointerId] != position) {
                                val lastPosition = pointers[pointerId]
                                if (lastPosition != null && lastPosition >= 0) {
                                    keyOff(lastPosition)
                                }
                                pointers = pointers.toMutableMap().apply {
                                    put(pointerId, position)
                                }
                                keyOn(position)
                                updatePlayPositions(pointers.values.toList())
                            }
                        }
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_POINTER_UP -> {
                        val pointerId = it.getPointerId(it.actionIndex)
                        pointers = pointers.toMutableMap().apply {
                            val position = remove(pointerId)
                            if (position != null && position >= 0) {
                                keyOff(position)
                            }
                        }
                        updatePlayPositions(pointers.values.toList())
                    }
                }
                when (it.action) {
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        pointers.forEach {
                            keyOff(it.value)
                        }
                        pointers = mapOf()
                        updatePlayPositions(listOf())
                    }
                }
                return@pointerInteropFilter true
            }
    ) {
        translate(left = -mOffset) {
            val whiteKeyPath = Path()
            val whiteKeyPressedPath = Path()
            val whiteKeyTipPath = Path()
            val blackKeyPath = Path()
            val blackKeyPressedPath = Path()
            val blackKeyTipPath = Path()

            keyRect.forEachIndexed { index, rect ->
                if (overlaps(
                        itemStart = rect.left - mOffset,
                        itemEnd = rect.right - mOffset,
                        visibleStart = 0f,
                        visibleEnd = size.width
                    )
                ) {
                    val isWhiteKey = PianoConstant.isWhiteKey(index)
                    if (isWhiteKey) {
                        whiteKeyPath.addRoundRect(rect)
                    } else {
                        blackKeyPath.addRoundRect(rect)
                    }
                    if (playPositions.contains(index)) {
                        if (isWhiteKey) {
                            whiteKeyPressedPath.addRoundRect(rect)
                        } else {
                            blackKeyPressedPath.addRoundRect(rect)
                        }
                    }
                    if (tipPositions.contains(index)) {
                        if (isWhiteKey) {
                            whiteKeyTipPath.addRoundRect(rect)
                        } else {
                            blackKeyTipPath.addRoundRect(rect)
                        }
                    }
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

            if (showKey) {
                keyRect.forEachIndexed { index, rect ->
                    if (overlaps(
                            itemStart = rect.left - mOffset,
                            itemEnd = rect.right - mOffset,
                            visibleStart = 0f,
                            visibleEnd = size.width
                        )
                    ) {
                        val isWhiteKey = PianoConstant.isWhiteKey(index)
                        if (isWhiteKey) {
                            //音符名
                            val keyNote = PianoConstant.getKeyNote(index, showGroup)
                            val noteText = textMeasurer.measure(
                                text = keyNote,
                                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            drawText(
                                textLayoutResult = noteText,
                                color = if (index == PianoConstant.C4Position) {
                                    C4Color
                                } else {
                                    if (isWhiteKey) Color.Black else Color.White
                                },
                                topLeft = Offset(
                                    rect.left + rect.width / 2 - noteText.size.width / 2f,
                                    rect.bottom - rect.width * 0.1f - noteText.size.height
                                )
                            )
                        }


                    }
                }
            }
        }
    }
}

private fun overlaps(
    itemStart: Float,
    itemEnd: Float,
    visibleStart: Float,
    visibleEnd: Float
): Boolean {
    return max(itemStart, visibleStart) <= min(itemEnd, visibleEnd)
}