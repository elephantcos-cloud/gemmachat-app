package com.elephantcos.gemmachat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.elephantcos.gemmachat.GemmaChatApp
import com.elephantcos.gemmachat.llm.LlmManager
import com.elephantcos.gemmachat.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(
    application: Application,
    private val conversationId: Long
) : AndroidViewModel(application) {

    private val app get() = getApplication<GemmaChatApp>()
    private val repository = ChatRepository(
        app.database.conversationDao(),
        app.database.messageDao()
    )

    val messages = repository.getMessages(conversationId)

    private val _isGenerating    = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingText   = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _conversationTitle = MutableStateFlow("New Chat")
    val conversationTitle: StateFlow<String> = _conversationTitle.asStateFlow()

    private val _error           = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isModelLoading  = MutableStateFlow(false)
    val isModelLoading: StateFlow<Boolean> = _isModelLoading.asStateFlow()

    init {
        viewModelScope.launch {
            val conv = repository.getConversationById(conversationId)
            _conversationTitle.value = conv?.title ?: "New Chat"
        }
        loadModel()
    }

    private fun loadModel() {
        if (LlmManager.isLoaded()) return
        viewModelScope.launch {
            _isModelLoading.value = true
            val prefs = app.getSharedPreferences("gemmachat_prefs", 0)
            val modelPath = prefs.getString("model_path", null)
            if (modelPath == null) {
                _error.value = "Model path not configured."
                _isModelLoading.value = false
                return@launch
            }
            try {
                withContext(Dispatchers.IO) {
                    LlmManager.getInstance(app, modelPath)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load model: ${e.message}"
            } finally {
                _isModelLoading.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            _error.value = null

            val llm = LlmManager.get()
            if (llm == null) {
                _error.value = "Model not ready. Please wait for it to load."
                return@launch
            }

            _isGenerating.value = true

            // Save user message
            repository.addMessage(conversationId, "user", text)

            // Update title on first message
            val allMessages = repository.getMessagesSync(conversationId)
            if (allMessages.size == 1) {
                val title = text.take(32) + if (text.length > 32) "…" else ""
                repository.updateConversationTitle(conversationId, title)
                _conversationTitle.value = title
            }

            // Build Gemma IT prompt from history
            val history = allMessages.map { Pair(it.role, it.content) }
            val prompt = llm.buildPrompt(history)

            _streamingText.value = ""
            try {
                val response = llm.generate(prompt) { token ->
                    _streamingText.value += token
                }
                repository.addMessage(conversationId, "model", response)
            } catch (e: Exception) {
                _error.value = "Generation error: ${e.message}"
            } finally {
                _streamingText.value = ""
                _isGenerating.value = false
            }
        }
    }

    companion object {
        fun Factory(conversationId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(
                    modelClass: Class<T>, extras: CreationExtras
                ): T {
                    val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                    return ChatViewModel(app as Application, conversationId) as T
                }
            }
    }
}
