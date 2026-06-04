package com.elephantcos.gemmachat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.elephantcos.gemmachat.GemmaChatApp
import com.elephantcos.gemmachat.data.entity.Conversation
import com.elephantcos.gemmachat.repository.ChatRepository
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(
        (application as GemmaChatApp).database.conversationDao(),
        application.database.messageDao()
    )

    val conversations = repository.conversations

    fun createConversation(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createConversation()
            onCreated(id)
        }
    }

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            repository.deleteConversation(conversation)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>, extras: CreationExtras
            ): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return HomeViewModel(app) as T
            }
        }
    }
}
