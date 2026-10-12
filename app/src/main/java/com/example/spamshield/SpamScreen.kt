package com.example.spamshield

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun SpamScreen(viewModel: SpamViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.messages.collectAsState()
    val spamMessages by viewModel.spamMessages.collectAsState()
    val flaggedMessages by viewModel.flaggedMessages.collectAsState()
    val blockedThreads by viewModel.blockedThreads.collectAsState()
    val context = LocalContext.current
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionMessage = null
            viewModel.importSystemMessages()
        } else {
            permissionMessage = "SMS permission was denied. Allow it in Android settings to import messages."
        }
    }
    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionMessage = null
            viewModel.importSystemCalls()
        } else {
            permissionMessage = "Call-log permission was denied. Allow it in Android settings to import call history."
        }
    }

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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_SMS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionMessage = null
                        viewModel.importSystemMessages()
                    } else {
                        smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                    }
                }
            ) {
                Text("Import SMS")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_CALL_LOG
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionMessage = null
                        viewModel.importSystemCalls()
                    } else {
                        callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    }
                }
            ) {
                Text("Import call history")
            }
        }

        permissionMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Text("Total records: ${messages.size}")
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
                        Text(
                            if (record.source == "call") {
                                "Call history • ${java.text.DateFormat.getDateTimeInstance().format(record.timestamp)}"
                            } else {
                                "Source: ${record.source} • Thread: ${record.threadId}"
                            }
                        )
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
                            if (record.source == "sms") {
                                OutlinedButton(onClick = { viewModel.blockThread(record.threadId) }) {
                                    Text("Block thread")
                                }
                                OutlinedButton(onClick = { viewModel.deleteThread(record.threadId) }) {
                                    Text("Delete thread")
                                }
                            } else {
                                OutlinedButton(onClick = { viewModel.deleteThread(record.threadId) }) {
                                    Text("Delete call record")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
