package com.yhy.commonlib.voice

import android.Manifest
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lib.bass.utils.BassUtils.STATE_IDLE
import com.lib.bass.utils.BassUtils.STATE_PLAYING
import com.yhy.commonlib.ui.dialog.LoadingDialog
import com.yhy.commonlib.utils.StringUtils
import com.yhy.commonlib.utils.ToastUtils
import com.yhy.commonlib.utils.requestPermission
import com.yhy.lib_compose.theme.bottomSheetBackground
import com.yhy.lib_compose.utils.DialogState
import com.yhy.lib_compose.utils.DisposableEffectWithLifecycle
import com.yhy.lib_compose.utils.isInvisible
import com.yhy.lib_compose.utils.roundClickable
import com.yhy.lib_compose.view.BottomSheetBehaviorProperties
import com.yhy.lib_compose.view.BottomSheetDialog
import com.yhy.lib_compose.view.BottomSheetDialogProperties
import kotlin.math.roundToInt

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/20 9:42
 **/
@Composable
fun RecordVoiceDialog(
    viewModel: RecordVoiceViewModel = viewModel(),
    tipContent: @Composable () -> Unit,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit,
    minDuration: Long = 0
) {
    val context = LocalContext.current
    val requestPermission = requestPermission(Manifest.permission.RECORD_AUDIO) {
        viewModel.startRecord()
    }

    val loadingDialogState = DialogState.build { LoadingDialog(it) }

    DisposableEffectWithLifecycle(
        onPause = viewModel::pause
    )

    BottomSheetDialog(
        onDismissRequest = {
            Log.d("xxx", "RecordVoiceDialog: dismiss")
            viewModel.release()
            onDismiss()
        },
        properties = BottomSheetDialogProperties(
            dismissOnClickOutside = false,
            behaviorProperties = BottomSheetBehaviorProperties(isDraggable = false)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .bottomSheetBackground(Color(0xFFEEEEEE))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 15.dp)
            ) {
                tipContent()
            }

            RecordWaveView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFFDDDDDD)),
                dataList = viewModel.recordWaveData
            )

            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 20.dp),
                text = StringUtils.stringForMsTime(viewModel.currentPosition.toLong()),
                fontSize = 25.sp
            )

            Row(
                modifier = Modifier
                    .padding(vertical = 20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    modifier = Modifier.isInvisible(viewModel.isRecordIdle()),
                    onClick = viewModel::release
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null
                    )
                }

                Icon(
                    modifier = Modifier
                        .size(48.dp)
                        .roundClickable(onClick = requestPermission),
                    imageVector = when (viewModel.recordState) {
                        STATE_IDLE -> Icons.Rounded.Mic
                        STATE_PLAYING -> Icons.Rounded.Pause
                        else -> Icons.Rounded.PlayArrow
                    },
                    contentDescription = null
                )

                IconButton(
                    modifier = Modifier.isInvisible(viewModel.isRecordIdle()),
                    onClick = {
                        if (viewModel.currentPosition >= minDuration) {
                            viewModel.saveRecord(
                                onStart = loadingDialogState::show,
                                onSaved = {
                                    loadingDialogState.dismiss()
                                    onSaved(it)
                                    onDismiss()
                                }
                            )
                        } else {
                            val seconds = (minDuration / 1000f).roundToInt()
                            ToastUtils.showToast(context, "录音时长至少为${seconds}秒")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null
                    )
                }
            }
        }
    }
}