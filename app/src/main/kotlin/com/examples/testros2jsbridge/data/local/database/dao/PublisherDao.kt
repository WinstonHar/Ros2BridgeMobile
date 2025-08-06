package com.examples.testros2jsbridge.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.examples.testros2jsbridge.data.local.database.entities.PublisherEntity

@Dao
abstract class PublisherDao {
    @Insert
    abstract suspend fun insertPublisher(publisher: PublisherEntity)

    @Update
    abstract suspend fun updatePublisher(publisher: PublisherEntity)

    @Query("SELECT * FROM publisher")
    abstract suspend fun getAllPublishers(): List<PublisherEntity>

    @Query("SELECT * FROM publisher WHERE id = :publisherId LIMIT 1")
    abstract suspend fun getPublisherById(publisherId: String): PublisherEntity?

    @Query("DELETE FROM publisher WHERE id = :publisherId")
    abstract suspend fun deletePublisherById(publisherId: String)

    @Query("DELETE FROM publisher")
    abstract suspend fun clearPublishers()
}