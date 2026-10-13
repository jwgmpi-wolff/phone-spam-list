package com.example.spamshield

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun SpamScreen(viewModel: SpamViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.messages.collectAsState()
    val spamMessages by viewModel.spamMessages.collectAsState()
    val flaggedMessages by viewModel.flaggedMessages.collectAsState()
    val blockedThreads by viewModel.blockedThreads.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteSpamConfirmation by remember { mutableStateOf(false) }
    var recipient by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var sendMessage by remember { mutableStateOf<String?>(null) }
    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        attachmentUri = uri
    }
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

    if (showDeleteSpamConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteSpamConfirmation = false },
            title = { Text("Delete all spam-marked records?") },
            text = {
                Text(
                    "This will permanently remove ${spamMessages.size} spam-marked record(s) from SpamShield. " +
                        "It will not delete the original SMS from Google Messages or Samsung Messages."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllSpamMessages()
                        showDeleteSpamConfirmation = false
                    }
                ) {
                    Text("Delete local records")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteSpamConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SpamShield",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Compose a message, then review and send it in your messaging app. " +
                        "Image and video MMS availability depends on your messaging app, carrier, and plan."
                )
                Text(
                    "Spam and block labels are stored only in SpamShield. They do not change Google Messages, Samsung Messages, or Phone app settings.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("New message", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Recipient phone number") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Message") }
                    )
                    attachmentUri?.let { uri ->
                        Text(
                            "Attachment: ${context.contentResolver.getType(uri) ?: "media"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedButton(onClick = { attachmentUri = null }) {
                            Text("Remove attachment")
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            attachmentPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        }
                    ) {
                        Text("Attach image or video")
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = recipient.isNotBlank() &&
                            (messageText.isNotBlank() || attachmentUri != null),
                        onClick = {
                            try {
                                val intent = if (attachmentUri == null) {
                                    Intent(
                                        Intent.ACTION_SENDTO,
                                        Uri.parse("smsto:${Uri.encode(recipient.trim())}")
                                    ).putExtra("sms_body", messageText)
                                } else {
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = context.contentResolver.getType(attachmentUri!!) ?: "*/*"
                                        putExtra(Intent.EXTRA_STREAM, attachmentUri)
                                        putExtra(Intent.EXTRA_TEXT, messageText)
                                        putExtra(Intent.EXTRA_PHONE_NUMBER, recipient.trim())
                                        clipData = android.content.ClipData.newUri(
                                            context.contentResolver,
                                            "message attachment",
                                            attachmentUri
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                }
                                context.startActivity(Intent.createChooser(intent, "Send message"))
                                sendMessage = "Messaging app opened. Review the message and tap Send there."
                            } catch (_: ActivityNotFoundException) {
                                sendMessage = "No compatible messaging app was found."
                            }
                        }
                    ) {
                        Text(if (attachmentUri == null) "Continue to send SMS" else "Continue to send MMS")
                    }
                    sendMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
        }

        item {
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
        }

        permissionMessage?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Text("Total records: ${messages.size}")
        }
        item {
            Text("Spam: ${spamMessages.size} | Flagged: ${flaggedMessages.size} | Blocked threads: ${blockedThreads.size}")
        }
        if (spamMessages.isNotEmpty()) {
            item {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDeleteSpamConfirmation = true }
                ) {
                    Text("Delete all locally spam-marked records (${spamMessages.size})")
                }
            }
        }

        if (blockedThreads.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Locally tracked blocked threads", style = MaterialTheme.typography.titleMedium)
                        blockedThreads.forEach { blocked ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("thread ${blocked.threadId}")
                                OutlinedButton(onClick = { viewModel.unblockThread(blocked.threadId) }) {
                                    Text("Remove local block")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Messages and call history", style = MaterialTheme.typography.titleLarge)
        }

        items(messages, key = { "${it.source}:${it.id}" }) { record ->
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
                    val localStatuses = buildList {
                        if (record.isSpam) add("Spam in SpamShield only")
                        if (record.isFlagged) add("Flagged in SpamShield only")
                        if (record.isBlocked) add("Thread tracked as blocked locally only")
                    }
                    if (localStatuses.isNotEmpty()) {
                        Text(
                            localStatuses.joinToString(" • "),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (record.tags.isNotEmpty()) {
                        Text("Tags: ${record.tags.joinToString()}")
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.toggleSpam(record.id, !record.isSpam) }) {
                            Text(if (record.isSpam) "Remove local spam label" else "Mark spam locally")
                        }
                        OutlinedButton(onClick = { viewModel.toggleFlag(record.id, !record.isFlagged) }) {
                            Text(if (record.isFlagged) "Remove local flag" else "Flag locally")
                        }
                        if (record.source == "sms") {
                            OutlinedButton(
                                onClick = {
                                    if (record.isBlocked) {
                                        viewModel.unblockThread(record.threadId)
                                    } else {
                                        viewModel.blockThread(record.threadId)
                                    }
                                }
                            ) {
                                Text(if (record.isBlocked) "Remove local thread block" else "Track thread as blocked locally")
                            }
                            OutlinedButton(onClick = { viewModel.deleteThread(record.threadId) }) {
                                Text("Delete local thread records")
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.deleteThread(record.threadId) }) {
                                Text("Delete local call record")
                            }
                        }
                    }
                }
            }
        }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            FilledTonalButton(
                onClick = {
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                }
            ) {
                Text("Top")
            }
            FilledTonalButton(
                enabled = listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 0,
                onClick = {
                    val pageSize = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                    val previousPage = (listState.firstVisibleItemIndex - pageSize).coerceAtLeast(0)
                    coroutineScope.launch { listState.animateScrollToItem(previousPage) }
                }
            ) {
                Text("Page up")
            }
            FilledTonalButton(
                enabled = listState.layoutInfo.totalItemsCount > 0 &&
                    listState.firstVisibleItemIndex < listState.layoutInfo.totalItemsCount - 1,
                onClick = {
                    val pageSize = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                    val nextPage = (listState.firstVisibleItemIndex + pageSize)
                        .coerceAtMost((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                    coroutineScope.launch { listState.animateScrollToItem(nextPage) }
                }
            ) {
                Text("Page down")
            }
        }
    }
}
