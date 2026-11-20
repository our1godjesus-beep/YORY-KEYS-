package app.yory.keys.ui

import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class YoryToolbarTool(val id: String, val title: String, val description: String, val icon: String, val required: Boolean = false)

@Composable
internal fun YoryToolbarCustomizationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("yory_user_settings", Context.MODE_PRIVATE) }
    val tools = remember {
        listOf(
            YoryToolbarTool("emoji", "Emoji", "Open emoji shortcuts", "☺"),
            YoryToolbarTool("language", "Language switch", "Change the active keyboard layout", "⇄", true),
            YoryToolbarTool("symbols", "Symbols", "Switch to numbers and symbols", "123"),
            YoryToolbarTool("settings", "YORY Studio", "Open keyboard settings", "⚙", true),
            YoryToolbarTool("clipboard", "Clipboard", "Open local clipboard history when enabled", "▣")
        )
    }
    val order = remember {
        val defaults = tools.map { it.id }
        val stored = prefs.getString("toolbar_order", null)?.split(",")?.filter { it in defaults }.orEmpty()
        mutableStateListOf(*(stored.distinct() + defaults.filterNot(stored::contains)).toTypedArray())
    }
    val visible = remember {
        val defaults = tools.map { it.id }.toSet()
        val stored = prefs.getStringSet("toolbar_visible", null) ?: defaults
        mutableStateMapOf<String, Boolean>().apply {
            tools.forEach { tool -> put(tool.id, tool.required || tool.id in stored) }
        }
    }
    var status by remember { mutableStateOf("Toolbar changes are stored on this device.") }

    fun persist() {
        val selected = visible.filterValues { it }.keys.toSet() + setOf("language", "settings")
        prefs.edit()
            .putString("toolbar_order", order.joinToString(","))
            .putStringSet("toolbar_visible", selected)
            .apply()
        status = "Toolbar preferences saved. Reopen the keyboard to see the new order."
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
                TextButton(onClick = {
                    order.clear()
                    order.addAll(tools.map { it.id })
                    tools.forEach { visible[it.id] = it.required || it.id != "clipboard" }
                    persist()
                }) { Text("Reset", color = Color(0xFFA7C7FF)) }
            }
            Text("Keyboard toolbar", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Choose which shortcuts appear and arrange their order. Language switching and Studio access remain available so the keyboard is always manageable.", color = Color(0xFFB0B6C4), fontSize = 13.sp, lineHeight = 19.sp)

            tools.forEach { tool ->
                val index = order.indexOf(tool.id)
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.055f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.09f)),
                            contentAlignment = Alignment.Center
                        ) { Text(tool.icon, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tool.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(tool.description, color = Color(0xFFB0B6C4), fontSize = 11.sp)
                        }
                        Checkbox(
                            checked = visible[tool.id] == true || tool.required,
                            onCheckedChange = { checked -> if (!tool.required) { visible[tool.id] = checked; persist() } },
                            enabled = !tool.required
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Position ${index + 1}", color = Color(0xFFB0B6C4), fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            if (index > 0) {
                                val item = order.removeAt(index)
                                order.add(index - 1, item)
                                persist()
                            }
                        }, enabled = index > 0) { Text("Move up") }
                        TextButton(onClick = {
                            if (index >= 0 && index < order.lastIndex) {
                                val item = order.removeAt(index)
                                order.add(index + 1, item)
                                persist()
                            }
                        }, enabled = index >= 0 && index < order.lastIndex) { Text("Move down") }
                    }
                }
            }
            Text(status, color = Color(0xFFA7C7FF), fontSize = 12.sp)
            Button(onClick = { persist() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Save toolbar")
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
