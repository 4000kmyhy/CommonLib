package com.yhy.commonlib.voice

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/21 14:59
 **/

private const val totalSize = 50

@Composable
fun RecordWaveView(
    modifier: Modifier = Modifier,
    dataList: List<Int>,
) {
    Canvas(modifier = modifier) {
        val waveWidth = size.width / (2 * totalSize + 1)
        repeat(totalSize) {
            val dx = (2 * it + 1.5f) * waveWidth
            val dataIndex = it + dataList.size - totalSize
            if (dataIndex in dataList.indices) {
                val dataValue = 0.9f * size.height * dataList[dataIndex].coerceIn(1, 100) / 100
                drawLine(
                    color = Color.Black,
                    start = Offset(dx, (size.height - dataValue) / 2),
                    end = Offset(dx, (size.height + dataValue) / 2),
                    strokeWidth = waveWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}