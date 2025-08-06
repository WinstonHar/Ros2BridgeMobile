package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SubscriberDao {
    @Insert
    abstract suspend fun insertSubscriber(subscriber: SubscriberEntity)

    @Update
    abstract suspend fun updateSubscriber(subscriber: SubscriberEntity)

    @Query("SELECT * FROM subscriber")
    abstract suspend fun getAllSubscribers(): List<SubscriberEntity>

    @Query("SELECT * FROM subscriber")
    abstract fun getAllSubscribersFlow(): Flow<List<SubscriberEntity>>

    @Query("SELECT * FROM subscriber WHERE topic = :topicId LIMIT 1")
    abstract suspend fun getSubscriberByTopic(topicId: String): SubscriberEntity?

    @Query("DELETE FROM subscriber WHERE topic = :topicId")
    abstract suspend fun deleteSubscriberByTopic(topicId: String)

    @Query("DELETE FROM subscriber")
    abstract suspend fun clearSubscribers()
}