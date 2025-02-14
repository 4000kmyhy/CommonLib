package com.lib.automix.ui.setting

import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lib.automix.R
import com.lib.automix.ui.AutoMixActivity
import com.lib.automix.ui.AutoMixViewModel
import com.lib.automix.ui.dialog.TimeDialog
import com.lib.automix.ui.dialog.rememberTimeDialogState
import com.lib.automix.ui.view.ComposeToolbar
import com.lib.automix.ui.view.getPainter
import com.lib.automix.utils.AutoMixConfigUtils
import java.util.Locale


/**
 * desc:
 **
 * user: xujj
 * time: 2024/7/24 20:05
 **/

data class AutoMixSettingState(
    val fadeTime: State<Long> = mutableLongStateOf(10000),
    val fadeEndTime: State<Long> = mutableLongStateOf(30000),
    val syncMode: State<Boolean> = mutableStateOf(true),
    val setFadeTime: (Long) -> Unit = {},
    val setFadeEndTime: (Long) -> Unit = {},
    val toggleSyncMode: () -> Unit = {}
)

@Composable
fun AutoMixSetting(
    viewModel: AutoMixViewModel = viewModel(LocalContext.current as AutoMixActivity),
    onNavigationClick: () -> Unit = {}
) {
    val state = AutoMixSettingState(
        fadeTime = viewModel.fadeTime.collectAsStateWithLifecycle(),
        fadeEndTime = viewModel.fadeEndTime.collectAsStateWithLifecycle(),
        syncMode = viewModel.syncMode.collectAsStateWithLifecycle(),
        setFadeTime = viewModel::setFadeTime,
        setFadeEndTime = viewModel::setFadeEndTime,
        toggleSyncMode = viewModel::toggleSyncMode,
    )
    AutoMixSetting(state, onNavigationClick)
}

@Composable
fun AutoMixSetting(
    state: AutoMixSettingState,
    onNavigationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colorResource(R.color.am_background),
        topBar = {
            val textColor = colorResource(R.color.am_text_color)
            ComposeToolbar(
                containerColor = Color.Transparent,
                titleResource = R.string.settings,
                titleSize = 18.sp,
                titleColor = textColor,
                titleWeight = FontWeight.Bold,
                navigationIconColor = textColor,
                actionsIconColor = textColor,
                navigationPainter = getPainter(imageVector = Icons.AutoMirrored.Default.ArrowBack),
                onNavigationClick = onNavigationClick
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {

            val timeDialogState = rememberTimeDialogState()
            TimeDialog(state = timeDialogState)

            val fadeTitle = stringResource(id = R.string.transition_duration)
            val fadeEndTitle = stringResource(id = R.string.transition_before_end)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ItemFadeTime(
                    title = fadeTitle,
                    time = state.fadeTime
                ) {
                    timeDialogState.show(fadeTitle, state.fadeTime) {
                        state.setFadeTime(it)
                    }
                }
                ItemFadeTime(
                    title = fadeEndTitle,
                    time = state.fadeEndTime
                ) {
                    timeDialogState.show(fadeEndTitle, state.fadeEndTime) {
                        state.setFadeEndTime(it)
                    }
                }
                ItemSyncMode(checked = state.syncMode) {
                    state.toggleSyncMode()
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = {
                    FrameLayout(it).also {
                        AutoMixConfigUtils.getConfig()?.createAdapterBanner(context, it)
                    }
                }
            )
        }
    }
}

@Composable
private fun ItemFadeTime(
    title: String,
    time: State<Long>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 25.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colorResource(id = R.color.am_text_color),
                fontSize = 15.sp
            )
            Text(
                text = stringResource(
                    id = R.string.num_of_second,
                    String.format(Locale.ENGLISH, "%.1f", time.value / 1000f)
                ),
                color = colorResource(id = R.color.am_subtext_color),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = colorResource(id = R.color.am_text_color)
        )
    }
}

@Composable
private fun ItemSyncMode(
    checked: State<Boolean>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 25.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.transition_sync),
            color = colorResource(id = R.color.am_text_color),
            fontSize = 15.sp
        )
        Switch(
            checked = checked.value,
            onCheckedChange = { onClick() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = colorResource(id = R.color.colorAccent),
                uncheckedThumbColor = Color(0xFFB9B9B9),
                uncheckedTrackColor = Color.Transparent,
                uncheckedBorderColor = Color(0xFF696B6B)
            )
        )
    }
}

@Preview(name = "landscape", device = "spec:shape=Normal,width=640,height=360,unit=dp,dpi=480")
@Composable
private fun AutoMixSettingPreview() {
    AutoMixSetting(AutoMixSettingState())
}