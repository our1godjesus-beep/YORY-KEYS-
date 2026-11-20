package app.yory.keys.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    fun applyTheme(next: YoryTheme) {
        theme = next
        YoryThemeStore.save(context, next)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0D1019)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
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

                SliderSetting("Glass opacity", theme.opacity, 0.35f..1f, (theme.opacity * 100).toInt().toString() + "%") {
                    applyTheme(theme.copy(opacity = it))
                }
                SliderSetting("Key corner radius", theme.cornerRadiusDp, 4f..28f, theme.cornerRadiusDp.toInt().toString() + " dp") {
                    applyTheme(theme.copy(cornerRadiusDp = it))
                }
                SliderSetting("Key height", theme.keyHeightDp, 36f..60f, theme.keyHeightDp.toInt().toString() + " dp") {
                    applyTheme(theme.copy(keyHeightDp = it))
                }

                Spacer(Modifier.weight(1f))
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
