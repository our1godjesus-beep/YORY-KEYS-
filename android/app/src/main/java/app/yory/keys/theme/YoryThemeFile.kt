package app.yory.keys.theme

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.UUID

/**
 * Portable cross-platform .yorytheme file codec.
 * Uses plain JVM JSON and color parsing so validation can run in local unit tests.
 */
object YoryThemeFile {
    const val MAX_FILE_BYTES = 128 * 1024

    fun encode(theme: YoryTheme): String {
        val appearance = JsonObject().apply {
            addProperty("background", theme.backgroundArgb.toRgbaHex())
            addProperty("key", theme.keyArgb.toRgbaHex())
            addProperty("keyPressed", theme.keyPressedArgb.toRgbaHex())
            addProperty("text", theme.textArgb.toRgbaHex())
            addProperty("accent", theme.accentArgb.toRgbaHex())
        }
        val geometry = JsonObject().apply {
            addProperty("keyShape", "rounded")
            addProperty("cornerRadius", theme.cornerRadiusDp)
            addProperty("keyHeight", theme.keyHeightDp)
            addProperty("keySpacing", theme.keySpacingDp)
            addProperty("keyShape", theme.keyShape)
        }
        val sound = JsonObject().apply { addProperty("enabled", theme.soundEnabled) }
        val haptics = JsonObject().apply { addProperty("enabled", theme.hapticsEnabled) }
        val glass = JsonObject().apply { addProperty("opacity", theme.opacity) }
        val typography = JsonObject().apply {
            addProperty("fontSize", theme.fontSizeSp)
            addProperty("fontFamily", theme.fontFamily)
        }

        val root = JsonObject().apply {
            addProperty("schemaVersion", 1)
            addProperty("id", UUID.randomUUID().toString())
            addProperty("name", theme.name.take(48))
            add("appearance", appearance)
            add("glass", glass)
            add("geometry", geometry)
            add("sound", sound)
            add("haptics", haptics)
            add("typography", typography)
        }
        return GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root)
    }

    fun decode(raw: String): YoryTheme {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) {
            "Theme file is too large."
        }
        try {
            val root = JsonParser.parseString(raw).asJsonObject
            require(root.intValue("schemaVersion", -1) == 1) {
                "Unsupported YORY theme version."
            }

            val name = root.stringValue("name", "Imported Theme").trim().take(48)
            require(name.isNotBlank()) { "Theme name is missing." }

            val appearance = root.objectValue("appearance")
                ?: throw IllegalArgumentException("Theme appearance is missing.")
            val glass = root.objectValue("glass") ?: JsonObject()
            val geometry = root.objectValue("geometry") ?: JsonObject()
            val sound = root.objectValue("sound") ?: JsonObject()
            val haptics = root.objectValue("haptics") ?: JsonObject()
            val typography = root.objectValue("typography") ?: JsonObject()

            return YoryTheme(
                name = name,
                backgroundArgb = parseColor(appearance.stringValue("background", "#1A1D2BD9")),
                keyArgb = parseColor(appearance.stringValue("key", "#F1F3FAE8")),
                textArgb = parseColor(appearance.stringValue("text", "#171923FF")),
                accentArgb = parseColor(appearance.stringValue("accent", "#78B8F8FF")),
                keyPressedArgb = parseColor(appearance.stringValue("keyPressed", appearance.stringValue("accent", "#78B8F8FF"))),
                opacity = glass.floatValue("opacity", 0.92f).coerceIn(0.35f, 1f),
                cornerRadiusDp = geometry.floatValue(
                    "cornerRadius",
                    geometry.floatValue("cornerRadiusDp", 14f)
                ).coerceIn(4f, 28f),
                keyHeightDp = geometry.floatValue(
                    "keyHeight",
                    geometry.floatValue("keyHeightDp", 46f)
                ).coerceIn(36f, 60f),
                keySpacingDp = geometry.floatValue("keySpacing", 4f).coerceIn(0f, 12f),
                keyShape = geometry.stringValue("keyShape", "rounded").takeIf {
                    it in setOf("rounded", "squircle", "pill", "circle", "rect")
                } ?: "rounded",
                fontSizeSp = typography.floatValue("fontSize", 18f).coerceIn(12f, 26f),
                fontFamily = typography.stringValue("fontFamily", "system").takeIf {
                    it in setOf("system", "rounded", "serif", "mono")
                } ?: "system",
                soundEnabled = sound.booleanValue("enabled", true),
                hapticsEnabled = haptics.booleanValue("enabled", true)
            )
        } catch (error: IllegalArgumentException) {
            throw error
        } catch (error: Exception) {
            throw IllegalArgumentException("Invalid YORY theme file.", error)
        }
    }

    private fun parseColor(raw: String): Int {
        require(raw.matches(Regex("^#[A-Fa-f0-9]{6}([A-Fa-f0-9]{2})?$"))) {
            "Invalid theme color."
        }
        val hex = raw.drop(1)
        val red = hex.substring(0, 2).toInt(16)
        val green = hex.substring(2, 4).toInt(16)
        val blue = hex.substring(4, 6).toInt(16)
        val alpha = if (hex.length == 8) hex.substring(6, 8).toInt(16) else 255
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun Int.toRgbaHex(): String {
        val red = (this ushr 16) and 0xFF
        val green = (this ushr 8) and 0xFF
        val blue = this and 0xFF
        val alpha = (this ushr 24) and 0xFF
        return "#%02X%02X%02X%02X".format(red, green, blue, alpha)
    }

    private fun JsonObject.stringValue(key: String, fallback: String): String =
        get(key)?.takeUnless { it.isJsonNull }?.asString ?: fallback

    private fun JsonObject.intValue(key: String, fallback: Int): Int =
        get(key)?.takeUnless { it.isJsonNull }?.asInt ?: fallback

    private fun JsonObject.floatValue(key: String, fallback: Float): Float =
        get(key)?.takeUnless { it.isJsonNull }?.asFloat ?: fallback

    private fun JsonObject.booleanValue(key: String, fallback: Boolean): Boolean =
        get(key)?.takeUnless { it.isJsonNull }?.asBoolean ?: fallback

    private fun JsonObject.objectValue(key: String): JsonObject? =
        get(key)?.takeUnless { it.isJsonNull }?.takeIf { it.isJsonObject }?.asJsonObject
}
