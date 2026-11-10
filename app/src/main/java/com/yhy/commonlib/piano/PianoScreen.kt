package com.yhy.commonlib.piano

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhy.commonlib.midi.MidiNote
import com.yhy.commonlib.utils.MusicalNotationUtils.parseNoteList
import com.yhy.lib_compose.utils.DisposableEffectWithLifecycle
import com.yhy.lib_piano.PianoConstant
import com.yhy.lib_piano.view.PianoSeekBar
import com.yhy.lib_piano.view.PianoView

/**
 * desc:
 **
 * user: xujj
 * time: 2026/6/25 15:42
 **/

@Composable
fun PianoScreen(viewModel: PianoViewModel = viewModel()) {
    val context = LocalContext.current

    var keyNum by remember { mutableIntStateOf(14) }
    var startProgress by remember {
        mutableFloatStateOf((PianoConstant.getCPosition() - 3).toFloat())
    }

    DisposableEffectWithLifecycle(
        onPause = viewModel::pause
    )

    Column(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
            .padding(10.dp)
            .clipToBounds()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SongSelector(viewModel::setNoteList)
            IconButton(onClick = viewModel::playNoteList) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    keyNum = (keyNum - 1).coerceAtLeast(10)
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.RemoveCircle,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            IconButton(
                onClick = {
                    keyNum = (keyNum + 1).coerceAtMost(20)
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddCircle,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Switch(
                modifier = Modifier.padding(horizontal = 10.dp),
                checked = viewModel.freePlayMode,
                onCheckedChange = {
                    viewModel.freePlayMode = it
                }
            )
            IconButton(
                onClick = {
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        PianoSeekBar(
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .height(40.dp),
            keyNum = keyNum,
            startProgress = startProgress,
            onStartProgressChange = {
                startProgress = it
            },
            playPositions = viewModel.playPositions,
            tipPositions = viewModel.tipPositions
        )

        PianoView(
            modifier = Modifier
                .padding(top = 5.dp)
                .fillMaxWidth()
                .weight(1f),
            keyNum = keyNum,
            startProgress = startProgress,
            playPositions = viewModel.playPositions,
            tipPositions = viewModel.tipPositions,
            keyOn = viewModel::keyOn,
            keyOff = viewModel::keyOff,
            updatePlayPositions = viewModel::updatePlayPositions
        )
    }
}

@Composable
fun SongSelector(
    onSongSelected: (List<MidiNote>) -> Unit
) {
    val context = LocalContext.current
    var songName by remember { mutableStateOf("None") }
    val list = remember {
        listOf(
            Pair("两只老虎", parseNoteList(context, "nmn/two_tiger.json")),
            Pair("生日快乐歌", parseNoteList(context, "nmn/birthday.json")),
            Pair("友谊天长地久", parseNoteList(context, "nmn/friendship.json")),
            Pair("小星星", parseNoteList(context, "nmn/little_star.json")),
            Pair("欢乐颂", parseNoteList(context, "nmn/ode_an_die_freude.json")),
            Pair("Jingle Bells", parseNoteList(context, "nmn/jingle_bells.json")),
            Pair("TIGA", parseNoteList(context, "nmn/tiga.json")),
            Pair("2077", parseNoteList(context, "nmn/2077.json")),
            Pair("Una Mattina", parseNoteList(context, "nmn/unamattina2.json")),
        )
    }
    Column {
        var popupState by remember { mutableStateOf(false) }
        if (popupState) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = {
                    popupState = false
                },
                offset = IntOffset(0, 100),
                properties = PopupProperties(focusable = true)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .background(
                            Color(0xFF646464),
                            RoundedCornerShape(10.dp)
                        )
                        .width(200.dp)
                        .heightIn(max = 250.dp)
                        .padding(vertical = 10.dp)
                ) {
                    items(list) {
                        Text(
                            text = it.first,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    songName = it.first
                                    onSongSelected(it.second)
                                    popupState = false
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                popupState = true
            }
        ) {
            Text(songName)
        }
    }
}