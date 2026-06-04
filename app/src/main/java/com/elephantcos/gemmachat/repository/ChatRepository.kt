package com.elephantcos.gemmachat.repository

import com.elephantcos.gemmachat.data.dao.ConversationDao
import com.elephantcos.gemmachat.data.dao.MessageDao
import com.elephantcos.gemmachat.data.entity.Conversation
import com.elephantcos.gemmachat.data.entity.Message
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    val conversations: Flow<List<Conversation>> = conversationDao.getAllConversations()

    fun getMessages(convId: Long): Flow<List<Message>> =
        messageDao.getMessagesForConversation(convId)

    suspend fun getMessagesSync(convId: Long): List<Message> =
        messageDao.getMessagesSync(convId)

    suspend fun getConversationById(id: Long): Conversation? =
        conversationDao.getById(id)

    suspend fun createConversation(title: String = "New Chat"): Long =
        conversationDao.insert(Conversation(title = title))

    suspend fun updateConversationTitle(id: Long, title: String) {
        val conv = conversationDao.getById(id) ?: return
        conversationDao.update(conv.copy(title = title, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(conversation: Conversation) {
        messageDao.deleteForConversation(conversation.id)
        conversationDao.delete(conversation)
    }

    suspend fun addMessage(convId: Long, role: String, content: String): Long =
        messageDao.insert(Message(conversationId = convId, role = role, content = content))
}
