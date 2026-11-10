package com.yhy.commonlib.voice

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yhy.commonlib.R
import com.yhy.commonlib.ui.theme.Purple80
import com.yhy.lib_compose.theme.bottomSheetBackground
import com.yhy.lib_compose.view.BottomSheetDialog

/**
 * desc:
 **
 * user: xujj
 * time: 2025/8/18 10:28
 **/
private suspend fun LazyListState.scrollToItemVisible(position: Int) {
    try {
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        if (visibleItemsInfo.isNotEmpty()) {
            val firstItem = visibleItemsInfo.first()
            val lastItem = visibleItemsInfo.last()
            if (position <= firstItem.index) {
                scrollToItem(position)
            } else if (position >= lastItem.index) {
                val endOffset =
                    layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset - firstItem.size
                scrollToItem(position, -endOffset)
            }
        } else {
            scrollToItem(position)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@SuppressLint("LocalContextConfigurationRead")
@Composable
fun ValidateLanguageDialog(
    language: Pair<String, String> = Pair("en", "英文"),
    updateLanguage: (Pair<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val lazyListState = rememberLazyListState()
    val (value, onValueChange) = remember(language) { mutableStateOf(language) }
    val languageList = remember {
        listOf(
            Pair("en", "英文"),
            Pair("zh", "中文"),
            Pair("es", "西班牙语"),
            Pair("fr", "法语"),
            Pair("pt", "葡萄牙语言"),
            Pair("de", "德语"),
            Pair("ja", "日语"),
            Pair("ko", "韩语"),
            Pair("hi", "印地语"),
            Pair("ru", "俄语"),
        )
    }

    LaunchedEffect(languageList) {
        languageList.indexOfLast { it.first == language.first }.let {
            if (it >= 0) {
//                lazyListState.scrollToItem(it)
                lazyListState.scrollToItemVisible(it)
            }
        }
    }

    BottomSheetDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .bottomSheetBackground(Color(0xFFEEEEEE))
        ) {
            Text(
//                text = stringResource(R.string.language),
//                color = White,
//                fontSize = titleSize,
                text = "Language",
                modifier = Modifier.padding(15.dp)
            )
            HorizontalDivider(
//                color = DividerColor,
                thickness = 0.5.dp
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 335.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                state = lazyListState
            ) {
                items(languageList) {
                    val selected = it.first == value.first
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable {
                                onValueChange(it)
                            }
                            .padding(start = 25.dp, end = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = it.second,
//                            fontSize = textLargeSize,
//                            color = if (selected) AccentColor else White,
                            modifier = Modifier.weight(1f)
                        )
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            enabled = false,
                            colors = RadioButtonDefaults.colors(
//                                disabledSelectedColor = AccentColor,
//                                disabledUnselectedColor = White50
                            )
                        )
                    }
                }
            }

            Row(modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 15.dp)) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.Black.copy(0.1f))
                        .clickable(onClick = onDismiss)
                        .wrapContentSize(),
                    text = stringResource(R.string.cancel),
//                    color = White,
//                    fontSize = textLargeSize
                )

                Text(
                    modifier = Modifier
                        .padding(start = 15.dp)
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(Purple80)
                        .clickable {
                            updateLanguage(value)
                            onDismiss()
                        }
                        .wrapContentSize(),
                    text = stringResource(R.string.ok),
//                    color = White,
//                    fontSize = textLargeSize
                )
            }
        }
    }
}