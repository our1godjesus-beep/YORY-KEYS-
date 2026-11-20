package app.yory.keys.theme

import android.content.Context

/** Host app and IME share this app-private preference store on Android. */
object YoryThemeStore {
    private const val FILE = "yory_local_theme"

    fun load(context: Context): YoryTheme {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val fallback = YoryTheme.default
        return YoryTheme(
            name = prefs.getString("name", fallback.name) ?: fallback.name,
            backgroundArgb = prefs.getInt("background", fallback.backgroundArgb),
            keyArgb = prefs.getInt("key", fallback.keyArgb),
            textArgb = prefs.getInt("text", fallback.textArgb),
            accentArgb = prefs.getInt("accent", fallback.accentArgb),
            opacity = prefs.getFloat("opacity", fallback.opacity).coerceIn(0.35f, 1f),
            cornerRadiusDp = prefs.getFloat("radius", fallback.cornerRadiusDp).coerceIn(4f, 28f),
            keyHeightDp = prefs.getFloat("height", fallback.keyHeightDp).coerceIn(36f, 60f),
            soundEnabled = prefs.getBoolean("sound_enabled", fallback.soundEnabled),
            hapticsEnabled = prefs.getBoolean("haptics_enabled", fallback.hapticsEnabled),
            clipboardEnabled = prefs.getBoolean("clipboard_enabled", fallback.clipboardEnabled),
            keyPressedArgb = prefs.getInt("key_pressed", fallback.keyPressedArgb),
            keySpacingDp = prefs.getFloat("key_spacing", fallback.keySpacingDp).coerceIn(0f, 12f),
            keyShape = prefs.getString("key_shape", fallback.keyShape)?.takeIf {
                it in setOf("rounded", "squircle", "pill", "circle", "rect")
            } ?: fallback.keyShape,
            fontSizeSp = prefs.getFloat("font_size", fallback.fontSizeSp).coerceIn(12f, 26f),
            fontFamily = prefs.getString("font_family", fallback.fontFamily)?.takeIf {
                it in setOf("system", "rounded", "serif", "mono")
            } ?: fallback.fontFamily
        )
    }

    fun save(context: Context, theme: YoryTheme) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("name", theme.name.take(48))
            .putInt("background", theme.backgroundArgb)
            .putInt("key", theme.keyArgb)
            .putInt("text", theme.textArgb)
            .putInt("accent", theme.accentArgb)
            .putFloat("opacity", theme.opacity.coerceIn(0.35f, 1f))
            .putFloat("radius", theme.cornerRadiusDp.coerceIn(4f, 28f))
            .putFloat("height", theme.keyHeightDp.coerceIn(36f, 60f))
            .putBoolean("sound_enabled", theme.soundEnabled)
            .putBoolean("haptics_enabled", theme.hapticsEnabled)
            .putBoolean("clipboard_enabled", theme.clipboardEnabled)
            .putInt("key_pressed", theme.keyPressedArgb)
            .putFloat("key_spacing", theme.keySpacingDp.coerceIn(0f, 12f))
            .putString("key_shape", theme.keyShape.takeIf {
                it in setOf("rounded", "squircle", "pill", "circle", "rect")
            } ?: "rounded")
            .putFloat("font_size", theme.fontSizeSp.coerceIn(12f, 26f))
            .putString("font_family", theme.fontFamily.takeIf {
                it in setOf("system", "rounded", "serif", "mono")
            } ?: "system")
            .apply()
    }
}
