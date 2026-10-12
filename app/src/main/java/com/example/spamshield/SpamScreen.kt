package com.example.spamshield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SpamScreen(viewModel: SpamViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.messages.collectAsState()
    val spamMessages by viewModel.spamMessages.collectAsState()
    val flaggedMessages by viewModel.flaggedMessages.collectAsState()
    val blockedThreads by viewModel.blockedThreads.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SpamShield",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.importSystemMessages() }) {
                Text("Import SMS")
            }
            OutlinedButton(onClick = { /* refresh occurs via flow */ }) {
                Text("Refresh")
            }
        }

        Text("Total messages: ${messages.size}")
        Text("Spam: ${spamMessages.size} | Flagged: ${flaggedMessages.size} | Blocked threads: ${blockedThreads.size}")

        if (blockedThreads.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Blocked threads", style = MaterialTheme.typography.titleMedium)
                    blockedThreads.forEach { blocked ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("thread ${blocked.threadId}")
                            OutlinedButton(onClick = { viewModel.unblockThread(blocked.threadId) }) {
                                Text("Unblock")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("Messages", style = MaterialTheme.typography.titleLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages) { record ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(record.sender, fontWeight = FontWeight.Bold)
                        Text(record.body.ifBlank { "(empty message)" })
                        Text("Source: ${record.source} • Thread: ${record.threadId}")
                        if (record.tags.isNotEmpty()) {
                            Text("Tags: ${record.tags.joinToString()}")
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.toggleSpam(record.id, !record.isSpam) }) {
                                Text(if (record.isSpam) "Mark safe" else "Mark spam")
                            }
                            OutlinedButton(onClick = { viewModel.toggleFlag(record.id, !record.isFlagged) }) {
                                Text(if (record.isFlagged) "Unflag" else "Flag")
                            }
                            OutlinedButton(onClick = { viewModel.blockThread(record.threadId) }) {
                                Text("Block thread")
                            }
                            OutlinedButton(onClick = { viewModel.deleteThread(record.threadId) }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
