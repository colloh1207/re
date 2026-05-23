package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.Chat
import com.sdd.marketplace.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChats(): Flow<List<Chat>>
    fun getChat(chatId: String): Flow<Chat>
    fun getMessages(chatId: String): Flow<List<Message>>
    fun getUnreadCount(): Flow<Int>
    suspend fun getOrCreateChat(otherUserId: String, productId: String? = null): Result<Chat>
    suspend fun sendMessage(chatId: String, content: String, type: String = "TEXT", replyToId: String? = null): Result<Message>
    suspend fun sendImageMessage(chatId: String, imagePath: String): Result<Message>
    suspend fun sendLocationMessage(chatId: String, lat: Double, lng: Double, address: String): Result<Message>
    suspend fun editMessage(messageId: String, newContent: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun unsendMessage(messageId: String): Result<Unit>
    suspend fun markChatRead(chatId: String): Result<Unit>
    suspend fun blockUser(userId: String): Result<Unit>
    suspend fun reportUser(userId: String, reason: String): Result<Unit>
    fun observeTypingStatus(chatId: String): Flow<String?>
    suspend fun sendTypingIndicator(chatId: String, isTyping: Boolean)
    fun observeOnlineStatus(userId: String): Flow<Boolean>
    fun searchUsers(query: String): Flow<List<com.sdd.marketplace.domain.model.User>>
}
