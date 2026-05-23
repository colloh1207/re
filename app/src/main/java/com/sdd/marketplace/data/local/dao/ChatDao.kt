package com.sdd.marketplace.data.local.dao

import androidx.room.*
import com.sdd.marketplace.data.local.entities.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id")
    fun getChatById(id: String): Flow<ChatEntity?>

    @Query("SELECT SUM(unreadCount) FROM chats")
    fun getTotalUnreadCount(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markChatRead(chatId: String)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteChat(id: String)
}
