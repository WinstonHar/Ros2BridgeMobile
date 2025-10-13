package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.*
import com.examples.testros2jsbridge.data.local.database.entities.PublisherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PublisherDao {
    @Query("SELECT * FROM publishers")
    fun getAllPublishersFlow(): Flow<List<PublisherEntity>>

    @Query("SELECT * FROM publishers")
    suspend fun getAllPublishers(): List<PublisherEntity>

    @Query("SELECT * FROM publishers WHERE id = :id")
    suspend fun getPublisherById(id: Int): PublisherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPublisher(publisher: PublisherEntity): Long

    @Update
    suspend fun updatePublisher(publisher: PublisherEntity)

    @Delete
    suspend fun deletePublisher(publisher: PublisherEntity)

    @Query("DELETE FROM publishers WHERE id = :id")
    suspend fun deletePublisherById(id: Int)

    @Query("DELETE FROM publishers")
    suspend fun deleteAllPublishers()
}
