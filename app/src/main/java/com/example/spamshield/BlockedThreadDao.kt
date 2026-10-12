package com.example.spamshield

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedThreadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thread: BlockedThread): Long

    @Query("SELECT * FROM blocked_threads ORDER BY createdAt DESC")
    suspend fun getAll(): List<BlockedThread>

    @Query("SELECT * FROM blocked_threads WHERE threadId = :threadId LIMIT 1")
    suspend fun getByThreadId(threadId: Long): BlockedThread?

    @Delete
    suspend fun delete(thread: BlockedThread)

    @Query("DELETE FROM blocked_threads WHERE threadId = :threadId")
    suspend fun deleteByThreadId(threadId: Long)
}
