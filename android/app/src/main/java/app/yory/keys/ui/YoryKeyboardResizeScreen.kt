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
import app.yory.keys.theme.YoryTheme
import app.yory.keys.theme.YoryThemeStore

private val ResizeBackground = Color(0xFF0A0D16)
private val ResizeAccent = Color(0xFFA7C7FF)
private val ResizeMuted = Color(0xFFB0B6C4)
private val ResizeBorder = Color.White.copy(alpha = 0.16f)

@Composable
internal fun YoryKeyboardResizeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var theme by remember { mutableStateOf(YoryThemeStore.load(context)) }
    var previewHeight by remember { mutableFloatStateOf(theme.keyHeightDp) }
    var status by remember { mutableStateOf("Changes are saved on this device.") }

    fun saveHeight(value: Float) {
        val safe = value.coerceIn(36f, 60f)
        val updated = YoryThemeStore.load(context).copy(keyHeightDp = safe)
        YoryThemeStore.save(context, updated)
        theme = updated
        previewHeight = safe
        status = "Keyboard height saved: ${safe.toInt()} dp."
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(Color(0xFF182236), ResizeBackground, Color(0xFF121526))
            )
        )
    ) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 2.dp)) {
                    Text("‹  Back", color = ResizeAccent, fontSize = 16.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { saveHeight(46f) }) {
                    Text("Reset", color = ResizeAccent, fontWeight = FontWeight.SemiBold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("YORY KEYS", color = ResizeAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Resize keyboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Adjust the key height and preview the result before returning to typing.", color = ResizeMuted, fontSize = 13.sp)
            }

            ResizePreviewCard(theme = theme, keyHeight = previewHeight)

            ResizeSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Keyboard height", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("36–60 dp", color = ResizeMuted, fontSize = 12.sp)
                    }
                    Text("${previewHeight.toInt()} dp", color = ResizeAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Slider(
                    value = previewHeight,
                    onValueChange = { previewHeight = it.coerceIn(36f, 60f) },
                    onValueChangeFinished = { saveHeight(previewHeight) },
                    valueRange = 36f..60f,
                    steps = 23,
                    colors = SliderDefaults.colors(
                        thumbColor = ResizeAccent,
                        activeTrackColor = ResizeAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Compact", color = ResizeMuted, fontSize = 11.sp)
                    Text("Tall", color = ResizeMuted, fontSize = 11.sp)
                }
            }

            ResizeSurface {
                Text("QUICK SIZES", color = ResizeMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Compact" to 40f, "Standard" to 46f, "Tall" to 54f).forEach { (label, value) ->
                        val selected = previewHeight.toInt() == value.toInt()
                        FilterChip(
                            selected = selected,
                            onClick = { saveHeight(value) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ResizeAccent.copy(alpha = 0.22f),
                                selectedLabelColor = Color.White,
                                labelColor = ResizeMuted
                            )
                        )
                    }
                }
                Text("The active keyboard reads this saved value the next time it is shown.", color = ResizeMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }

            Text(status, color = ResizeAccent, fontSize = 12.sp)
            ResizeSurface {
                Text("More personalization", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Key spacing, corner radius, letter size and key shape are available in Themes. Drag-to-reposition and floating keyboard modes are not part of this control yet.", color = ResizeMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ResizePreviewCard(theme: YoryTheme, keyHeight: Float) {
    val keyColor = Color(theme.keyArgb)
    val textColor = Color(theme.textArgb)
    val background = Color(theme.backgroundArgb)
    val keyRadius = RoundedCornerShape(theme.cornerRadiusDp.coerceIn(6f, 18f).dp)
    val rows = listOf("qwertyuiop", "asdfghjkl", "⇧zxcvbnm⌫")

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(0.8.dp, ResizeBorder, RoundedCornerShape(22.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LIVE PREVIEW", color = ResizeMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Spacer(Modifier.weight(1f))
            Text("EN · QWERTY", color = ResizeAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(background.copy(alpha = theme.opacity.coerceIn(0.35f, 1f)))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(theme.keySpacingDp.dp.coerceIn(1.dp, 7.dp))
        ) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { letter ->
                        Box(
                            Modifier.weight(1f).height((keyHeight * 0.62f).dp.coerceIn(24.dp, 40.dp))
                                .clip(keyRadius)
                                .background(keyColor.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(letter.toString(), color = textColor, fontSize = theme.fontSizeSp.coerceIn(12f, 22f).sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf("!#1", "☺", "SPACE", "↵").forEach { key ->
                    Box(
                        Modifier.weight(if (key == "SPACE") 2.8f else 1f)
                            .height((keyHeight * 0.62f).dp.coerceIn(24.dp, 40.dp))
                            .clip(keyRadius)
                            .background(keyColor.copy(alpha = 0.82f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(key, color = textColor, fontSize = if (key == "SPACE") 8.sp else 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResizeSurface(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .border(0.8.dp, ResizeBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}
