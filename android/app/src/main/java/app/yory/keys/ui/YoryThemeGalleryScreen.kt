package app.yory.keys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import app.yory.keys.theme.YoryTheme
import app.yory.keys.theme.YoryThemeStore
import app.yory.keys.theme.YoryThemeLibraryStore

private val GalleryBackground = Color(0xFF090A0C)
private val GalleryMuted = Color(0xFFC2BBAE)
private val GalleryAccent = Color(0xFFC9AE80)
private val GalleryBorder = Color.White.copy(alpha = 0.11f)

@Composable
internal fun YoryThemeGalleryScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onCustomize: () -> Unit
) {
    val context = LocalContext.current
    var activeTheme by remember { mutableStateOf(YoryThemeStore.load(context)) }
    var category by remember { mutableStateOf("All") }
    var feedback by remember { mutableStateOf("") }
    var libraryRevision by remember { mutableIntStateOf(0) }
    var detailTheme by remember { mutableStateOf<YoryTheme?>(null) }
    val savedThemes = remember(libraryRevision) { YoryThemeLibraryStore.list(context) }
    val favorites = remember(libraryRevision) { YoryThemeLibraryStore.favorites(context) }

    if (detailTheme != null) {
        val current = detailTheme!!
        val isSaved = savedThemes.any { it.name.equals(current.name, ignoreCase = true) }
        YoryThemeDetailScreen(
            theme = current,
            isApplied = activeTheme.name == current.name,
            isSaved = isSaved,
            isFavorite = favorites.contains(current.name.lowercase()),
            onBack = { detailTheme = null },
            onApply = {
                YoryThemeStore.save(context, current)
                activeTheme = current
                feedback = "${current.name} is now active."
            },
            onCustomize = {
                YoryThemeStore.save(context, current)
                activeTheme = current
                onCustomize()
            },
            onFavorite = { enabled ->
                YoryThemeLibraryStore.setFavorite(context, current.name, enabled)
                libraryRevision++
            },
            onRename = { newName ->
                val renamed = YoryThemeLibraryStore.rename(context, current.name, newName)
                if (renamed) {
                    if (activeTheme.name.equals(current.name, ignoreCase = true)) {
                        activeTheme = activeTheme.copy(name = newName.trim().take(48))
                        YoryThemeStore.save(context, activeTheme)
                    }
                    detailTheme = current.copy(name = newName.trim().take(48))
                    libraryRevision++
                }
                renamed
            },
            onDuplicate = { newName ->
                val duplicated = YoryThemeLibraryStore.duplicate(context, current, newName)
                if (duplicated) libraryRevision++
                duplicated
            },
            onDelete = {
                YoryThemeLibraryStore.delete(context, current.name)
                if (activeTheme.name.equals(current.name, ignoreCase = true)) {
                    val fallback = YoryTheme.default
                    YoryThemeStore.save(context, fallback)
                    activeTheme = fallback
                }
                libraryRevision++
                detailTheme = null
            }
        )
        return
    }
    val builtInThemes = YoryTheme.presets.filterNot { preset ->
        savedThemes.any { it.name.equals(preset.name, ignoreCase = true) }
    }
    val themes = savedThemes + builtInThemes + listOf(activeTheme).filter { current ->
        savedThemes.none { it.name == current.name } && builtInThemes.none { it.name == current.name }
    }
    val visibleThemes = when (category) {
        "Yours" -> savedThemes
        "Favorites" -> themes.filter { favorites.contains(it.name.lowercase()) }
        "Dark" -> themes.filter { it.name in setOf("Obsidian Luxe", "Tinted Glass", "Midnight", "Neon", "Custom") }
        "Light" -> themes.filter { it.name in setOf("Arctic", "Rose") }
        "Colorful" -> themes.filter { it.name in setOf("Neon", "Rose", "Arctic") }
        else -> themes
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF182236), GalleryBackground, Color(0xFF121526)))
    )) {
        val compact = maxWidth < 360.dp
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 14.dp else 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)) {
                    Text("‹  Back", color = GalleryAccent, fontSize = 16.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCreate) {
                    Text("Create", color = GalleryAccent, fontWeight = FontWeight.SemiBold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("YORY KEYS", color = GalleryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Themes", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                Text("Choose a look for your keyboard. Your selection stays on this device.", color = GalleryMuted, fontSize = 13.sp)
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Yours", "Favorites", "Dark", "Light", "Colorful").forEach { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text(item) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GalleryAccent.copy(alpha = 0.22f),
                            selectedLabelColor = Color.White,
                            labelColor = GalleryMuted
                        )
                    )
                }
            }

            if (category == "All") {
                Text("AVAILABLE THEMES", color = GalleryMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            } else if (category == "Yours") {
                Text("YOUR SAVED THEMES", color = GalleryMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            if (visibleThemes.isEmpty()) {
                GalleryPanel {
                    Text(if (category == "Yours") "No saved themes yet" else "No themes in this category", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Customize a preset, give it a name, then save it here for offline use.", color = GalleryMuted, fontSize = 12.sp)
                    OutlinedButton(onClick = onCreate) { Text("Create your theme", color = GalleryAccent) }
                }
            } else if (compact) {
                visibleThemes.forEach { theme ->
                    GalleryThemeCard(
                        theme = theme,
                        selected = activeTheme.name == theme.name,
                        saved = savedThemes.any { it.name == theme.name },
                        favorite = favorites.contains(theme.name.lowercase()),
                        onDetails = { detailTheme = theme },
                        onApply = {
                            YoryThemeStore.save(context, theme)
                            activeTheme = theme
                            feedback = "${theme.name} is now active."
                        },
                        onCustomize = {
                            YoryThemeStore.save(context, theme)
                            activeTheme = theme
                            onCustomize()
                        }
                    )
                }
            } else {
                visibleThemes.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { theme ->
                            Box(Modifier.weight(1f)) {
                                GalleryThemeCard(
                                    theme = theme,
                                    selected = activeTheme.name == theme.name,
                                    saved = savedThemes.any { it.name == theme.name },
                                    favorite = favorites.contains(theme.name.lowercase()),
                                    onDetails = { detailTheme = theme },
                                    onApply = {
                                        YoryThemeStore.save(context, theme)
                                        activeTheme = theme
                                        feedback = "${theme.name} is now active."
                                    },
                                    onCustomize = {
                                        YoryThemeStore.save(context, theme)
                                        activeTheme = theme
                                        onCustomize()
                                    }
                                )
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (feedback.isNotEmpty()) {
                Text(feedback, color = GalleryAccent, fontSize = 12.sp)
            }
            GalleryPanel {
                Text("More ways to personalize", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(
                    "Open the editor to adjust colors, glass opacity, key size, spacing, shape and lettering. Customization works offline.",
                    color = GalleryMuted, fontSize = 12.sp, lineHeight = 18.sp
                )
                OutlinedButton(onClick = onCustomize, modifier = Modifier.fillMaxWidth()) {
                    Text("Customize current theme", color = GalleryAccent)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GalleryThemeCard(
    theme: YoryTheme,
    selected: Boolean,
    saved: Boolean,
    favorite: Boolean,
    onDetails: () -> Unit,
    onApply: () -> Unit,
    onCustomize: () -> Unit
) {
    val surface = Color(theme.backgroundArgb)
    val key = Color(theme.keyArgb)
    val label = Color(theme.textArgb)
    val shape = RoundedCornerShape(theme.cornerRadiusDp.coerceIn(7f, 20f).dp)

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(if (selected) 1.6.dp else 0.8.dp, if (selected) GalleryAccent else GalleryBorder, RoundedCornerShape(19.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(surface.copy(alpha = 0.98f), surface.copy(alpha = 0.80f))))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("☺", "✦", "文", "⚙", "•••").forEach {
                    Text(it, color = Color(theme.accentArgb), fontSize = 11.sp)
                }
            }
            listOf("qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫").forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { character ->
                        Box(
                            Modifier.weight(1f).height(23.dp).clip(shape)
                                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.13f), key.copy(alpha = theme.opacity.coerceIn(0.35f, 1f))))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(character.toString(), color = label, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("!#1", "◎", "English", "↵").forEach { labelText ->
                    Box(
                        Modifier.weight(if (labelText == "English") 2.7f else 1f).height(22.dp)
                            .clip(shape).background(key.copy(alpha = 0.70f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(labelText, color = label, fontSize = 7.sp)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(theme.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    when {
                        selected -> "Currently applied"
                        saved -> "Saved by you"
                        else -> "Built-in preset"
                    },
                    color = if (selected) GalleryAccent else GalleryMuted,
                    fontSize = 10.sp
                )
            }
            if (selected) Text("✓", color = GalleryAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected) GalleryAccent.copy(alpha = 0.18f) else GalleryAccent,
                contentColor = if (selected) Color.White else Color(0xFF101725)
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(if (selected) "Applied" else "Apply theme", fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDetails, modifier = Modifier.weight(1f)) {
                Text("Details", color = GalleryAccent, fontSize = 12.sp)
            }
            TextButton(onClick = onCustomize, modifier = Modifier.weight(1f)) {
                Text("Customize", color = GalleryAccent, fontSize = 12.sp)
            }
            if (favorite) Text("★", color = GalleryAccent, fontSize = 14.sp)
        }
    }
}

@Composable
private fun GalleryPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(0.8.dp, GalleryBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = content
    )
}

@Composable
private fun YoryThemeDetailScreen(
    theme: YoryTheme,
    isApplied: Boolean,
    isSaved: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onCustomize: () -> Unit,
    onFavorite: (Boolean) -> Unit,
    onRename: (String) -> Boolean,
    onDuplicate: (String) -> Boolean,
    onDelete: () -> Unit
) {
    var renameDialog by remember { mutableStateOf(false) }
    var duplicateDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var nameInput by remember(theme.name) { mutableStateOf(theme.name) }
    var message by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFF182236), GalleryBackground, Color(0xFF121526)))
    )) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹  Back", color = GalleryAccent) }
                Spacer(Modifier.weight(1f))
                Text(if (isSaved) "YOUR THEME" else "BUILT-IN", color = GalleryMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            Text(theme.name, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            GalleryLargePreview(theme)
            Text(if (isApplied) "Currently applied" else "Ready to apply", color = if (isApplied) GalleryAccent else GalleryMuted, fontSize = 13.sp)
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (isApplied) "Applied" else "Apply theme")
            }
            OutlinedButton(onClick = onCustomize, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Customize this theme")
            }
            GalleryPanel {
                Text("Theme controls", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Key height: ${theme.keyHeightDp.toInt()} dp", color = GalleryMuted, fontSize = 12.sp)
                Text("Key spacing: ${theme.keySpacingDp.toInt()} dp", color = GalleryMuted, fontSize = 12.sp)
                Text("Corner radius: ${theme.cornerRadiusDp.toInt()} dp", color = GalleryMuted, fontSize = 12.sp)
                Text("Letter size: ${theme.fontSizeSp.toInt()} sp", color = GalleryMuted, fontSize = 12.sp)
            }
            GalleryPanel {
                Text("Manage theme", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { onFavorite(!isFavorite) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isFavorite) "★ Remove from Favorites" else "☆ Add to Favorites", color = GalleryAccent)
                }
                OutlinedButton(onClick = { nameInput = theme.name; renameDialog = true }, enabled = isSaved, modifier = Modifier.fillMaxWidth()) {
                    Text("Rename")
                }
                OutlinedButton(onClick = { nameInput = "Copy of ${theme.name}".take(48); duplicateDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Duplicate")
                }
                OutlinedButton(onClick = { confirmDelete = true }, enabled = isSaved, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete saved theme", color = Color(0xFFFF9A9A))
                }
                if (!isSaved) Text("Built-in presets cannot be deleted or renamed. Duplicate one to make your own copy.", color = GalleryMuted, fontSize = 12.sp)
            }
            if (message.isNotBlank()) Text(message, color = GalleryAccent, fontSize = 12.sp)
        }
    }

    if (renameDialog) {
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text("Rename theme") },
            text = {
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it.take(48) }, label = { Text("Theme name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val success = onRename(nameInput)
                    message = if (success) "Theme renamed." else "That name is empty or already used."
                    renameDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("Cancel") } }
        )
    }
    if (duplicateDialog) {
        AlertDialog(
            onDismissRequest = { duplicateDialog = false },
            title = { Text("Duplicate theme") },
            text = {
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it.take(48) }, label = { Text("New theme name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    val success = onDuplicate(nameInput)
                    message = if (success) "Theme duplicated into My Themes." else "That name is empty or already used."
                    duplicateDialog = false
                }) { Text("Duplicate") }
            },
            dismissButton = { TextButton(onClick = { duplicateDialog = false }) { Text("Cancel") } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete theme?") },
            text = { Text("This removes the saved theme from this device. It will not affect built-in presets.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun GalleryLargePreview(theme: YoryTheme) {
    val keyColor = Color(theme.keyArgb)
    val textColor = Color(theme.textArgb)
    val keyShape = RoundedCornerShape(theme.cornerRadiusDp.coerceIn(6f, 18f).dp)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Color(theme.backgroundArgb).copy(alpha = theme.opacity.coerceIn(0.35f, 1f)))
            .border(0.8.dp, GalleryBorder, RoundedCornerShape(22.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(theme.keySpacingDp.coerceIn(1f, 7f).dp)
    ) {
        listOf("qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫").forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { character ->
                    Box(
                        Modifier.weight(1f).height(theme.keyHeightDp.coerceIn(34f, 54f).dp)
                            .clip(keyShape).background(keyColor.copy(alpha = 0.94f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(character.toString(), color = textColor, fontSize = theme.fontSizeSp.coerceIn(12f, 24f).sp)
                    }
                }
            }
        }
    }
}
