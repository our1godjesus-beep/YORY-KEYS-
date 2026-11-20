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
            hapticsEnabled = prefs.getBoolean("haptics_enabled", fallback.hapticsEnabled)
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
            .apply()
    }
}
