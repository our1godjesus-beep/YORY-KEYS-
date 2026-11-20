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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.yory.keys.ime.PersonalDictionaryStore
import app.yory.keys.theme.YoryTheme
import app.yory.keys.theme.YoryThemeStore

@Composable
internal fun YoryThemesEditorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var theme by remember { mutableStateOf(YoryThemeStore.load(context)) }
    var backgroundHex by remember { mutableStateOf(theme.backgroundArgb.toHexArgb()) }
    var keyHex by remember { mutableStateOf(theme.keyArgb.toHexArgb()) }
    var textHex by remember { mutableStateOf(theme.textArgb.toHexArgb()) }
    var accentHex by remember { mutableStateOf(theme.accentArgb.toHexArgb()) }
    var pressedHex by remember { mutableStateOf(theme.keyPressedArgb.toHexArgb()) }
    var fileMessage by remember { mutableStateOf("Themes stay on this device unless you export them.") }
    var savedThemeName by remember { mutableStateOf("My " + theme.name) }
    var personalLearningEnabled by remember { mutableStateOf(PersonalDictionaryStore.isEnabled(context)) }
    var showClearLearningDialog by remember { mutableStateOf(false) }

    fun applyTheme(next: YoryTheme) {
        theme = next
        backgroundHex = next.backgroundArgb.toHexArgb()
        keyHex = next.keyArgb.toHexArgb()
        textHex = next.textArgb.toHexArgb()
        accentHex = next.accentArgb.toHexArgb()
        pressedHex = next.keyPressedArgb.toHexArgb()
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

    if (showClearLearningDialog) {
        AlertDialog(
            onDismissRequest = { showClearLearningDialog = false },
            title = { Text("Clear personal dictionary?") },
            text = { Text("All locally learned words will be permanently removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    PersonalDictionaryStore.clear(context)
                    showClearLearningDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearLearningDialog = false }) { Text("Cancel") }
            }
        )
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0D16)) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF101725), Color(0xFF080B12), Color(0xFF151A28))))
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                        .clickable { onBack() }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("‹", color = Color.White, fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Text("Themes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YORY KEYS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("YORY Studio", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                        Text("Your words. Your flow.", color = Color(0xFF9CA7BD), fontSize = 14.sp)
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
                                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color(preset.keyArgb).copy(alpha = 0.30f))))
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
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color(colorInt))))
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
                HexColorInput("Pressed key", pressedHex, { pressedHex = it }) {
                    parseHexArgb(pressedHex)?.let { color -> applyTheme(theme.copy(keyPressedArgb = color, name = "Custom")) }
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
                SliderSetting("Key spacing", theme.keySpacingDp, 0f..12f, theme.keySpacingDp.toInt().toString() + " dp") {
                    applyTheme(theme.copy(keySpacingDp = it))
                }
                SliderSetting("Letter size", theme.fontSizeSp, 12f..26f, theme.fontSizeSp.toInt().toString() + " sp") {
                    applyTheme(theme.copy(fontSizeSp = it))
                }
                Text("LETTER FONT", color = Color(0xFF9CA7BD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("system", "rounded", "serif", "mono").forEach { family ->
                        FilterChip(
                            selected = theme.fontFamily == family,
                            onClick = { applyTheme(theme.copy(fontFamily = family)) },
                            label = { Text(family.replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }
                        )
                    }
                }
                Text("KEY SHAPE", color = Color(0xFF9CA7BD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("rounded", "squircle", "pill", "circle", "rect").forEach { shape ->
                        FilterChip(
                            selected = theme.keyShape == shape,
                            onClick = { applyTheme(theme.copy(keyShape = shape)) },
                            label = { Text(shape.replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }
                        )
                    }
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

                Text("MY THEMES", color = Color(0xFF9CA7BD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                OutlinedTextField(
                    value = savedThemeName,
                    onValueChange = { savedThemeName = it.take(48) },
                    label = { Text("Theme name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val cleanName = savedThemeName.trim().take(48)
                        if (cleanName.isBlank()) {
                            fileMessage = "Enter a name before saving this theme."
                        } else {
                            val saved = theme.copy(name = cleanName)
                            YoryThemeStore.save(context, saved)
                            YoryThemeLibraryStore.save(context, saved)
                            theme = saved
                            fileMessage = "${cleanName} was saved to My Themes."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save to My Themes") }
                Text("Saved themes stay on this device and are available offline.", color = Color(0xFF8994AA), fontSize = 11.sp)

                Text("PERSONAL DICTIONARY", color = Color(0xFF9CA7BD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                SwitchSetting("Learn words on this device", personalLearningEnabled) {
                    personalLearningEnabled = it
                    PersonalDictionaryStore.setEnabled(context, it)
                }
                Text(
                    "Optional and encrypted with Android Keystore. Words are learned only after you finish typing them in ordinary text fields. Learning is disabled in password, numeric, email and URL fields. Nothing is uploaded.",
                    color = Color(0xFF8994AA), fontSize = 11.sp
                )
                OutlinedButton(onClick = { showClearLearningDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear learned words", color = Color.White)
                }

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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB9D8FF).copy(alpha = 0.075f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
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
                                 .clip(themeShape(theme))
                                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color(theme.keyArgb).copy(alpha = 0.28f))))
                                .border(0.7.dp, Color.White.copy(alpha = 0.2f), themeShape(theme)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(char.toString(), color = Color(theme.textArgb), fontSize = theme.fontSizeSp.sp)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("!#1", "◎", "English", "↵").forEach { label ->
                    Box(
                        modifier = Modifier.weight(if (label == "English") 3f else 1f).height(32.dp)
                            .clip(themeShape(theme)).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color(theme.keyArgb).copy(alpha = 0.26f)))).border(0.7.dp, Color.White.copy(alpha = 0.28f), themeShape(theme)),
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
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.055f)).border(0.7.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)).padding(horizontal = 13.dp, vertical = 10.dp)) {
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
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.055f)).border(0.7.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.055f)).border(0.7.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)).padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun themeShape(theme: YoryTheme): RoundedCornerShape {
    val radius = when (theme.keyShape) {
        "pill", "circle" -> (theme.keyHeightDp / 2f).dp
        "squircle" -> (theme.cornerRadiusDp * 1.3f).dp
        "rect" -> 3.dp
        else -> theme.cornerRadiusDp.dp
    }
    return RoundedCornerShape(radius)
}

private fun Int.toHexArgb(): String = "#%08X".format(this)
