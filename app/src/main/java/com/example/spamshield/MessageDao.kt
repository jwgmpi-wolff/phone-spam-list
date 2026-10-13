package com.example.spamshield

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageRecord>)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageRecord>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MessageRecord?

    @Query("SELECT * FROM messages WHERE isSpam = 1 ORDER BY timestamp DESC")
    fun getSpamMessages(): Flow<List<MessageRecord>>

    @Query("SELECT * FROM messages WHERE isFlagged = 1 ORDER BY timestamp DESC")
    fun getFlaggedMessages(): Flow<List<MessageRecord>>

    @Query("SELECT * FROM messages WHERE isBlocked = 1 ORDER BY timestamp DESC")
    fun getBlockedMessages(): Flow<List<MessageRecord>>

    @Query("SELECT * FROM messages WHERE threadId = :threadId ORDER BY timestamp DESC")
    fun getThreadMessages(threadId: Long): Flow<List<MessageRecord>>

    @Query("SELECT DISTINCT threadId, sender FROM messages WHERE isSpam = 1 OR isFlagged = 1 ORDER BY sender ASC")
    fun getSpamThreads(): Flow<List<ThreadSummary>>

    @Update
    suspend fun update(message: MessageRecord)

    @Delete
    suspend fun delete(message: MessageRecord)

    @Query("DELETE FROM messages WHERE threadId = :threadId")
    suspend fun deleteThreadMessages(threadId: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM messages WHERE isSpam = 1")
    suspend fun deleteAllSpamMessages(): Int

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}

data class ThreadSummary(
    val threadId: Long,
    val sender: String
)
