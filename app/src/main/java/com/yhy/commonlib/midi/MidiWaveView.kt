package com.yhy.commonlib.midi

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yhy.commonlib.ui.theme.InstrumentColor
import com.yhy.commonlib.utils.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/4 16:45
 **/

private const val KeyCount = 88 // A0~C8，白键52，黑键36
private const val WhiteKeyCount = 52

fun hasBlackKey(position: Int): Boolean {
    val positionInGroup = (position + 5) % 7
    return positionInGroup != 2 && positionInGroup != 6 && position != WhiteKeyCount - 1
}

/**
 * 判断是否为黑键，这里的position是所有键(88键)的索引
 * 每组有12个键：7白+5黑
 * 第0组有3个键：2白+1黑
 */
fun isBlackKey(index: Int): Boolean {
    val positionInGroup = (index + 9) % 12
    return positionInGroup == 1 || positionInGroup == 3 ||
            positionInGroup == 6 || positionInGroup == 8 || positionInGroup == 10
}

fun isWhiteKey(index: Int) = !isBlackKey(index)

/**
 * 从52白键中找到C键
 */
fun getCNote(position: Int): String? {
    val positionInGroup = (position + 5) % 7
    if (positionInGroup == 0) {
        val group = (position + 5) / 7
        return "C$group"
    }
    return null
}

/**
 * 88键中找到C键，A0、A#0，B0，C1...
 */
//fun getCNote(index: Int): String? {
//    if (index >= 3) {
//        val cIndex = index - 3 // 让c1从0开始
//        val group = cIndex / 12 + 1 // 12个一组
//        if (cIndex % 12 == 0) { //第一个键是C
//            return "C$group"
//        }
//    }
//    return null
//}

