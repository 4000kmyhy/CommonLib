package com.yhy.commonlib.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhy.commonlib.ui.dialog.LoadingDialog
import com.yhy.commonlib.ui.theme.Purple80
import com.yhy.commonlib.ui.view.PlayerSlider
import com.yhy.commonlib.utils.StringUtils
import com.yhy.lib_compose.utils.DialogState
import com.yhy.lib_compose.utils.DisposableEffectWithLifecycle
import com.yhy.lib_compose.utils.roundClickable
import com.yhy.lib_compose.utils.toTextFieldValue

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/17 15:00
 **/
@Composable
fun VoiceGenerateScreen(
    viewModel: VoiceGenerateViewModel = viewModel()
) {
    val validateSentence by viewModel.validateSentence.collectAsStateWithLifecycle()
    val playState by viewModel.playStateFlow.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPositionFlow.collectAsStateWithLifecycle()
    val duration by viewModel.durationFlow.collectAsStateWithLifecycle()

    val loadingDialogState = DialogState.build { LoadingDialog(it) }

    val recordDialogState = DialogState.build {
        RecordVoiceDialog(
            tipContent = {
                Text(text = "请朗读验证短句")
                Text(
                    text = validateSentence,
                    fontWeight = FontWeight.Bold
                )
            },
            onDismiss = it,
            onSaved = viewModel::loadFile
        )
    }

    val startGeneration: () -> Unit = {
        viewModel.generateVoice(
            onStart = loadingDialogState::show,
            onFailed = loadingDialogState::dismiss,
            onSuccess = {
                loadingDialogState.dismiss()
            }
        )
    }

    DisposableEffectWithLifecycle(
        onPause = viewModel::pause
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "验证短句：$validateSentence")
        if (viewModel.audioFile == null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.regenerateValidation(
                        onStart = loadingDialogState::show,
                        onCompletion = loadingDialogState::dismiss
                    )
                }
            ) {
                Text(text = "重新生成验证短句")
            }
            Text(text = "Step2: 开始录制验证您的声音")
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = recordDialogState::show
            ) {
                Text(text = "开始录制")
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color.Black,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = viewModel::playOrPause
                ) {
                    Icon(
                        modifier = Modifier.size(48.dp),
                        imageVector = if (playState) {
                            Icons.Rounded.PauseCircle
                        } else {
                            Icons.Rounded.PlayCircle
                        },
                        contentDescription = null
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    PlayerSlider(
                        modifier = Modifier.align(Alignment.Center),
                        currentPosition = currentPosition,
                        duration = duration,
                        seekTo = viewModel::seekTo
                    )
                    Text(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 15.dp),
                        text = StringUtils.stringForTime(currentPosition.toLong()),
                        fontSize = 12.sp,
                        lineHeight = 1.em
                    )
                    Text(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 15.dp),
                        text = StringUtils.stringForTime(duration.toLong()),
                        fontSize = 12.sp,
                        lineHeight = 1.em
                    )
                }

                IconButton(
                    onClick = viewModel::deleteFile
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null
                    )
                }
            }

            InputBox(
                title = "Voice Name",
                value = viewModel.voiceName.toTextFieldValue(),
                onValueChange = {
                    viewModel.voiceName = it.text
                }
            )

            InputBox(
                title = "Description (optional)",
                value = viewModel.description.toTextFieldValue(),
                onValueChange = {
                    viewModel.description = it.text
                }
            )

            InputBox(
                title = "Style (optional)",
                value = viewModel.style.toTextFieldValue(),
                onValueChange = {
                    viewModel.style = it.text
                }
            )

            Text(text = "歌手技能等级")
            val skillLevelList = listOf(
                "beginner",
                "intermediate",
                "advanced",
                "professional"
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                skillLevelList.forEach {
                    Text(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFDDDDDD))
                            .clickable {
                                viewModel.singerSkillLevel = it
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        text = it,
                        color = if (viewModel.singerSkillLevel == it) {
                            Color(0xFF435F8B)
                        } else {
                            Color.Unspecified
                        },
                        fontWeight = if (viewModel.singerSkillLevel == it) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = startGeneration
            ) {
                Text(text = "生成音色")
            }

            if (!viewModel.voiceId.isNullOrEmpty()) {
                Text(text = "VoiceId")
                SelectionContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Color.Black,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(15.dp)
                ) {
                    Text(text = viewModel.voiceId.orEmpty())
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = viewModel::checkVoice
                ) {
                    Text(text = "检查音色是否可用")
                }
            }
        }
    }
}

@Composable
private fun InputBox(
    modifier: Modifier = Modifier,
    title: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    inputLines: Int = 1
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Color.Black,
                RoundedCornerShape(10.dp)
            )
    ) {
        Text(
            modifier = Modifier.padding(top = 15.dp, start = 15.dp, end = 15.dp),
            text = title
        )

        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            minLines = inputLines,
            maxLines = inputLines,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 12.sp,
                lineHeight = 2.em
            ),
            colors = mTextFieldColors()
        )

        Icon(
            modifier = Modifier
                .align(Alignment.End)
                .padding(start = 15.dp, end = 15.dp, bottom = 15.dp)
                .roundClickable {
                    onValueChange(TextFieldValue())
                }
                .padding(5.dp),
            imageVector = Icons.Rounded.Clear,
            contentDescription = null
        )
    }
}

@Composable
fun mTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    cursorColor = Purple80,
    selectionColors = TextSelectionColors(
        Purple80,
        Purple80.copy(0.2f)
    )
)