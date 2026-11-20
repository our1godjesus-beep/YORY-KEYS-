package app.yory.keys.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.yory.keys.theme.YoryTheme
import app.yory.keys.theme.YoryThemeStore

@Composable
fun YoryStudioScreen() {
    val context = LocalContext.current
    var theme by remember { mutableStateOf(YoryThemeStore.load(context)) }
    var backgroundHex by remember { mutableStateOf(theme.backgroundArgb.toHexArgb()) }
    var keyHex by remember { mutableStateOf(theme.keyArgb.toHexArgb()) }
    var textHex by remember { mutableStateOf(theme.textArgb.toHexArgb()) }
    var accentHex by remember { mutableStateOf(theme.accentArgb.toHexArgb()) }
    var fileMessage by remember { mutableStateOf("Themes stay on this device unless you export them.") }

    fun applyTheme(next: YoryTheme) {
        theme = next
        backgroundHex = next.backgroundArgb.toHexArgb()
        keyHex = next.keyArgb.toHexArgb()
        textHex = next.textArgb.toHexArgb()
        accentHex = next.accentArgb.toHexArgb()
        YoryThemeStore.save(context, next)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(app.yory.keys.theme.YoryThemeFile.encode(theme))
                } ?: error("Could not open the selected destination.")
            }.onSuccess { fileMessage = "Theme exported successfully." }
                .onFailure { fileMessage = "Export failed: " + (it.message ?: "unknown error") }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Could not read the selected file.")
                app.yory.keys.theme.YoryThemeFile.decode(raw)
            }.onSuccess {
                applyTheme(it)
                fileMessage = "Theme imported and applied."
            }.onFailure {
                fileMessage = "Import failed: " + (it.message ?: "invalid theme")
            }
        }
    }

    fun parseHexArgb(value: String): Int? {
        val clean = value.trim().removePrefix("#")
        val normalized = when (clean.length) {
            6 -> "FF$clean"
            8 -> clean
            else -> return null
        }
        return normalized.toLongOrNull(16)?.toInt()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0D1019)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YORY KEYS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("YORY Studio", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                        Text("Make the keyboard yours.", color = Color(0xFF9CA7BD), fontSize = 14.sp)
                    }
                    Text("✦", color = Color(theme.accentArgb), fontSize = 34.sp)
                }

                KeyboardPreview(theme)

                Text("START WITH A LOOK", color = Color(0xFF9CA7BD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    YoryTheme.presets.forEach { preset ->
                        Box(
                            modifier = Modifier.weight(1f).height(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(preset.keyArgb))
                                .border(
                                    if (theme.name == preset.name) 2.dp else 1.dp,
                                    if (theme.name == preset.name) Color(preset.accentArgb) else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(13.dp)
                                )
                                .clickable { applyTheme(preset) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(preset.name.substringBefore(" "), color = Color(preset.textArgb), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text("ACCENT COLOR", color = Color(0xFF9CA7BD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(0xFF78B8F8, 0xFF9B8CFF, 0xFF51E5DC, 0xFFDE7297, 0xFFFFC66D, 0xFF8ED6A6).forEach { raw ->
                        val colorInt = raw.toInt()
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(colorInt))
                                .border(if (theme.accentArgb == colorInt) 3.dp else 1.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                                .clickable { applyTheme(theme.copy(accentArgb = colorInt)) }
                        )
                    }
                }

                Text("CUSTOM COLORS (HEX / ARGB)", color = Color(0xFF9CA7BD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                HexColorInput("Background", backgroundHex, { backgroundHex = it }) {
                    parseHexArgb(backgroundHex)?.let { color -> applyTheme(theme.copy(backgroundArgb = color, name = "Custom")) }
                }
                HexColorInput("Key surface", keyHex, { keyHex = it }) {
                    parseHexArgb(keyHex)?.let { color -> applyTheme(theme.copy(keyArgb = color, name = "Custom")) }
                }
                HexColorInput("Key labels", textHex, { textHex = it }) {
                    parseHexArgb(textHex)?.let { color -> applyTheme(theme.copy(textArgb = color, name = "Custom")) }
                }
                HexColorInput("Accent", accentHex, { accentHex = it }) {
                    parseHexArgb(accentHex)?.let { color -> applyTheme(theme.copy(accentArgb = color, name = "Custom")) }
                }

                SliderSetting("Glass opacity", theme.opacity, 0.35f..1f, (theme.opacity * 100).toInt().toString() + "%") {
                    applyTheme(theme.copy(opacity = it))
                }
                SliderSetting("Key corner radius", theme.cornerRadiusDp, 4f..28f, theme.cornerRadiusDp.toInt().toString() + " dp") {
                    applyTheme(theme.copy(cornerRadiusDp = it))
                }
                SliderSetting("Key height", theme.keyHeightDp, 36f..60f, theme.keyHeightDp.toInt().toString() + " dp") {
                    applyTheme(theme.copy(keyHeightDp = it))
                }

                Text("THEME FILES", color = Color(0xFF9CA7BD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("YORY-" + theme.name.replace(Regex("[^A-Za-z0-9]+"), "-") + ".yorytheme") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export .yorytheme") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import theme") }
                }
                Text(fileMessage, color = Color(0xFF8994AA), fontSize = 11.sp)

                Text("SOUND & TOUCH", color = Color(0xFF9CA7BD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                SwitchSetting("Key click sound", theme.soundEnabled) {
                    applyTheme(theme.copy(soundEnabled = it))
                }
                SwitchSetting("Haptic feedback", theme.hapticsEnabled) {
                    applyTheme(theme.copy(hapticsEnabled = it))
                }
                SwitchSetting("Local clipboard history", theme.clipboardEnabled) {
                    applyTheme(theme.copy(clipboardEnabled = it))
                }
                Text(
                    "Clipboard history is optional, stays on this device, and filters common one-time codes. Review and clear saved items from the keyboard.",
                    color = Color(0xFF8994AA), fontSize = 11.sp
                )

                Button(onClick = { context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable YORY KEYS")
                }
                OutlinedButton(
                    onClick = {
                        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        manager.showInputMethodPicker()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Choose YORY KEYS", color = Color.White) }
                Text(
                    "Your theme is saved on this device. Typing content is not sent to a server.",
                    color = Color(0xFF8994AA), fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun KeyboardPreview(theme: YoryTheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(theme.backgroundArgb).copy(alpha = theme.opacity)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                listOf("☻", "✦", "文", "⚙", "•••").forEach {
                    Text(it, color = Color(theme.accentArgb), fontSize = 17.sp)
                }
            }
            listOf("qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { char ->
                        Box(
                            modifier = Modifier.weight(1f).height((theme.keyHeightDp * 0.68f).dp)
                                .clip(RoundedCornerShape(theme.cornerRadiusDp.dp))
                                .background(Color(theme.keyArgb))
                                .border(0.7.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(theme.cornerRadiusDp.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(char.toString(), color = Color(theme.textArgb), fontSize = 13.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("!#1", "◎", "English", "↵").forEach { label ->
                    Box(
                        modifier = Modifier.weight(if (label == "English") 3f else 1f).height(32.dp)
                            .clip(RoundedCornerShape(theme.cornerRadiusDp.dp)).background(Color(theme.keyArgb)),
                        contentAlignment = Alignment.Center
                    ) { Text(label, color = Color(theme.textArgb), fontSize = 10.sp) }
                }
            }
        }
    }
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(valueLabel, color = Color(0xFF9CA7BD), fontSize = 12.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}


@Composable
private fun HexColorInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onApply: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodySmall
        )
        Button(onClick = onApply) { Text("Apply") }
    }
}

@Composable
private fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun Int.toHexArgb(): String = "#%08X".format(this)
