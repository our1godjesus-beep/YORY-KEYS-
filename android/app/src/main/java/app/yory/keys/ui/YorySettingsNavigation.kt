package app.yory.keys.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.content.Context.INPUT_METHOD_SERVICE
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import app.yory.keys.theme.YoryThemeStore
import app.yory.keys.ime.PersonalDictionaryStore
import app.yory.keys.ime.ClipboardHistoryStore

private data class SettingsItem(val icon: String, val title: String, val subtitle: String = "")

@Composable
fun YoryStudioScreen(onSignOut: () -> Unit = {}) {
    val context = LocalContext.current
    var page by remember { mutableStateOf<String?>(null) }
    var editingThemes by remember { mutableStateOf(false) }
    val items = listOf(
        SettingsItem("◉", "Account", "Optional account for cloud sync and online features"),
        SettingsItem("✧", "YORY Rewards", "Discover keyboard milestones"),
        SettingsItem("◎", "Languages", "English (US), العربية"),
        SettingsItem("◈", "Themes", "Glass themes and customization"),
        SettingsItem("T", "Typing", "Suggestions and personal dictionary"),
        SettingsItem("☻", "Emoji", "Emoji predictions and keyboard key"),
        SettingsItem("♬", "Rich input", "Clipboard and input tools"),
        SettingsItem("☷", "Keyboard toolbar", "Choose and arrange keyboard shortcuts"),
        SettingsItem("▦", "Layout & keys", "Size, rows, symbols and key behavior"),
        SettingsItem("◖", "Sound & vibration", "Key click and haptic feedback"),
        SettingsItem("⬡", "Privacy", "Your data and on-device controls"),
        SettingsItem("☆", "Achievements", "Your YORY milestones"),
        SettingsItem("?", "Help & Feedback", "Support and feedback"),
        SettingsItem("ⓘ", "About YORY KEYS", "Version and product information")
    )
    if (page == "Keyboard toolbar") {
        YoryToolbarCustomizationScreen(onBack = { page = null })
        return
    }
    if (page == "Themes") {
        if (editingThemes) {
            YoryThemesEditorScreen(onBack = { editingThemes = false })
        } else {
            YoryThemeGalleryScreen(
                onBack = { page = null },
                onCreate = { editingThemes = true },
                onCustomize = { editingThemes = true }
            )
        }
        return
    }

    GlassScaffold {
        if (page == null) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("YORY KEYS", color = GlassAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                        Text("Keyboard settings", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("✦", color = GlassAccent, fontSize = 30.sp)
                }
                Spacer(Modifier.height(5.dp))
                items.forEach { item ->
                    GlassSettingsRow(item) {
                        if (item.title == "Themes") editingThemes = false
                        page = item.title
                    }
                }
                Spacer(Modifier.height(84.dp))
            }
        } else {
            SettingsDetailPage(page = page!!, onBack = { page = null }, onSignOut = onSignOut)
        }
        FloatingActionButton(
            onClick = {
                val manager = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                manager.showInputMethodPicker()
            },
            modifier = Modifier.align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 14.dp),
            containerColor = GlassAccent,
            contentColor = Color(0xFF101725),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("⌨", fontSize = 23.sp)
        }
    }
}

