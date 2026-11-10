package com.yhy.commonlib.voice

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhy.commonlib.entity.ValidateInfo
import com.yhy.commonlib.ui.dialog.LoadingDialog
import com.yhy.commonlib.utils.FileUtils
import com.yhy.commonlib.utils.StringUtils
import com.yhy.lib_compose.utils.DialogState
import com.yhy.lib_compose.utils.DisposableEffectWithLifecycle
import com.yhy.lib_compose.utils.pureClickable

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/15 10:41
 **/
@Composable
fun VoiceValidateScreen(
    viewModel: VoiceValidateViewModel = viewModel(),
    navigateToGenerate: (ValidateInfo) -> Unit
) {
    val playState by viewModel.playStateFlow.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPositionFlow.collectAsStateWithLifecycle()
    val duration by viewModel.durationFlow.collectAsStateWithLifecycle()
    val startPosition by viewModel.startPositionFlow.collectAsStateWithLifecycle()
    val endPosition by viewModel.endPositionFlow.collectAsStateWithLifecycle()
    val waveformData by viewModel.waveformDataFlow.collectAsStateWithLifecycle()
    val language by viewModel.languageFlow.collectAsStateWithLifecycle()

    val selectAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val uri = it.data?.data
            if (uri != null) {
                viewModel.loadUri(uri)
            }
        }
    }

    val loadingDialogState = DialogState.build { LoadingDialog(it) }

    val recordDialogState = DialogState.build {
        RecordVoiceDialog(
            tipContent = {
                Text(text = "请录制一段清唱人声样本")
                Text(
                    text = "(示例)秋风瑟瑟卷枝头，\n" +
                            "两叶相依不肯休。\n" +
                            "共舞长空终入土，\n" +
                            "来年春至又重游。",
                    color = Color.Black.copy(0.5f)
                )
            },
            onDismiss = it,
            onSaved = viewModel::loadFile,
            minDuration = VoiceValidateViewModel.MIN_DURATION
        )
    }

    val languageDialogState = DialogState.build {
        ValidateLanguageDialog(
            language = language,
            updateLanguage = viewModel::updateLanguage,
            onDismiss = it
        )
    }

    val startGeneration: () -> Unit = {
        viewModel.generateValidation(
            onStart = loadingDialogState::show,
            onFailed = loadingDialogState::dismiss,
            onSuccess = {
                navigateToGenerate(it)
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "Step1: 选择包含自己的声音的原始录音文件或录制一段清唱人声样本，生成验证短句。")

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                selectAudioLauncher.launch(FileUtils.openDocumentIntent())
            }
        ) {
            Text(text = "选择音频文件")
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = recordDialogState::show
        ) {
            Text(text = "录制声音")
        }

        viewModel.audioFile?.let { file ->
            Text(text = file.name)
            Text(
                text = StringUtils.stringForTime(currentPosition.toLong()) + "/" +
                        StringUtils.stringForTime(duration.toLong())
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
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

                WaveformView(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .fillMaxWidth()
                        .height(80.dp),
                    dataList = waveformData,
                    currentPosition = currentPosition,
                    duration = duration,
                    startPosition = startPosition,
                    endPosition = endPosition,
                    extractPosition = viewModel::extractPosition,
                    onSeekTo = viewModel::seekTo,
                    minDuration = VoiceValidateViewModel.MIN_DURATION,
                    totalSize = VoiceValidateViewModel.WAVE_TOTAL_SIZE
                )
            }


            Text(
                text = if (duration >= VoiceValidateViewModel.MIN_DURATION) {
                    "请截取有清晰人声片段"
                } else {
                    "请选择时长至少为10秒的音频文件"
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "开始：${startPosition / 1000}秒")
                Text(text = "结束：${endPosition / 1000}秒")
                Text(text = "时长：${(endPosition - startPosition) / 1000}秒")
            }

            Row(
                modifier = Modifier
                    .border(
                        1.dp,
                        Color.Black,
                        RoundedCornerShape(5.dp)
                    )
                    .pureClickable(onClick = languageDialogState::show)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "验证短句语言：${language.second}")
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = startGeneration
            ) {
                Text(text = "生成验证短句")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    navigateToGenerate(
                        ValidateInfo(
                            taskId = "4c1df1b52d4a9659eaa0622630bab1eb",
                            validateInfo = "音乐海浪轻轻回响在耳畔"
                        )
                    )
                }
            ) {
                Text(text = "test")
            }
        }
    }
}