package com.example.spamshield

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SmsRepository(
    private val appContext: Context,
    private val db: SpamDatabase
) {
    suspend fun importSmsThreads(): List<MessageRecord> {
        val records = mutableListOf<MessageRecord>()
        withContext(Dispatchers.IO) {
            val uri = Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            )

            val cursor: Cursor? = appContext.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdIndex = it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val address = it.getString(addressIndex) ?: "unknown"
                    val body = it.getString(bodyIndex) ?: ""
                    val threadId = it.getLong(threadIdIndex)
                    records.add(
                        MessageRecord(
                            threadId = threadId,
                            sender = address,
                            body = body,
                            timestamp = it.getLong(dateIndex),
                            source = "sms",
                            appPackage = "android.sms",
                            tags = detectSpamTags(body)
                        )
                    )
                }
            }
        }

        return records
    }

    suspend fun saveImportedMessages(messages: List<MessageRecord>) {
        if (messages.isEmpty()) return
        db.messageDao().insertAll(messages)
    }

    suspend fun toggleSpam(id: Long, value: Boolean) {
        val all = db.messageDao().getAllMessages().first() // Flow -> first emitted list
        val target = all.firstOrNull { it.id == id } ?: return
        db.messageDao().update(target.copy(isSpam = value, isFlagged = value || target.isFlagged))
    }

    suspend fun toggleFlag(id: Long, value: Boolean) {
        val all = db.messageDao().getAllMessages().first()
        val target = all.firstOrNull { it.id == id } ?: return
        db.messageDao().update(target.copy(isFlagged = value))
    }

    suspend fun blockThread(threadId: Long, reason: String) {
        val existing = db.blockedThreadDao().getByThreadId(threadId)
        if (existing == null) {
            db.blockedThreadDao().insert(BlockedThread(threadId = threadId, contactName = "Thread $threadId", reason = reason))
        }

        val threadMessages = db.messageDao().getThreadMessages(threadId).first()
        val updated = threadMessages.map { it.copy(isBlocked = true) }
        if (updated.isNotEmpty()) {
            db.messageDao().insertAll(updated)
        }
    }

    suspend fun unblockThread(threadId: Long) {
        db.blockedThreadDao().deleteByThreadId(threadId)
        val threadMessages = db.messageDao().getThreadMessages(threadId).first()
        db.messageDao().insertAll(threadMessages.map { it.copy(isBlocked = false) })
    }

    suspend fun deleteThread(threadId: Long) {
        db.messageDao().deleteThreadMessages(threadId)
        db.blockedThreadDao().deleteByThreadId(threadId)
    }

    fun detectSpamTags(text: String): List<String> {
        val candidate = text.lowercase()
        val tags = mutableListOf<String>()
        if (candidate.contains("free") || candidate.contains("win") || candidate.contains("claim")) tags += "promo"
        if (candidate.contains("click") || candidate.contains("link") || candidate.contains("urgent")) tags += "suspicious"
        if (candidate.contains("call me") || candidate.contains("limited time")) tags += "scam"
        return tags
    }

    companion object {
        private const val TAG = "SmsRepository"
    }
}
