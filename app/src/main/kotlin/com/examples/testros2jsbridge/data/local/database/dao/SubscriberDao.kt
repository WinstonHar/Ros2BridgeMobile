package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.*
import com.examples.testros2jsbridge.data.local.database.entities.SubscriberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriberDao {
    @Query("SELECT * FROM subscribers")
    fun getAllSubscribersFlow(): Flow<List<SubscriberEntity>>

    @Query("SELECT * FROM subscribers")
    suspend fun getAllSubscribers(): List<SubscriberEntity>

    @Query("SELECT * FROM subscribers WHERE id = :id")
    suspend fun getSubscriberById(id: Int): SubscriberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriber(subscriber: SubscriberEntity): Long

    @Update
    suspend fun updateSubscriber(subscriber: SubscriberEntity)

    @Delete
    suspend fun deleteSubscriber(subscriber: SubscriberEntity)

    @Query("DELETE FROM subscribers WHERE id = :id")
    suspend fun deleteSubscriberById(id: Int)

    @Query("DELETE FROM subscribers")
    suspend fun deleteAllSubscribers()
}
