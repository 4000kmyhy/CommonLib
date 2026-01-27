package com.yhy.commonlib.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yhy.commonlib.ui.theme.Pink80
import com.yhy.commonlib.ui.theme.Purple80
import com.yhy.lib_retrofit.entity.DeepSeekChat.getResult
import kotlinx.coroutines.launch

/**
 * desc:
 **
 * user: xujj
 * time: 2025/7/15 14:57
 **/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val chatMessages by viewModel.chatMessagesFlow.collectAsStateWithLifecycle()
    var userContent by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(chatMessages) {
        lazyListState.animateScrollToItem(chatMessages.size)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
//                    Text(
//                        text = "AI Chat",
//                        color = Color.Black,
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 20.sp,
//                        modifier = Modifier.clickable {
//                            viewModel.saveConversation()
//                        }
//                    )
                    ModelSelector {
                        viewModel.setModel(it)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.clearConversation()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Purple80
                )
            )
        },
        bottomBar = {
            ChatSendView(
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                content = userContent,
                onSend = {
                    userContent = ""
                    viewModel.chat(it)
                }
            )
        },
        containerColor = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .onSizeChanged {
                    scope.launch {
                        lazyListState.animateScrollToItem(chatMessages.size)
                    }
                },
            state = lazyListState,
            contentPadding = PaddingValues(15.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            items(chatMessages) { message ->
                if (message.role == "user") {
                    Text(
                        text = message.content,
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.CenterEnd)
                            .background(
                                color = Color(0xFFC7F8F8),
                                shape = RoundedCornerShape(
                                    topStart = 10.dp,
                                    bottomStart = 10.dp,
                                    bottomEnd = 10.dp
                                )
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = {
                                    userContent = message.content
                                })
                            }
                            .padding(horizontal = 15.dp, vertical = 10.dp)
                    )
                } else {
                    Text(
                        text = message.getResult(),
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.CenterStart)
                            .background(
                                color = Color(0xFFFFDBB9),
                                shape = RoundedCornerShape(
                                    topEnd = 10.dp,
                                    bottomStart = 10.dp,
                                    bottomEnd = 10.dp
                                )
                            )
                            .padding(horizontal = 15.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSendView(
    modifier: Modifier = Modifier,
    content: String,
    onSend: (String) -> Unit
) {
    val (value, onValueChange) = remember(content) { mutableStateOf(content) }
    TextField(
        modifier = modifier
            .background(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(15.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFE1E4EC),
                shape = RoundedCornerShape(15.dp)
            ),
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onSend(value)
            onValueChange("")
        }),
        textStyle = TextStyle(
            color = Color.Black,
            fontSize = 16.sp
        ),
        trailingIcon = {
            Text(
                text = "Send",
                color = Color.White,
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        color = if (value.isNotEmpty()) {
                            Purple80
                        } else {
                            Purple80.copy(0.5f)
                        }
                    )
                    .clickable(
                        enabled = value.isNotEmpty(),
                        onClick = {
                            onSend(value)
                            onValueChange("")
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            cursorColor = Pink80,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            selectionColors = TextSelectionColors(Pink80, Pink80.copy(0.2f))
        )
    )
}

@Composable
private fun ModelSelector(
    onModelChange: (String) -> Unit
) {
    var model by remember { mutableStateOf("DeepSeek-V3") }
    val list by remember {
        mutableStateOf(
            listOf(
                "InternVL2-8B",
                "Qwen2-Audio-7B-Instruct",
                "DeepSeek-Prover-V2-7B",
                "InternVL3-38B",
                "ERNIE-4.5-Turbo",
                "InternVL3-78B",
                "Lingshu-32B",
                "HealthGPT-L14",
                "DeepSeek-R1",
                "DeepSeek-V3"
            )
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
                properties = PopupProperties(focusable = true)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .background(
                            Color(0xFF646464),
                            RoundedCornerShape(10.dp)
                        )
                        .width(300.dp)
                        .padding(vertical = 10.dp)
                ) {
                    items(list) {
                        Text(
                            text = it,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    model = it
                                    onModelChange(it)
                                    popupState = false
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = model,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.clickable {
                popupState = true
            }
        )
    }
}