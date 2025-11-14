package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.examples.testros2jsbridge.data.local.database.entities.GeometryMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeometryMessageDao {
    @Query("SELECT * FROM geometry_messages")
    fun getAllMessagesFlow(): Flow<List<GeometryMessageEntity>>

    @Query("SELECT * FROM geometry_messages")
    suspend fun getAllMessages(): List<GeometryMessageEntity>

    @Query("SELECT * FROM geometry_messages WHERE id = :id")
    suspend fun getMessageById(id: Int): GeometryMessageEntity?

    @Query("SELECT * FROM geometry_messages WHERE topic = :topic ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessageForTopic(topic: String): GeometryMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: GeometryMessageEntity): Long

    @Update
    suspend fun updateMessage(message: GeometryMessageEntity)

    @Delete
    suspend fun deleteMessage(message: GeometryMessageEntity)

    @Query("DELETE FROM geometry_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Query("DELETE FROM geometry_messages")
    suspend fun deleteAllMessages()
}
