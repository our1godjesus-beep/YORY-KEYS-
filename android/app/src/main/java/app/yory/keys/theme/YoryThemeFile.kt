package app.yory.keys.theme

import android.graphics.Color
import org.json.JSONObject

object YoryThemeFile {
    const val MAX_FILE_BYTES = 128 * 1024

    fun encode(theme: YoryTheme): String {
        val appearance = JSONObject()
            .put("background", theme.backgroundArgb.toHex())
            .put("key", theme.keyArgb.toHex())
            .put("text", theme.textArgb.toHex())
            .put("accent", theme.accentArgb.toHex())
        val geometry = JSONObject()
            .put("cornerRadiusDp", theme.cornerRadiusDp)
            .put("keyHeightDp", theme.keyHeightDp)
        val sensory = JSONObject()
            .put("soundEnabled", theme.soundEnabled)
            .put("hapticsEnabled", theme.hapticsEnabled)
        val safeId = theme.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").take(32)
        return JSONObject()
            .put("schemaVersion", 1)
            .put("id", "local-" + safeId)
            .put("name", theme.name.take(48))
            .put("appearance", appearance)
            .put("glass", JSONObject().put("opacity", theme.opacity))
            .put("geometry", geometry)
            .put("sound", sensory)
            .toString(2)
    }

    fun decode(raw: String): YoryTheme {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) { "Theme file is too large." }
        val root = JSONObject(raw)
        require(root.optInt("schemaVersion", -1) == 1) { "Unsupported YORY theme version." }

        val name = root.optString("name", "Imported Theme").trim().take(48)
        require(name.isNotBlank()) { "Theme name is missing." }
        val appearance = root.optJSONObject("appearance")
            ?: throw IllegalArgumentException("Theme appearance is missing.")
        val glass = root.optJSONObject("glass") ?: JSONObject()
        val geometry = root.optJSONObject("geometry") ?: JSONObject()
        val sensory = root.optJSONObject("sound") ?: JSONObject()

        return YoryTheme(
            name = name,
            backgroundArgb = parseColor(appearance.optString("background", "#D91A1D2B")),
            keyArgb = parseColor(appearance.optString("key", "#E8F1F3FA")),
            textArgb = parseColor(appearance.optString("text", "#FF171923")),
            accentArgb = parseColor(appearance.optString("accent", "#FF78B8F8")),
            opacity = glass.optDouble("opacity", 0.92).toFloat().coerceIn(0.35f, 1f),
            cornerRadiusDp = geometry.optDouble("cornerRadiusDp", 14.0).toFloat().coerceIn(4f, 28f),
            keyHeightDp = geometry.optDouble("keyHeightDp", 46.0).toFloat().coerceIn(36f, 60f),
            soundEnabled = sensory.optBoolean("soundEnabled", true),
            hapticsEnabled = sensory.optBoolean("hapticsEnabled", true)
        )
    }

    private fun parseColor(raw: String): Int {
        val normalized = if (raw.startsWith("#") && raw.length == 7) "#FF" + raw.drop(1) else raw
        require(normalized.matches(Regex("^#[A-Fa-f0-9]{8}$"))) { "Invalid theme color." }
        return Color.parseColor(normalized)
    }

    private fun Int.toHex(): String = "#%08X".format(this)
}
