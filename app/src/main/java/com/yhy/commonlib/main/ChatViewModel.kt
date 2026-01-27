package com.yhy.commonlib.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.yhy.lib_retrofit.entity.ChatMessage
import com.yhy.lib_retrofit.service.GiteeAiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * desc:
 **
 * user: xujj
 * time: 2025/7/15 15:35
 **/
class ChatViewModel : ViewModel() {

    private var chatJob: Job? = null

    private val conversationHistory = mutableListOf<ChatMessage>()
    val chatMessagesFlow = MutableStateFlow(listOf<ChatMessage>())

    private var chatModel = "DeepSeek-V3"

    init {
        initSystem()
    }

    private fun initSystem() {
        //添加人设
//        conversationHistory.add(ChatMessage("system", SystemPrompt.LyricsCreator))
    }

    fun chat(content: String) {
        chatJob?.cancel()
        chatJob = viewModelScope.launch(Dispatchers.IO) {
            //1.添加用户新消息
            conversationHistory.add(ChatMessage("user", content))
            chatMessagesFlow.value += ChatMessage("user", content)

            GiteeAiApiService.request(
                model = chatModel,
                apiKey = "H9ONNXBJNKIANVLDQTSSKJVDBDPXDTT7PGRJU91K",
                messages = conversationHistory.toList(),
                onResponse = {
                    if (it.choices.isNotEmpty()) {
                        conversationHistory.add(it.choices[0].message)
                        chatMessagesFlow.value += it.choices[0].message
                    }
                },
                onError = {
                    conversationHistory.removeLastOrNull()
                    chatMessagesFlow.value += ChatMessage("system", it.toString())
                }
            )
        }
    }

    fun clearConversation() {
        chatJob?.cancel()
        conversationHistory.clear()
        chatMessagesFlow.value = listOf()
    }

    fun saveConversation() {
        val gson = Gson()
        val historyJson = gson.toJson(conversationHistory)
        Log.d("xxx", "saveConversation: " + historyJson)
    }

    fun setModel(model: String) {
        chatJob?.cancel()
        conversationHistory.clear()
        initSystem()
        chatModel = model
    }
}