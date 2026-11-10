package com.yhy.commonlib.voice

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/16 17:38
 **/
@Composable
fun WaveformView(
    modifier: Modifier = Modifier,
    dataList: List<Int>,
    currentPosition: Int,
    duration: Int,
    startPosition: Int,
    endPosition: Int,
    extractPosition: (Int, Int) -> Unit,
    onSeekTo: (Int) -> Unit,
    minDuration: Long = 10000L,
    totalSize: Int = 100
) {
    var width by remember { mutableIntStateOf(0) }
    var downType by remember { mutableIntStateOf(0) }//1: start, 2: end, 3: center
    var downX by remember { mutableFloatStateOf(0f) }
    var isMoved by remember { mutableStateOf(false) }

    val requestDisallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()

    val density = LocalDensity.current
    val borderWidth = with(density) { 1.dp.toPx() }
    val controllerWidth = with(density) { 12.dp.toPx() }

    var newStart by remember(startPosition) { mutableIntStateOf(startPosition) }
    var newEnd by remember(endPosition) { mutableIntStateOf(endPosition) }

    val positionToX: (Int) -> Float = {
        if (duration > 0) {
            controllerWidth + (width - controllerWidth * 2) * it / duration
        } else {
            0f
        }
    }

    val xToPosition: (Float) -> Int = {
        if (width > 0) {
            (duration * it / width).roundToInt()
        } else {
            0
        }
    }

    val setMoveOffset: (Float) -> Unit = { offsetX ->
        val offsetPosition = xToPosition(offsetX)
        if (offsetPosition != 0) {
            when (downType) {
                1 -> {
                    if (newEnd - minDuration >= 0) {
                        newStart = (newStart + offsetPosition).coerceIn(
                            0,
                            newEnd - minDuration.toInt()
                        )
                        extractPosition(newStart, newEnd)
                    }
                }

                2 -> {
                    if (newStart + minDuration <= duration) {
                        newEnd = (newEnd + offsetPosition).coerceIn(
                            newStart + minDuration.toInt(),
                            duration
                        )
                        extractPosition(newStart, newEnd)
                    }
                }

                3 -> {
                    if (minDuration <= duration) {
                        val diffPosition = (newEnd - newStart).coerceIn(
                            minDuration.toInt(),
                            duration
                        )
                        if (duration - diffPosition >= 0) {
                            newStart = (newStart + offsetPosition).coerceIn(
                                0,
                                duration - diffPosition
                            )
                            newEnd = newStart + diffPosition
                            extractPosition(newStart, newEnd)
                        }
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged {
                width = it.width
            }
            .pointerInteropFilter(
                requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
            ) {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (duration > 0) {
                            val startX = positionToX(newStart)
                            val endX = positionToX(newEnd)
                            isMoved = false
                            downX = it.x
                            downType = when (downX) {
                                in startX - controllerWidth..startX -> 1
                                in endX..endX + controllerWidth -> 2
                                in startX..endX -> 3
                                else -> 0
                            }
                            requestDisallowInterceptTouchEvent.invoke(true)
                        }
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (downType != 0) {
                            isMoved = true
                            setMoveOffset(it.x - downX)
                            downX = it.x
                        }
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        if (isMoved) {
                            onSeekTo(newStart)
                        }
                        requestDisallowInterceptTouchEvent.invoke(false)
                    }
                }
                return@pointerInteropFilter true
            }
    ) {
        //背景框
        drawRoundRect(
            color = Color.Black.copy(0.2f),
            topLeft = Offset(borderWidth / 2, borderWidth / 2),
            size = Size(size.width - borderWidth, size.height - borderWidth),
            cornerRadius = CornerRadius(5.dp.toPx()),
            style = Stroke(borderWidth)
        )

        if (duration > 0) {
            val currentX = positionToX(currentPosition)
            val startX = positionToX(newStart)
            val endX = positionToX(newEnd)

            //波形图
            val waveWidth = (size.width - controllerWidth * 2) /
                    (2 * totalSize + 1)
            dataList.forEachIndexed { index, data ->
                val dx = controllerWidth + (2 * index + 1.5f) * waveWidth
                val dataValue = 0.9f * size.height * data.coerceIn(1, 100) / 100
                drawLine(
                    color = if (dx in startX..endX) {
                        if (dx <= currentX) {
                            Color.Red
                        } else {
                            Color.Black
                        }
                    } else {
                        Color.Black.copy(0.3f)
                    },
                    start = Offset(dx, (size.height - dataValue) / 2),
                    end = Offset(dx, (size.height + dataValue) / 2),
                    strokeWidth = waveWidth,
                    cap = StrokeCap.Round
                )
            }

            //操作框
            val path = Path()
            path.addRoundRect(
                RoundRect(
                    rect = Rect(
                        Offset(startX - controllerWidth, 0f),
                        Size(controllerWidth, size.height)
                    ),
                    topLeft = CornerRadius(5.dp.toPx()),
                    bottomLeft = CornerRadius(5.dp.toPx())
                )
            )
            path.addRoundRect(
                RoundRect(
                    rect = Rect(
                        Offset(endX, 0f),
                        Size(controllerWidth, size.height)
                    ),
                    topRight = CornerRadius(5.dp.toPx()),
                    bottomRight = CornerRadius(5.dp.toPx())
                )
            )
            drawPath(
                path = path,
                color = Color.Red
            )
            drawLine(
                color = Color.Red,
                start = Offset(startX, borderWidth / 2),
                end = Offset(endX, borderWidth / 2),
                strokeWidth = borderWidth
            )
            drawLine(
                color = Color.Red,
                start = Offset(startX, size.height - borderWidth / 2),
                end = Offset(endX, size.height - borderWidth / 2),
                strokeWidth = borderWidth
            )

            //进度线
            drawLine(
                color = Color.Black,
                start = Offset(currentX, 0f),
                end = Offset(currentX, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}