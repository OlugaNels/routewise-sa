package com.routewise.sa.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routewise.sa.data.remote.RetrofitClient
import com.routewise.sa.data.remote.dto.AiChatRequestDto
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatbotViewModel : ViewModel() {

    private val _messages = MutableLiveData<MutableList<ChatMessage>>(
        mutableListOf(
            ChatMessage(
                sender = "ai",
                text = "Sawubona! I am your RouteWise AI Scout. Ask me anything about traffic conditions, load shedding robot outages, N1/N3 toll plazas, or the safest scenic corridors."
            )
        )
    )
    val messages: LiveData<MutableList<ChatMessage>> = _messages

    private val _isSending = MutableLiveData<Boolean>(false)
    val isSending: LiveData<Boolean> = _isSending

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        val current = _messages.value ?: mutableListOf()
        current.add(ChatMessage(sender = "user", text = prompt))
        _messages.value = current

        _isSending.value = true
        viewModelScope.launch {
            try {
                val response = RetrofitClient.aiService.askRoadAssistant(AiChatRequestDto(prompt = prompt))
                val reply = response.body()?.responseText
                    ?: "Sorry, the assistant could not answer that right now. Check the Alerts and Map tabs for live road reports."
                current.add(ChatMessage(sender = "ai", text = reply))
                _messages.postValue(current)
            } catch (e: Exception) {
                // Never invent traffic information: say honestly that the assistant is unavailable
                current.add(ChatMessage(sender = "ai", text = "Sorry, the assistant is not available right now. Check the Alerts and Map tabs for live road reports."))
                _messages.postValue(current)
            } finally {
                _isSending.postValue(false)
            }
        }
    }
}