@Composable
fun MidiWaveView(
    modifier: Modifier = Modifier,
    trackList: List<InstrumentTrack>,
    currentPosition: Int,
    duration: Int,
    playState: Boolean,
    scale: Float = 100f,
    bpm: Float = 120f,
    seekTo: (Int) -> Unit,
    onPlay: () -> Unit = {},
    onPause: () -> Unit = {}
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    var mWidth by remember { mutableIntStateOf(0) }
    var mHeight by remember { mutableIntStateOf(0) }
    val requestDisallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()

    val verticalPadding = with(LocalDensity.current) { 20.dp.toPx() }
    val keyPadding = with(LocalDensity.current) { 1.dp.toPx() }
    val whiteKeyWidth = with(density) { 40.dp.toPx() }
    val blackKeyWidth = with(density) { 20.dp.toPx() }
    var whiteKeyHeight by remember { mutableFloatStateOf(0f) }
    var blackKeyHeight by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    var whiteKeyPath by remember { mutableStateOf(Path()) }
    var blackKeyPath by remember { mutableStateOf(Path()) }
    var noteCellPath by remember { mutableStateOf(Path()) }
    var cNoteList by remember { mutableStateOf(listOf<Pair<String, Float>>()) }

    fun getPitchHeight(index: Int): Float {
        val group = (index + 9) / 12
        val positionInGroup = (index + 9) % 12
        return if (group == 0) {
            (whiteKeyHeight * 2 + keyPadding) / 3
        } else if (group == 8) {
            whiteKeyHeight
        } else {
            if (positionInGroup < 5) {
                (whiteKeyHeight * 3 + keyPadding * 2) / 5
            } else {
                (whiteKeyHeight * 4 + keyPadding * 3) / 7
            }
        }
    }

    fun getPitchY(index: Int, pitchHeight: Float): Float {
        val group = (index + 9) / 12
        val positionInGroup = (index + 9) % 12
        return if (group == 0) {
            mHeight - verticalPadding - pitchHeight * (index + 1)
        } else {
            if (positionInGroup < 5) {
                mHeight - verticalPadding -
                        (whiteKeyHeight + keyPadding) * 2 - //第0组2个白键
                        (whiteKeyHeight + keyPadding) * 7 * (group - 1) - //每7个白键一组
                        pitchHeight * (positionInGroup + 1)
            } else {
                mHeight - verticalPadding -
                        (whiteKeyHeight + keyPadding) * 5 - //第0组5个白键
                        (whiteKeyHeight + keyPadding) * 7 * (group - 1) - //每7个白键一组
                        pitchHeight * (positionInGroup - 4)
            }
        }
    }

    LaunchedEffect(mWidth, mHeight) {
        if (mWidth > 0 && mHeight > 0) {
            whiteKeyHeight =
                (mHeight - verticalPadding * 2 - keyPadding * (WhiteKeyCount - 1)) / WhiteKeyCount
            blackKeyHeight = whiteKeyHeight * 3 / 5
            offsetX = (mWidth + whiteKeyWidth) / 2

            withContext(Dispatchers.Default) {
                val whiteKeyPathTemp = Path()
                val blackKeyPathTemp = Path()
                val noteCellPathTemp = Path()
                val cNoteListTemp = ArrayList<Pair<String, Float>>()

                repeat(WhiteKeyCount) { position ->
                    //白健
                    val keyY =
                        mHeight - verticalPadding - whiteKeyHeight * (position + 1) - keyPadding * position
                    whiteKeyPathTemp.addRect(
                        Rect(
                            offset = Offset(0f, keyY),
                            size = Size(whiteKeyWidth, whiteKeyHeight)
                        )
                    )
                    //键名
                    val cNote = getCNote(position)
                    if (!cNote.isNullOrEmpty()) {
                        cNoteListTemp.add(
                            Pair(cNote, keyY)
                        )
                    }
                    //黑键
                    if (hasBlackKey(position)) {
                        val blackKeyY = keyY - keyPadding / 2 - blackKeyHeight / 2
                        blackKeyPathTemp.addRect(
                            Rect(
                                offset = Offset(0f, blackKeyY),
                                size = Size(blackKeyWidth, blackKeyHeight)
                            )
                        )
                    }
                }

                repeat(KeyCount) { index ->
                    //白键音符格子
                    val isWhiteKey = isWhiteKey(index)
                    if (isWhiteKey) {
                        val pitchHeight = getPitchHeight(index)
                        val pitchY = getPitchY(index, pitchHeight)
                        noteCellPathTemp.addRect(
                            Rect(
                                offset = Offset(whiteKeyWidth, pitchY),
                                size = Size(mWidth - whiteKeyWidth, pitchHeight)
                            )
                        )
                    }
                }

                whiteKeyPath = whiteKeyPathTemp
                blackKeyPath = blackKeyPathTemp
                noteCellPath = noteCellPathTemp
                cNoteList = cNoteListTemp
            }
        }
    }

    var isPressed by remember { mutableStateOf(false) }
    var pressedX by remember { mutableFloatStateOf(0f) }
    var playStateOnPressed by remember { mutableStateOf(false) }

    val timeText = remember(currentPosition, duration) {
        textMeasurer.measure(
            text = StringUtils.stringForTime(currentPosition.toLong()) + " / " +
                    StringUtils.stringForTime(duration.toLong()),
            style = TextStyle(fontSize = 10.sp),
            maxLines = 1
        )
    }

    Canvas(
        modifier = modifier
            .background(Color.Black)
            .onSizeChanged {
                mWidth = it.width
                mHeight = it.height
            }
            .pointerInteropFilter(requestDisallowInterceptTouchEvent) {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        pressedX = it.x
                        playStateOnPressed = playState
                        onPause()
                        requestDisallowInterceptTouchEvent.invoke(true)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = it.x - pressedX
                        pressedX = it.x
                        offsetX = (offsetX - deltaX).coerceIn(
                            (mWidth + whiteKeyWidth) / 2,
                            (mWidth + whiteKeyWidth) / 2 + duration / 1000f * scale
                        )
                        val position =
                            ((offsetX - (mWidth + whiteKeyWidth) / 2) * 1000f / scale).roundToInt()
                        seekTo(position)
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        if (playStateOnPressed) {
                            onPlay()
                        }
                        requestDisallowInterceptTouchEvent.invoke(false)
                    }
                }
                return@pointerInteropFilter true
            }
    ) {
        val width = size.width
        val height = size.height

        drawPath(
            path = whiteKeyPath,
            color = Color.White
        )
        drawPath(
            path = blackKeyPath,
            color = Color.Black
        )
        drawPath(
            path = noteCellPath,
            color = Color.White.copy(0.2f)
        )
        cNoteList.forEach { (text, top) ->
            val noteText = textMeasurer.measure(
                text = text,
                style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            drawText(
                textLayoutResult = noteText,
                color = Color.Black,
                topLeft = Offset(
                    whiteKeyWidth - noteText.size.width - 2.dp.toPx(),
                    top + (whiteKeyHeight - noteText.size.height) / 2f
                )
            )
        }

        if (duration > 0) {
            if (!isPressed) {
                offsetX = (width + whiteKeyWidth) / 2 + currentPosition / 1000f * scale
            }
            clipRect(left = whiteKeyWidth) {
                translate(left = -offsetX) {
                    if (bpm > 0) {
                        //四分音符
                        val beatWidth = 4 * 60 / bpm * scale
                        val beatCount = (bpm * duration / 1000 / 60 / 4).toInt() + 1
                        repeat(beatCount) {
                            val beatX = whiteKeyWidth + width + beatWidth * it
                            if (beatX - offsetX in 0f..width) {
                                drawLine(
                                    color = Color.White.copy(0.25f),
                                    start = Offset(beatX, 0f),
                                    end = Offset(beatX, height - verticalPadding)
                                )
                                repeat(3) {//四分音符
                                    val lineX = beatX + beatWidth / 4 * (it + 1)
                                    drawLine(
                                        color = Color.White.copy(0.2f),
                                        start = Offset(lineX, verticalPadding),
                                        end = Offset(lineX, height - verticalPadding)
                                    )
                                }

                                val beatText = textMeasurer.measure(
                                    text = "${it + 1}",
                                    style = TextStyle(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1
                                )
                                drawText(
                                    textLayoutResult = beatText,
                                    color = Color.White,
                                    topLeft = Offset(
                                        beatX + 2.dp.toPx(),
                                        0f
                                    )
                                )
                                val beatTime = textMeasurer.measure(
                                    text = StringUtils.stringForMsTime((it * 60 * 1000 * 4 / bpm).toLong()),
                                    style = TextStyle(fontSize = 8.sp),
                                    maxLines = 1
                                )
                                drawText(
                                    textLayoutResult = beatTime,
                                    color = Color.White.copy(0.5f),
                                    topLeft = Offset(
                                        beatX + 2.dp.toPx(),
                                        verticalPadding / 2
                                    )
                                )
                            }
                        }
                    }

                    trackList.forEach { track ->
                        val program = MidiConstant.getProgramNumber(track.name)
                        val category = MidiConstant.programToCategory(program)
                        val color = InstrumentColor.getOrElse(
                            category,
                            { InstrumentColor[0] }
                        )

                        track.notes.forEach {
                            if (it.pitch in 21..108) {
                                val startX = whiteKeyWidth + width + it.start.toFloat() * scale
                                val endX = whiteKeyWidth + width + it.end.toFloat() * scale

                                if (startX - offsetX in 0f..width ||
                                    endX - offsetX in 0f..width
                                ) {
                                    val keyPosition = it.pitch - 21
                                    val pitchHeight = getPitchHeight(keyPosition)
                                    val pitchY = getPitchY(keyPosition, pitchHeight)

                                    drawRect(
                                        color = color,
                                        topLeft = Offset(startX, pitchY),
                                        size = Size(endX - startX, pitchHeight)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val currentX = (width + whiteKeyWidth) / 2
            drawLine(
                color = Color.Red,
                start = Offset(currentX, 0f),
                end = Offset(currentX, height - verticalPadding),
                strokeWidth = 1.dp.toPx()
            )
            drawText(
                textLayoutResult = timeText,
                color = Color.White,
                topLeft = Offset(
                    currentX - timeText.size.width / 2,
                    height - (verticalPadding + timeText.size.height) / 2f
                )
            )
        }
    }
}