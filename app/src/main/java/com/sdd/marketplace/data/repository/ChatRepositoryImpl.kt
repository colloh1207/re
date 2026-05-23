package com.sdd.marketplace.data.repository

import com.sdd.marketplace.BuildConfig
import com.sdd.marketplace.data.local.dao.ChatDao
import com.sdd.marketplace.data.local.dao.MessageDao
import com.sdd.marketplace.data.local.entities.ChatEntity
import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.data.mappers.toEntity
import com.sdd.marketplace.data.remote.dto.ChatDto
import com.sdd.marketplace.data.remote.dto.MessageDto
import com.sdd.marketplace.data.remote.dto.SendMessageRequest
import com.sdd.marketplace.domain.model.Chat
import com.sdd.marketplace.domain.model.Message
import com.sdd.marketplace.domain.repository.AuthRepository
import com.sdd.marketplace.domain.repository.ChatRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val storage: Storage,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val authRepository: AuthRepository
) : ChatRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeChannels = mutableMapOf<String, RealtimeChannel>()
    private val _typingStatus = MutableStateFlow<Map<String, String?>>(emptyMap())

    override fun getChat(chatId: String): Flow<Chat> = flow {
        try {
            val dto = postgrest["chats"].select { filter { eq("id", chatId) } }.decodeSingle<ChatDto>()
            chatDao.insertChat(dto.toChatEntity())
        } catch (e: Exception) { Timber.e(e) }
        emitAll(chatDao.getChatById(chatId).mapNotNull { it?.toDomain() })
    }

    override fun getChats(): Flow<List<Chat>> = flow {
        val userId = authRepository.getCurrentUserId() ?: return@flow
        try {
            val dtos = postgrest["chats"].select {
                filter { or { eq("participant1_id", userId); eq("participant2_id", userId) } }
                order("updated_at", Order.DESCENDING)
            }.decodeList<ChatDto>()
            dtos.forEach { dto ->
                chatDao.insertChat(dto.toChatEntity())
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching chats")
        }
        emitAll(chatDao.getAllChats().map { entities ->
            entities.map { it.toDomain() }
        })
    }

    override fun getMessages(chatId: String): Flow<List<Message>> = flow {
        subscribeToMessages(chatId)
        try {
            val dtos = postgrest["messages"].select {
                filter { eq("chat_id", chatId) }
                order("sent_at", Order.ASCENDING)
            }.decodeList<MessageDto>()
            messageDao.insertMessages(dtos.map { it.toDomain().toEntity() })
        } catch (e: Exception) {
            Timber.e(e, "Error fetching messages")
        }
        emitAll(messageDao.getMessagesByChatId(chatId).map { list -> list.map { it.toDomain() } })
    }

    override fun getUnreadCount(): Flow<Int> =
        chatDao.getTotalUnreadCount().map { it ?: 0 }

    override suspend fun getOrCreateChat(otherUserId: String, productId: String?): Result<Chat> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        val existing = try {
            postgrest["chats"].select {
                filter {
                    or {
                        and { eq("participant1_id", userId); eq("participant2_id", otherUserId) }
                        and { eq("participant1_id", otherUserId); eq("participant2_id", userId) }
                    }
                }
            }.decodeSingleOrNull<ChatDto>()
        } catch (e: Exception) { null }

        if (existing != null) return@runCatching existing.toDomain()

        val newChat = mapOf(
            "participant1_id" to userId,
            "participant2_id" to otherUserId,
            "product_id" to productId
        )
        val created = postgrest["chats"].insert(newChat) { select() }.decodeSingle<ChatDto>()
        created.toDomain()
    }

    override suspend fun editMessage(messageId: String, newContent: String): Result<Unit> = runCatching {
        postgrest["messages"].update(mapOf(
            "content" to newContent,
            "edited_at" to java.time.Instant.now().toString()
        )) { filter { eq("id", messageId) } }
        messageDao.updateMessageContent(messageId, newContent)
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = runCatching {
        postgrest["messages"].update(mapOf("is_deleted" to true)) { filter { eq("id", messageId) } }
        messageDao.markMessageDeleted(messageId)
    }

    override suspend fun unsendMessage(messageId: String): Result<Unit> = runCatching {
        postgrest["messages"].update(mapOf("is_unsent" to true, "content" to "")) { filter { eq("id", messageId) } }
        messageDao.markMessageUnsent(messageId)
    }

    override fun searchUsers(query: String): kotlinx.coroutines.flow.Flow<List<com.sdd.marketplace.domain.model.User>> = flow {
        try {
            val dtos = postgrest["users"].select {
                filter { ilike("full_name", "%$query%") }
                limit(10)
            }.decodeList<com.sdd.marketplace.data.remote.dto.UserDto>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override suspend fun sendMessage(chatId: String, content: String, type: String, replyToId: String?): Result<Message> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        val sanitizedContent = if (type == "text") com.sdd.marketplace.core.util.SanitizationUtils.sanitizeChatMessage(content) else content
        val request = SendMessageRequest(
            chatId = chatId, senderId = userId, content = sanitizedContent, type = type, replyToMessageId = replyToId
        )
        val created = postgrest["messages"].insert(request) { select() }.decodeSingle<MessageDto>()
        postgrest["chats"].update(mapOf(
            "last_message_content" to content,
            "last_message_sent_at" to created.sentAt,
            "updated_at" to created.sentAt
        )) { filter { eq("id", chatId) } }
        val message = created.toDomain()
        messageDao.insertMessage(message.toEntity())
        message
    }

    override suspend fun sendImageMessage(chatId: String, imagePath: String): Result<Message> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        val file = File(imagePath)
        val fileName = "chats/$chatId/${System.currentTimeMillis()}.jpg"
        storage.from(BuildConfig.BUCKET_CHAT).upload(fileName, file.readBytes()) {
            upsert = true
        }
        val imageUrl = storage.from(BuildConfig.BUCKET_CHAT).publicUrl(fileName)
        val request = SendMessageRequest(
            chatId = chatId, senderId = userId, content = "📷 Image",
            type = "IMAGE", imageUrl = imageUrl
        )
        val created = postgrest["messages"].insert(request) { select() }.decodeSingle<MessageDto>()
        val message = created.toDomain()
        messageDao.insertMessage(message.toEntity())
        message
    }

    override suspend fun sendLocationMessage(chatId: String, lat: Double, lng: Double, address: String): Result<Message> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        val request = SendMessageRequest(
            chatId = chatId, senderId = userId, content = "📍 Location",
            type = "LOCATION", latitude = lat, longitude = lng, locationAddress = address
        )
        val created = postgrest["messages"].insert(request) { select() }.decodeSingle<MessageDto>()
        val message = created.toDomain()
        messageDao.insertMessage(message.toEntity())
        message
    }

    override suspend fun markChatRead(chatId: String): Result<Unit> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        postgrest["messages"].update(mapOf("is_read" to true)) {
            filter { eq("chat_id", chatId) }
        }
        chatDao.markChatRead(chatId)
        messageDao.markMessagesRead(chatId)
    }

    override suspend fun blockUser(userId: String): Result<Unit> = runCatching {
        val currentUserId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        postgrest["blocked_users"].insert(mapOf(
            "blocker_id" to currentUserId,
            "blocked_id" to userId
        ))
    }

    override suspend fun reportUser(userId: String, reason: String): Result<Unit> = runCatching {
        val currentUserId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        postgrest["reports"].insert(mapOf(
            "reporter_id" to currentUserId,
            "reported_id" to userId,
            "reason" to reason
        ))
    }

    override fun observeTypingStatus(chatId: String): Flow<String?> =
        _typingStatus.map { it[chatId] }

    override suspend fun sendTypingIndicator(chatId: String, isTyping: Boolean) {
        try {
            val userId = authRepository.getCurrentUserId() ?: return
            val channel = activeChannels[chatId] ?: return
            channel.broadcast("typing", buildJsonObject {
                put("user_id", userId)
                put("is_typing", isTyping)
            })
        } catch (e: Exception) {
            Timber.e(e, "Error sending typing indicator")
        }
    }

    override fun observeOnlineStatus(userId: String): Flow<Boolean> = flow {
        emit(false)
    }

    private fun subscribeToMessages(chatId: String) {
        if (activeChannels.containsKey(chatId)) return
        scope.launch {
            try {
                val channel = realtime.channel("messages:$chatId")
                activeChannels[chatId] = channel

                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                    filter("chat_id", FilterOperator.EQ, chatId)
                }.collect { action ->
                    try {
                        val dto = action.decodeRecord<MessageDto>()
                        messageDao.insertMessage(dto.toDomain().toEntity())
                    } catch (e: Exception) {
                        Timber.e(e, "Error decoding message")
                    }
                }
                channel.subscribe()
            } catch (e: Exception) {
                Timber.e(e, "Error subscribing to messages")
            }
        }
    }

    private fun ChatDto.toChatEntity() = ChatEntity(
        id = id,
        participantIds = participants.joinToString(",") { it.id },
        participantNamesJson = participants.joinToString(",") { it.fullName },
        lastMessageContent = lastMessage?.content,
        lastMessageSentAt = lastMessage?.sentAt,
        lastMessageType = lastMessage?.type,
        unreadCount = unreadCount,
        productId = productId,
        productTitle = product?.title,
        productImageUrl = product?.images?.firstOrNull(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
