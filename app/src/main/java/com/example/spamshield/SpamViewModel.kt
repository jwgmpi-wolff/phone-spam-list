package com.example.spamshield

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpamViewModel(application: Application) : AndroidViewModel(application) {
    private val database = (application as SpamApplication).database
    private val repository = SmsRepository(application.applicationContext, database)

    val messages: StateFlow<List<MessageRecord>> =
        database.messageDao().getAllMessages().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val spamMessages: StateFlow<List<MessageRecord>> =
        database.messageDao().getSpamMessages().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val flaggedMessages: StateFlow<List<MessageRecord>> =
        database.messageDao().getFlaggedMessages().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _blockedThreads = MutableStateFlow<List<BlockedThread>>(emptyList())
    val blockedThreads: StateFlow<List<BlockedThread>> = _blockedThreads.asStateFlow()

    init {
        refreshBlockedThreads()
    }

    fun importSystemMessages() {
        viewModelScope.launch {
            repository.importSmsThreads().let { repository.saveImportedMessages(it) }
            refreshBlockedThreads()
        }
    }

    fun importSystemCalls() {
        viewModelScope.launch {
            repository.importCallHistory().let { repository.saveImportedMessages(it) }
        }
    }

    fun toggleSpam(id: Long, isSpam: Boolean) {
        viewModelScope.launch {
            repository.toggleSpam(id, isSpam)
        }
    }

    fun toggleFlag(id: Long, isFlagged: Boolean) {
        viewModelScope.launch {
            repository.toggleFlag(id, isFlagged)
        }
    }

    fun blockThread(threadId: Long) {
        viewModelScope.launch {
            repository.blockThread(threadId, "manual-review")
            refreshBlockedThreads()
        }
    }

    fun unblockThread(threadId: Long) {
        viewModelScope.launch {
            repository.unblockThread(threadId)
            refreshBlockedThreads()
        }
    }

    fun deleteThread(threadId: Long) {
        viewModelScope.launch {
            repository.deleteThread(threadId)
            refreshBlockedThreads()
        }
    }

    private fun refreshBlockedThreads() {
        viewModelScope.launch {
            _blockedThreads.value = database.blockedThreadDao().getAll()
        }
    }
}
