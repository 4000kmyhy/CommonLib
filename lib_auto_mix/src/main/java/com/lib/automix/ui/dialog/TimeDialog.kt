package com.lib.automix.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lib.automix.R
import java.util.Locale

@Composable
fun rememberTimeDialogState(
    title: String = "",
    time: Long = 0
) = remember(title, time) {
    TimeDialogState(title, time)
}

class TimeDialogState(var title: String, var time: Long) {

    private var onConfirm: ((Long) -> Unit)? = null
    private val showDialog = mutableStateOf(false)

    fun show(title: String, timeState: State<Long>, onConfirm: (Long) -> Unit) {
        this.title = title
        this.time = timeState.value
        this.onConfirm = onConfirm
        show()
    }

    fun show() {
        this.showDialog.value = true
    }

    fun dismiss() {
        showDialog.value = false
    }

    val isShow: Boolean
        get() = showDialog.value

    fun confirm(value: Long) {
        onConfirm?.invoke(value)
    }
}

@Composable
fun TimeDialog(
    state: TimeDialogState
) {
    if (state.isShow) {
        val timeValue = remember {
            mutableLongStateOf(state.time)
        }

        Dialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { state.dismiss() }) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .background(
                        colorResource(id = R.color.am_dialog_background),
                        shape = RoundedCornerShape(5.dp)
                    )
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 20.dp,
                        bottom = 10.dp
                    )
            ) {
                Text(
                    text = state.title,
                    color = colorResource(id = R.color.am_text_color),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(
                        id = R.string.num_of_second,
                        String.format(Locale.ENGLISH, "%.1f", timeValue.longValue / 1000f)
                    ),
                    color = colorResource(id = R.color.am_subtext_color),
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = timeValue.longValue / 1000f,
                    onValueChange = {
                        timeValue.longValue = (it * 1000).toLong()
                    },
                    valueRange = 1f..60f,
                    colors = SliderColors(
                        thumbColor = colorResource(id = R.color.colorAccent),
                        activeTrackColor = colorResource(id = R.color.colorAccent),
                        activeTickColor = Color.Unspecified,
                        inactiveTrackColor = colorResource(id = R.color.am_progress_background),
                        inactiveTickColor = Color.Unspecified,
                        disabledThumbColor = Color.DarkGray,
                        disabledActiveTrackColor = colorResource(id = R.color.am_progress_background),
                        disabledActiveTickColor = Color.Unspecified,
                        disabledInactiveTrackColor = colorResource(id = R.color.am_progress_background),
                        disabledInactiveTickColor = Color.Unspecified
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = stringResource(id = android.R.string.cancel),
                        color = colorResource(id = R.color.colorAccent),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .clickable {
                                state.dismiss()
                            }
                            .padding(10.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = stringResource(id = android.R.string.ok),
                        color = colorResource(id = R.color.colorAccent),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .clickable {
                                state.confirm(timeValue.longValue)
                                state.dismiss()
                            }
                            .padding(10.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun TimeDialogPreview() {
    val state = rememberTimeDialogState(title = "xxx", time = 10000)
    state.show()
    TimeDialog(state = state)
}