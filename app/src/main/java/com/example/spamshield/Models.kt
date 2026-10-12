package com.example.spamshield

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: Long,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val source: String,
    val isSpam: Boolean = false,
    val isFlagged: Boolean = false,
    val isBlocked: Boolean = false,
    val isDeleted: Boolean = false,
    val tags: List<String> = emptyList(),
    val appPackage: String = ""
)

@Entity(tableName = "blocked_threads")
data class BlockedThread(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: Long,
    val contactName: String,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)
