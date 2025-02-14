package com.lib.automix.ui.view

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lib.automix.R
import com.lib.automix.ui.home.HomeState
import com.lib.automix.utils.getAccentColor
import com.lib.automix.utils.stringForTime

/**
 * desc:
 **
 * user: xujj
 * time: 2024/7/1 16:46
 **/

const val RATE = 5f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ComposeDisk(
    modifier: Modifier = Modifier,
    homeState: HomeState,
) {
    val context = LocalContext.current
    val currentColor = Color(getAccentColor(context, homeState.currentDeck.value))
    val anotherColor = Color(getAccentColor(context, !homeState.currentDeck.value))
    val innerScale = (208f + (420f - 208f) * if (homeState.currentDeck.value) {
        1f * homeState.volumeProgress.value / 200
    } else {
        1 - 1f * homeState.volumeProgress.value / 200
    }) / 420f

    val requestDisallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()
    val bitmapDisc = ImageBitmap.imageResource(id = R.drawable.automix_disk)
    val textMeasurer = rememberTextMeasurer()

    //手势监听
    val width = remember { mutableIntStateOf(0) }
    val height = remember { mutableIntStateOf(0) }
    val isInCircle = remember { mutableStateOf(false) }
    val currentDegree = remember { mutableFloatStateOf(0f) }
    val isPressed = remember { mutableStateOf(false) }
    val mDegrees = remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .onSizeChanged {
                width.intValue = it.width
                height.intValue = it.height
            }
            .pointerInteropFilter(
                requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
            ) {
                if (!homeState.transitionState.value) { //非过渡状态
                    if (it.actionIndex == 0) { //只监听第一根手指
                        when (it.action) {
                            MotionEvent.ACTION_DOWN -> {
                                isInCircle.value = isInCircle(
                                    it.x,
                                    it.y,
                                    width.intValue,
                                    height.intValue
                                )
                                if (isInCircle.value) {
                                    currentDegree.floatValue = deltaDegree(
                                        it.x,
                                        it.y,
                                        width.intValue,
                                        height.intValue
                                    )
                                    isPressed.value = true
                                    homeState.onStartTrackingTouch()
                                    requestDisallowInterceptTouchEvent.invoke(true)
                                }
                            }

                            MotionEvent.ACTION_MOVE -> {
                                if (isInCircle.value) {
                                    val degree = deltaDegree(
                                        it.x,
                                        it.y,
                                        width.intValue,
                                        height.intValue
                                    )
                                    var delta = degree - currentDegree.floatValue
                                    if (delta < -270) { // 如果小于-90度说明 它跨周了，需要特殊处理350->17,
                                        delta += 360
                                    } else if (delta > 270) { // 如果大于90度说明 它跨周了，需要特殊处理-350->-17,
                                        delta -= 360
                                    }
                                    currentDegree.floatValue = degree

                                    var newDegrees: Float = mDegrees.floatValue + delta
                                    newDegrees = Math.max(newDegrees, 0f)
                                    newDegrees =
                                        Math.min(newDegrees, homeState.duration.value / RATE)
                                    mDegrees.floatValue = newDegrees

                                    val progress = (newDegrees * RATE).toInt()
                                    homeState.onProgressChanged(progress)
                                }
                            }

                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL,
                            MotionEvent.ACTION_POINTER_UP -> {
                                if (isInCircle.value) {
                                    isPressed.value = false
                                    homeState.onStopTrackingTouch()
                                }
                                requestDisallowInterceptTouchEvent.invoke(false)
                            }
                        }
                    }
                    return@pointerInteropFilter true
                }
                false
            }
    ) {

        if (!isPressed.value) {
            mDegrees.floatValue = if (homeState.transitionState.value) {
                homeState.transitionPosition.value / RATE
            } else {
                homeState.currentPosition.value / RATE
            }
        }
        rotate(mDegrees.floatValue) {
            //disc
            drawImage(
                image = bitmapDisc,
                dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
        }

        //外圈
        val border = 2.5.dp.toPx()
        val outRadius = (size.width - border) / 2
        drawCircle(
            color = currentColor,
            radius = outRadius,
            style = Stroke(width = border)
        )

        if (homeState.transitionState.value) {
            //内圈
            val innerRadius = (innerScale * size.width - border) / 2
            drawCircle(
                color = anotherColor,
                radius = innerRadius,
                style = Stroke(width = border)
            )
        }

        val currentTime =
            textMeasurer.measure(
                AnnotatedString(stringForTime(homeState.currentPosition.value.toLong())),
                style = TextStyle(fontSize = 14.sp)
            )
        drawText(
            currentTime,
            color = currentColor,
            topLeft = Offset(
                (size.width - currentTime.size.width) / 2,
                if (homeState.transitionState.value) {
                    size.height / 2 - currentTime.size.height - 2.5.dp.toPx()
                } else {
                    (size.height - currentTime.size.height) / 2
                }

            )
        )

        if (homeState.transitionState.value) {
            val transitionTime =
                textMeasurer.measure(
                    AnnotatedString(stringForTime(homeState.transitionPosition.value.toLong())),
                    style = TextStyle(fontSize = 14.sp)
                )
            drawText(
                transitionTime,
                color = anotherColor,
                topLeft = Offset(
                    (size.width - currentTime.size.width) / 2,
                    size.height / 2 + 2.5.dp.toPx()
                )
            )
        }
    }
}

/**
 * 按下时在圆内
 */
private fun isInCircle(x: Float, y: Float, width: Int, height: Int): Boolean {
    val centerDis = Math.sqrt(
        ((x - 0.5f * width) * (x - 0.5f * width) +
                (y - 0.5f * height) * (y - 0.5f * height)).toDouble()
    ).toFloat()
    return centerDis <= 0.5f * Math.min(width, height)
}

private fun deltaDegree(targetX: Float, targetY: Float, width: Int, height: Int): Float {
    val deltaX = targetX - 0.5f * width
    val deltaY = targetY - 0.5f * height
    val d = if (deltaX != 0f) {
        val tan = Math.abs(deltaY / deltaX)
        if (deltaX > 0) {
            if (deltaY >= 0) {
                Math.atan(tan.toDouble())
            } else {
                2 * Math.PI - Math.atan(tan.toDouble())
            }
        } else {
            if (deltaY >= 0) {
                Math.PI - Math.atan(tan.toDouble())
            } else {
                Math.PI + Math.atan(tan.toDouble())
            }
        }
    } else {
        if (deltaY > 0) {
            Math.PI / 2
        } else {
            -Math.PI / 2
        }
    }
    return (d * 180 / Math.PI).toFloat()
}

@Preview
@Composable
private fun DiskPreview() {
    ComposeDisk(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(1f),
        homeState = HomeState()
    )
}