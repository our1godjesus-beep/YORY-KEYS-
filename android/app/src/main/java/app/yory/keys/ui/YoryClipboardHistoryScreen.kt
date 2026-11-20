package app.yory.keys.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.yory.keys.ime.ClipboardHistoryStore
import app.yory.keys.ime.YoryClipboardEntry

@Composable
internal fun YoryClipboardHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var items by remember { mutableStateOf(ClipboardHistoryStore.entries(context)) }
    var lastDeleted by remember { mutableStateOf<YoryClipboardEntry?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Clipboard history is encrypted and stays on this device.") }

    val visibleItems = items.filter { ClipboardHistoryPolicy.matchesSearch(it, searchQuery) }

    fun reload() {
        items = ClipboardHistoryStore.entries(context)
    }

    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF182236), Color(0xFF0A0D16), Color(0xFF121526)))
    )) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹  Back", color = Color(0xFFA7C7FF)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showClearDialog = true }, enabled = items.isNotEmpty()) {
                    Text("Clear all", color = Color(0xFFFFA0A0))
                }
            }
            Text("Clipboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Pinned clips stay until you remove them. Other clips expire after 24 hours.", color = Color(0xFFB0B6C4), fontSize = 13.sp, lineHeight = 19.sp)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search saved clips") },
                placeholder = { Text("Search text in clipboard history") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFA7C7FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                    focusedLabelColor = Color(0xFFA7C7FF),
                    unfocusedLabelColor = Color(0xFFB0B6C4)
                )
            )

            if (lastDeleted != null) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF243149))
                        .border(0.8.dp, Color(0xFFA7C7FF).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Clip deleted", color = Color.White, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        lastDeleted?.let { ClipboardHistoryStore.restore(context, it) }
                        lastDeleted = null
                        reload()
                        message = "Deleted clip restored."
                    }) { Text("Undo", color = Color(0xFFA7C7FF), fontWeight = FontWeight.SemiBold) }
                }
            }

            if (items.isEmpty()) {
                ClipboardPanel {
                    Text("No saved clips yet", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Copy text while the YORY keyboard is active and clipboard history is enabled. Sensitive patterns and one-time codes are filtered.", color = Color(0xFFB0B6C4), fontSize = 12.sp, lineHeight = 18.sp)
                }
            } else if (visibleItems.isEmpty()) {
                ClipboardPanel {
                    Text("No matching clips", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Try a different search term or clear the search.", color = Color(0xFFB0B6C4), fontSize = 12.sp)
                    TextButton(onClick = { searchQuery = "" }) { Text("Clear search", color = Color(0xFFA7C7FF)) }
                }
            } else {
                visibleItems.forEach { entry ->
                    ClipboardPanel {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    entry.text,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (entry.isPinned) "Pinned · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.copiedAt))
                                    else "Auto-clears · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.copiedAt)),
                                    color = Color(0xFFB0B6C4),
                                    fontSize = 10.sp
                                )
                            }
                            TextButton(onClick = {
                                ClipboardHistoryStore.setPinned(context, entry.id, !entry.isPinned)
                                reload()
                                message = if (entry.isPinned) "Clip unpinned." else "Clip pinned."
                            }) {
                                Text(if (entry.isPinned) "★" else "☆", color = Color(0xFFA7C7FF), fontSize = 18.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    manager.setPrimaryClip(ClipData.newPlainText("YORY clip", entry.text))
                                    message = "Clip copied to system clipboard."
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                            ) { Text("Copy again") }
                            OutlinedButton(
                                onClick = {
                                    lastDeleted = ClipboardHistoryStore.delete(context, entry.id)
                                    reload()
                                    message = "Clip deleted."
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                            ) { Text("Delete", color = Color(0xFFFFA0A0)) }
                        }
                    }
                }
            }
            Text(message, color = Color(0xFFA7C7FF), fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear clipboard history?") },
            text = { Text("This permanently removes all saved clips from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    ClipboardHistoryStore.clear(context)
                    items = emptyList()
                    lastDeleted = null
                    showClearDialog = false
                    message = "Clipboard history cleared."
                }) { Text("Clear", color = Color(0xFFFFA0A0)) }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ClipboardPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(17.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}
