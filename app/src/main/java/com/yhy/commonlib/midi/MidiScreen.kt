package com.yhy.commonlib.midi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhy.commonlib.ui.theme.Purple80
import com.yhy.commonlib.utils.StringUtils
import com.yhy.commonlib.viewmodel.MidiViewModel
import com.yhy.lib_compose.utils.roundClickable

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/1 9:48
 **/

@Composable
fun MidiScreen(
    viewModel: MidiViewModel = viewModel()
) {
    val trackPairs by viewModel.trackPairsFlow.collectAsStateWithLifecycle()
    val selectedList by viewModel.selectedListFlow.collectAsStateWithLifecycle()

    val currentPosition by viewModel.currentPositionFlow.collectAsStateWithLifecycle()
    val duration by viewModel.durationFlow.collectAsStateWithLifecycle()
    val playState by viewModel.playStateFlow.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .size(70.dp)
                    .roundClickable {
                        viewModel.playOrPause()
                    },
                imageVector = if (playState) {
                    Icons.Rounded.PauseCircle
                } else {
                    Icons.Rounded.PlayCircle
                },
                contentDescription = null,
                tint = Purple80
            )

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.convertToMidi()
                }
            ) {
                Text(text = "Convert to MIDI")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    viewModel.playMidi()
                }
            ) {
                Text(text = "Play MIDI")
            }
        }

        MidiWaveView(
            modifier = Modifier
                .padding(bottom = 15.dp)
                .fillMaxWidth()
                .height(500.dp),
            trackList = selectedList,
            currentPosition = currentPosition,
            duration = duration,
            playState = playState,
            seekTo = viewModel::seekTo,
            onPlay = viewModel::play,
            onPause = viewModel::pause
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                val isNotAll = trackPairs.any { !it.second }
                TrackItem(
                    text = "全选",
                    selected = !isNotAll,
                    onClick = {
                        viewModel.selectAll()
                    }
                )
            }
            itemsIndexed(trackPairs) { index, item ->
                TrackItem(
                    text = "${item.first.name}\n${item.first.notes.size}音符",
                    selected = item.second,
                    onClick = {
                        viewModel.setSelected(index)
                    }
                )
            }
        }

//        Row(
//            modifier = Modifier.padding(horizontal = 15.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = StringUtils.stringForTime(currentPosition.toLong())
//            )
//            Slider(
//                modifier = Modifier
//                    .padding(horizontal = 15.dp)
//                    .weight(1f),
//                value = if (duration > 0) {
//                    1f * currentPosition / duration
//                } else {
//                    0f
//                },
//                onValueChange = {
//                    val position = duration * it
//                    viewModel.seekTo(position.toInt())
//                },
//                colors = SliderDefaults.colors(
//                    thumbColor = Purple80,
//                    activeTrackColor = Purple80
//                )
//            )
//            Text(
//                text = StringUtils.stringForTime(duration.toLong())
//            )
//        }
    }
}

@Composable
private fun TrackItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            fontSize = 16.sp
        )
        Checkbox(
            checked = selected,
            onCheckedChange = null,
            enabled = false
        )
    }
}