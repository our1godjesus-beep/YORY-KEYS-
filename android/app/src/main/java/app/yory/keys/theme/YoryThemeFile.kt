package app.yory.keys.theme

import android.graphics.Color
import org.json.JSONObject

object YoryThemeFile {
    const val MAX_FILE_BYTES = 128 * 1024

    fun encode(theme: YoryTheme): String {
        val appearance = JSONObject()
            .put("background", theme.backgroundArgb.toRgbHex())
            .put("key", theme.keyArgb.toRgbHex())
            .put("keyPressed", theme.accentArgb.toRgbHex())
            .put("text", theme.textArgb.toRgbHex())
            .put("accent", theme.accentArgb.toRgbHex())
        val geometry = JSONObject()
            .put("keyShape", "rounded")
            .put("cornerRadius", theme.cornerRadiusDp)
            .put("keyHeight", theme.keyHeightDp)
        val sensory = JSONObject().put("enabled", theme.soundEnabled)
        val haptics = JSONObject().put("enabled", theme.hapticsEnabled)
        val safeId = theme.name.lowercase().replace(Regex("[^a-z0-9]+"), "-").take(32)
        return JSONObject()
            .put("schemaVersion", 1)
            .put("id", "local-" + safeId)
            .put("name", theme.name.take(48))
            .put("appearance", appearance)
            .put("glass", JSONObject().put("opacity", theme.opacity))
            .put("geometry", geometry)
            .put("sound", sensory)
            .put("haptics", haptics)
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
        val haptics = root.optJSONObject("haptics") ?: JSONObject()

        return YoryTheme(
            name = name,
            backgroundArgb = parseColor(appearance.optString("background", "#D91A1D2B")),
            keyArgb = parseColor(appearance.optString("key", "#E8F1F3FA")),
            textArgb = parseColor(appearance.optString("text", "#FF171923")),
            accentArgb = parseColor(appearance.optString("accent", "#FF78B8F8")),
            opacity = glass.optDouble("opacity", 0.92).toFloat().coerceIn(0.35f, 1f),
            cornerRadiusDp = geometry.optDouble("cornerRadius", geometry.optDouble("cornerRadiusDp", 14.0)).toFloat().coerceIn(4f, 28f),
            keyHeightDp = geometry.optDouble("keyHeight", geometry.optDouble("keyHeightDp", 46.0)).toFloat().coerceIn(36f, 60f),
            soundEnabled = sensory.optBoolean("enabled", true),
            hapticsEnabled = haptics.optBoolean("enabled", true)
        )
    }

    private fun parseColor(raw: String): Int {
        require(raw.matches(Regex("^#[A-Fa-f0-9]{6}([A-Fa-f0-9]{2})?$"))) { "Invalid theme color." }
        val hex = raw.drop(1)
        val argb = if (hex.length == 8) {
            "#" + hex.substring(6, 8) + hex.substring(0, 6)
        } else {
            "#FF" + hex
        }
        return Color.parseColor(argb)
    }

    private fun Int.toRgbHex(): String = "#%06X".format(this and 0x00FFFFFF)
}
