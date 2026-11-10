package com.yhy.commonlib.midi

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.yhy.commonlib.ui.theme.InstrumentColor
import com.yhy.commonlib.ui.theme.Purple80
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/4 16:45
 **/
@Composable
fun MidiWaveView2(
    modifier: Modifier = Modifier,
    trackList: List<InstrumentTrack>,
    currentPosition: Int,
    duration: Int,
    playState: Boolean,
    seekTo: (Int) -> Unit
) {
    var mWidth by remember { mutableIntStateOf(0) }
    val requestDisallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()

    val setTouch: (Float) -> Unit = {
        val position = if (mWidth > 0) {
            (it * duration / mWidth).roundToInt()
        } else {
            0
        }
        seekTo(position)
    }

    Canvas(
        modifier = modifier
            .height(128.dp)
            .onSizeChanged {
                mWidth = it.width
            }
            .pointerInteropFilter(requestDisallowInterceptTouchEvent) {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        setTouch(it.x)
                        requestDisallowInterceptTouchEvent.invoke(true)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        setTouch(it.x)
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        requestDisallowInterceptTouchEvent.invoke(false)
                    }
                }
                return@pointerInteropFilter true
            }) {
        val width = size.width
        val height = size.height

        trackList.forEach { track ->
            val program = MidiConstant.getProgramNumber(track.name)
            val category = MidiConstant.programToCategory(program)
            val color = InstrumentColor.getOrElse(
                category,
                { InstrumentColor[0] }
            )

            track.notes.forEach {
                val startX = width * it.start.toFloat() * 1000 / duration
                val endX = width * it.end.toFloat() * 1000 / duration
                val pitchY = height * (1 - it.pitch / 128f)

                drawLine(
                    color = color,
                    start = Offset(startX, pitchY),
                    end = Offset(endX, pitchY),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        val currentX = if (duration > 0) {
            1f * width * currentPosition / duration
        } else {
            0f
        }
        drawLine(
            color = Color.Red,
            start = Offset(currentX, 0f),
            end = Offset(currentX, height),
            strokeWidth = 1.dp.toPx()
        )
    }
}