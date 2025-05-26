package com.yhy.commonlib.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhy.commonlib.ui.theme.Purple80
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * desc:
 **
 * user: xujj
 * time: 2025/4/7 14:53
 **/

@Composable
fun WaveformScreen(
    viewModel: WaveformViewModel = viewModel(),
    data: String
) {
    val waveformData by viewModel.waveformFlow.collectAsStateWithLifecycle()

    LaunchedEffect(data) {
        viewModel.loadWaveform(data)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        WaveformView(
            modifier = Modifier
//                .fillMaxSize()
                .fillMaxWidth()
                .height(200.dp),
            waveformData = waveformData
        )
        WaveformView(
            modifier = Modifier
//                .fillMaxSize()
                .fillMaxWidth()
                .height(200.dp),
            waveformData = waveformData,
            count = 300
        )
        WaveformView(
            modifier = Modifier
//                .fillMaxSize()
                .fillMaxWidth()
                .height(200.dp),
            waveformData = waveformData,
            count = 500
        )
    }
}

@Composable
fun WaveformView(
    modifier: Modifier = Modifier,
    waveformData: Pair<Float, List<IntArray>>,
    count: Int = 100
) {
    var width by remember { mutableIntStateOf(0) }
    var height by remember { mutableIntStateOf(0) }
    var waveformPath by remember { mutableStateOf(Path()) }
    var waveformPath2 by remember { mutableStateOf(Path()) }
    var mPosition by remember { mutableIntStateOf(0) }

    LaunchedEffect(waveformData, width, height) {
        withContext(Dispatchers.Default) {
            if (width == 0 || height == 0) return@withContext

            if (waveformData.first == 0f) {
                mPosition = 0
                waveformPath = Path()
                waveformPath2 = Path()
            } else {
                val path = Path()
                path.addPath(waveformPath)
                val path2 = Path()
                path2.addPath(waveformPath2)

                val dx = 1f * width / count
                val centerY = height / 2f
//                val getY: (Int) -> Float = {
//                    val normalized =
//                        (1f * waveformData.second.getOrElse(it, { 0 }) / Short.MAX_VALUE)
//                    (centerY * normalized).coerceAtLeast(1f)
//                }
                for (i in mPosition until count) {
                    val drawProgress = 1f * i / count//绘制进度
                    val dataIndex =
                        (drawProgress * waveformData.second.size / waveformData.first).toInt()
                    if (dataIndex in waveformData.second.indices) {
                        val x = dx * i
//                        val y = getY(dataIndex)
//                        path.addRect(
//                            Rect(
//                                offset = Offset(x, centerY - y),
//                                size = Size(dx, y * 2)
//                            )
//                        )
                        waveformData.second.getOrElse(dataIndex, { intArrayOf() })
                            .forEachIndexed { index, value ->
                                val y = (centerY * value / Short.MAX_VALUE).coerceAtLeast(1f)
                                if (index == 0) {
                                    path.addRect(
                                        Rect(
                                            offset = Offset(x, centerY - y),
                                            size = Size(dx, y * 2)
                                        )
                                    )
                                } else {
                                    path2.addRect(
                                        Rect(
                                            offset = Offset(x, centerY - y / 2),
                                            size = Size(dx, y)
                                        )
                                    )
                                }
                            }

                        mPosition = i + 1
                    } else {
                        break
                    }
                }

                waveformPath = path
                waveformPath2 = path2
            }
        }
    }

    Canvas(
        modifier = modifier.onSizeChanged {
            width = it.width
            height = it.height
        }
    ) {
        drawPath(
            path = waveformPath,
            color = Purple80.copy(0.5f),
//            style = Stroke(
//                width = 1.dp.toPx(),
//                cap = StrokeCap.Round
//            )
        )
        drawPath(
            path = waveformPath2,
            color = Purple80.copy(0.5f)
        )
    }
}