@Composable
private fun SettingsDetailPage(page: String, onBack: () -> Unit, onSignOut: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("yory_user_settings", Context.MODE_PRIVATE) }
    var refresh by remember { mutableIntStateOf(0) }
    var activeLanguage by remember { mutableStateOf(prefs.getString("active_language", "en-US") ?: "en-US") }
    var addedLanguages by remember {
        mutableStateOf(
            (prefs.getStringSet("added_languages", setOf("en-US", "ar")) ?: setOf("en-US", "ar")).toSet()
        )
    }
    var personalLearning by remember { mutableStateOf(PersonalDictionaryStore.isEnabled(context)) }
    var showResizeEditor by remember { mutableStateOf(false) }
    var showClipboardHistory by remember { mutableStateOf(false) }
    var showClearDictionaryDialog by remember { mutableStateOf(false) }
    var showClearClipboardDialog by remember { mutableStateOf(false) }
    var privacyMessage by remember { mutableStateOf("") }
    var longPressDuration by remember { mutableFloatStateOf(prefs.getFloat("long_press_duration", 450f)) }
    var keypressVolume by remember { mutableFloatStateOf(prefs.getFloat("keypress_volume", 0.35f)) }
    var vibrationDuration by remember { mutableFloatStateOf(prefs.getFloat("vibration_duration", 12f)) }
    var numberPosition by remember { mutableStateOf(prefs.getString("number_position", "top") ?: "top") }
    var soundProfile by remember { mutableStateOf(prefs.getString("sound_profile", "Modern") ?: "Modern") }

    fun readBool(key: String, default: Boolean = false): Boolean {
        refresh
        return prefs.getBoolean(key, default)
    }
    fun writeBool(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        refresh++
    }
    fun chooseLanguage(tag: String) {
        activeLanguage = tag
        addedLanguages = addedLanguages + tag
        prefs.edit()
            .putString("active_language", tag)
            .putStringSet("added_languages", addedLanguages)
            .apply()
        refresh++
    }

    fun setLanguageAdded(tag: String, enabled: Boolean) {
        val updated = if (enabled) {
            addedLanguages + tag
        } else if (addedLanguages.size > 1) {
            addedLanguages - tag
        } else {
            addedLanguages
        }
        addedLanguages = updated
        prefs.edit().putStringSet("added_languages", updated).apply()
        if (!enabled && activeLanguage == tag && tag !in updated) {
            activeLanguage = updated.first()
            prefs.edit().putString("active_language", activeLanguage).apply()
        }
        refresh++
    }

    if (page == "Layout & keys" && showResizeEditor) {
        YoryKeyboardResizeScreen(onBack = { showResizeEditor = false })
        return
    }
    if (page == "Rich input" && showClipboardHistory) {
        YoryClipboardHistoryScreen(onBack = { showClipboardHistory = false })
        return
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(17.dp)).background(GlassSurface)
                .border(0.8.dp, GlassBorder, RoundedCornerShape(17.dp))
                .clickable { onBack() }.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹", color = Color.White, fontSize = 30.sp)
            Spacer(Modifier.width(12.dp))
            Text(page, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Medium)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (page) {
                "Languages" -> {
                    Text("YOUR LANGUAGES", color = GlassMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                    LanguageChoice("English (US)", "QWERTY · starter word list", "en-US", activeLanguage == "en-US", "en-US" in addedLanguages, { chooseLanguage("en-US") }) { enabled -> setLanguageAdded("en-US", enabled) }
                    LanguageChoice("العربية (مصر)", "Arabic 123 (PC) · starter word list", "ar", activeLanguage == "ar", "ar" in addedLanguages, { chooseLanguage("ar") }) { enabled -> setLanguageAdded("ar", enabled) }
                    LanguageChoice("Deutsch (DE)", "QWERTZ · layout only", "de", activeLanguage == "de", "de" in addedLanguages, { chooseLanguage("de") }) { enabled -> setLanguageAdded("de", enabled) }
                    LanguageChoice("Español", "QWERTY · layout only", "es", activeLanguage == "es", "es" in addedLanguages, { chooseLanguage("es") }) { enabled -> setLanguageAdded("es", enabled) }
                    LanguageChoice("Français", "AZERTY · layout only", "fr", activeLanguage == "fr", "fr" in addedLanguages, { chooseLanguage("fr") }) { enabled -> setLanguageAdded("fr", enabled) }
                    LanguageChoice("Português", "QWERTY · layout only", "pt", activeLanguage == "pt", "pt" in addedLanguages, { chooseLanguage("pt") }) { enabled -> setLanguageAdded("pt", enabled) }
                    LanguageChoice("Italiano", "QWERTY · layout only", "it", activeLanguage == "it", "it" in addedLanguages, { chooseLanguage("it") }) { enabled -> setLanguageAdded("it", enabled) }
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Text("Layouts and dictionaries are separate", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Seven keyboard layouts are available offline. Bundled starter word lists are currently available only for English and Arabic; the other languages can use personal learning, but do not have shipped prediction dictionaries yet. The full 120-language catalogue is a target list, not 120 ready-to-use languages.", color = GlassMuted, fontSize = 13.sp)
                    }
                }
                "Emoji" -> {
                    ToggleCard("Emoji predictions", "Show emoji suggestions in the suggestion bar", readBool("emoji_predictions", true)) { writeBool("emoji_predictions", it) }
                    ToggleCard("Dedicated emoji key", "Show an emoji shortcut on the keyboard", readBool("emoji_key", true)) { writeBool("emoji_key", it) }
                    ToggleCard("Recent emoji first", "Keep recently used emoji at the beginning", readBool("emoji_recent", true)) { writeBool("emoji_recent", it) }
                }
                "Layout & keys" -> {
                    ToggleCard("Number row", "Always show a number row above letters", readBool("number_row", false)) { writeBool("number_row", it) }
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Text("Number position", color = Color.White, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("top", "bottom").forEach { position ->
                                FilterChip(
                                    selected = numberPosition == position,
                                    onClick = {
                                        numberPosition = position
                                        prefs.edit().putString("number_position", position).apply()
                                    },
                                    label = { Text(position.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                    ToggleCard("Long-press symbols", "Show secondary symbols on letter keys", readBool("long_press_symbols", true)) { writeBool("long_press_symbols", it) }
                    ToggleCard("Accented characters", "Long-press to see available accents", readBool("accented_chars", true)) { writeBool("accented_chars", it) }
                    ToggleCard("Arrow keys", "Show cursor movement keys on a dedicated row", readBool("arrow_keys", false)) { writeBool("arrow_keys", it) }
                    ToggleCard("Key pop-ups", "Show a visual pop-up when tapping a key", readBool("key_popups", true)) { writeBool("key_popups", it) }
                    ToggleCard("Large key text", "Use larger labels on keyboard keys", readBool("large_key_text", true)) { writeBool("large_key_text", it) }
                    PreferenceSlider("Long-press duration", longPressDuration, 200f..900f, "${longPressDuration.toInt()} ms") {
                        longPressDuration = it
                        prefs.edit().putFloat("long_press_duration", it).apply()
                    }
                    GlassPanel(Modifier.fillMaxWidth().clickable { showResizeEditor = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Resize keyboard", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Preview key height, choose a size or fine-tune it.", color = GlassMuted, fontSize = 12.sp)
                            }
                            Text("›", color = GlassAccent, fontSize = 28.sp)
                        }
                    }
                    InfoCard("More appearance controls", "Change key spacing, corner radius and lettering in Themes.")
                }
                "Sound & vibration" -> {
                    val theme = remember { mutableStateOf(YoryThemeStore.load(context)) }
                    ToggleCard("Keypress sound", "Play a soft sound when a key is pressed", theme.value.soundEnabled) {
                        theme.value = theme.value.copy(soundEnabled = it); YoryThemeStore.save(context, theme.value)
                    }
                    PreferenceSlider("Keypress sound volume", keypressVolume, 0f..1f, "${(keypressVolume * 100).toInt()}%") {
                        keypressVolume = it
                        prefs.edit().putFloat("keypress_volume", it).apply()
                    }
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Text("Keypress sound profile", color = Color.White, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Classic", "Modern", "Soft").forEach { profile ->
                                FilterChip(
                                    selected = soundProfile == profile,
                                    onClick = {
                                        soundProfile = profile
                                        prefs.edit().putString("sound_profile", profile).apply()
                                    },
                                    label = { Text(profile) }
                                )
                            }
                        }
                    }
                    ToggleCard("Keypress vibration", "Use haptic feedback for key presses", theme.value.hapticsEnabled) {
                        theme.value = theme.value.copy(hapticsEnabled = it); YoryThemeStore.save(context, theme.value)
                    }
                    ToggleCard("Use Android default vibration", "Follow the device haptic feedback settings", readBool("system_haptics", true)) { writeBool("system_haptics", it) }
                    PreferenceSlider("Keypress vibration duration", vibrationDuration, 0f..80f, "${vibrationDuration.toInt()} ms") {
                        vibrationDuration = it
                        prefs.edit().putFloat("vibration_duration", it).apply()
                    }
                }
                "Rich input" -> {
                    val theme = remember { mutableStateOf(YoryThemeStore.load(context)) }
                    ToggleCard("Clipboard history", "Keep recent copied text on this device", theme.value.clipboardEnabled) {
                        theme.value = theme.value.copy(clipboardEnabled = it); YoryThemeStore.save(context, theme.value)
                    }
                    GlassPanel(Modifier.fillMaxWidth().clickable { showClipboardHistory = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Manage clipboard", color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("Pinned items, recent clips, delete and undo", color = GlassMuted, fontSize = 12.sp)
                            }
                            Text("›", color = GlassAccent, fontSize = 26.sp)
                        }
                    }
                    ToggleCard("Compact mode", "Show fewer suggestions in a shorter bar", readBool("compact_mode", false)) {
                        writeBool("compact_mode", it)
                    }
                    InfoCard("Voice typing", "Voice input is not connected in this build yet.")
                    InfoCard("Search and online suggestions", "No external search service is enabled. YORY keyboard typing remains local.")
                }
                "Typing" -> {
                    ToggleCard("Word suggestions", "Show local word suggestions while typing", readBool("word_suggestions", true)) { writeBool("word_suggestions", it) }
                    ToggleCard("Auto-capitalization", "Capitalize the first letter of sentences", readBool("auto_caps", true)) { writeBool("auto_caps", it) }
                    ToggleCard("Personal dictionary", "Learn accepted words on this device", personalLearning) {
                        personalLearning = it
                        PersonalDictionaryStore.setEnabled(context, it)
                    }
                    InfoCard("Personal learning", "Learned words are stored locally. Password, numeric, email and URL fields are excluded.")
                }
                "Privacy" -> {
                    InfoCard("Private by design", "YORY does not send keystrokes, passwords or typed text to a server.")
                    InfoCard("On-device storage", "Personal dictionary and optional clipboard history remain on this device. Clipboard data is encrypted at rest.")
                    InfoCard("Sensitive fields", "Learning and clipboard capture are disabled for sensitive fields and Incognito sessions.")
                    InfoCard("Permissions", "The keyboard does not request camera, contacts or location access.")
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Text("Manage local data", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("These actions remove saved data from this device. They do not change your typing settings.", color = GlassMuted, fontSize = 12.sp)
                        OutlinedButton(
                            onClick = { showClearDictionaryDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Clear personal dictionary", color = Color(0xFFFFA0A0)) }
                        OutlinedButton(
                            onClick = { showClearClipboardDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Clear clipboard history", color = Color(0xFFFFA0A0)) }
                        if (privacyMessage.isNotBlank()) {
                            Text(privacyMessage, color = GlassAccent, fontSize = 12.sp)
                        }
                    }
                }
                "Account" -> {
                    InfoCard("YORY account", "An account is optional. Create or sign in for cloud features; your keyboard, themes and local settings continue to work offline.")
                    Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Connect account / sign out") }
                }
                "YORY Rewards", "Achievements" -> InfoCard("Coming to YORY", "This section is part of the settings structure. Rewards and achievement tracking are not connected in this preview.")
                "Help & Feedback" -> {
                    InfoCard("Help", "Enable YORY KEYS in Android keyboard settings, then choose it as the active keyboard.")
                    InfoCard("Feedback", "Feedback submission is not connected in this preview.")
                }
                "About YORY KEYS" -> {
                    InfoCard("YORY KEYS", "A privacy-first multilingual keyboard with customizable themes.")
                    InfoCard("Preview build", "Version 0.1.0 · Android preview")
                }
            }
        }
    }

    if (showClearDictionaryDialog) {
        AlertDialog(
            onDismissRequest = { showClearDictionaryDialog = false },
            title = { Text("Clear personal dictionary?") },
            text = { Text("All locally learned words will be deleted. Personal learning can remain enabled afterward.") },
            confirmButton = {
                TextButton(onClick = {
                    PersonalDictionaryStore.clear(context)
                    showClearDictionaryDialog = false
                    privacyMessage = "Personal dictionary cleared from this device."
                    refresh++
                }) { Text("Clear", color = Color(0xFFFFA0A0)) }
            },
            dismissButton = { TextButton(onClick = { showClearDictionaryDialog = false }) { Text("Cancel") } }
        )
    }
    if (showClearClipboardDialog) {
        AlertDialog(
            onDismissRequest = { showClearClipboardDialog = false },
            title = { Text("Clear clipboard history?") },
            text = { Text("All saved clipboard clips will be deleted from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    ClipboardHistoryStore.clear(context)
                    showClearClipboardDialog = false
                    privacyMessage = "Clipboard history cleared from this device."
                    refresh++
                }) { Text("Clear", color = Color(0xFFFFA0A0)) }
            },
            dismissButton = { TextButton(onClick = { showClearClipboardDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun LanguageChoice(
    title: String,
    subtitle: String,
    tag: String,
    selected: Boolean,
    added: Boolean,
    onSelect: () -> Unit,
    onAddedChange: (Boolean) -> Unit
) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                Modifier.weight(1f).clickable(onClick = onSelect).padding(vertical = 3.dp)
            ) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = GlassMuted, fontSize = 12.sp)
                if (selected) {
                    Text("Active layout", color = GlassAccent, fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (added) "Added" else "Add", color = if (added) GlassAccent else GlassMuted, fontSize = 10.sp)
                Switch(
                    checked = added,
                    onCheckedChange = onAddedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF0A0D16),
                        checkedTrackColor = GlassAccent,
                        uncheckedThumbColor = Color(0xFFB5BBC8),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}

@Composable
private fun PreferenceSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit
) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(valueLabel, color = GlassMuted, fontSize = 12.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ToggleCard(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = GlassMuted, fontSize = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0A0D16), checkedTrackColor = GlassAccent,
                uncheckedThumbColor = Color(0xFFB5BBC8), uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
            ))
        }
    }
}

@Composable
private fun InfoCard(title: String, description: String) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(description, color = GlassMuted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun GlassSettingsRow(item: SettingsItem, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                    .border(0.7.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(item.icon, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (item.subtitle.isNotEmpty()) Text(item.subtitle, color = GlassMuted, fontSize = 12.sp)
            }
            Text("›", color = GlassMuted, fontSize = 25.sp)
        }
    }
}

@Composable
private fun GlassAction(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), GlassAccent.copy(alpha = 0.28f))))
            .border(0.8.dp, GlassBorder, RoundedCornerShape(15.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontSize = 22.sp) }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(19.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.105f), Color.White.copy(alpha = 0.035f))))
            .border(0.8.dp, GlassBorder, RoundedCornerShape(19.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp), content = content
    )
}

@Composable
private fun GlassScaffold(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF252C3D), Color(0xFF0B0E16), Color(0xFF141A29)))
    ), content = content)
}

private val GlassAccent = Color(0xFFA7C7FF)
private val GlassMuted = Color(0xFFB0B6C4)
private val GlassSurface = Color.White.copy(alpha = 0.065f)
private val GlassBorder = Color.White.copy(alpha = 0.19f)
