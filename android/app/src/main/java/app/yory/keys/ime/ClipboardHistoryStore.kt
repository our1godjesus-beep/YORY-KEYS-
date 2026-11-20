package app.yory.keys.ime

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class YoryClipboardEntry(val text: String, val copiedAt: Long)

object ClipboardHistoryStore {
    private const val FILE = "yory_clipboard_history"
    private const val KEY = "items"
    private const val MAX_ITEMS = 50
    private const val MAX_TEXT_LENGTH = 2000

    fun entries(context: Context): List<YoryClipboardEntry> {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val text = item.optString("text").takeIf { it.isNotBlank() } ?: continue
                    add(YoryClipboardEntry(text, item.optLong("copiedAt")))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, rawText: String) {
        val text = rawText.trim()
        if (!isSafeToStore(text)) return
        val current = entries(context).filterNot { it.text == text }
        val updated = listOf(YoryClipboardEntry(text, System.currentTimeMillis())) + current
        val json = JSONArray()
        updated.take(MAX_ITEMS).forEach { entry ->
            json.put(JSONObject().put("text", entry.text).put("copiedAt", entry.copiedAt))
        }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, json.toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun isSafeToStore(text: String): Boolean {
        if (text.isBlank() || text.length > MAX_TEXT_LENGTH) return false
        val compact = text.filterNot(Char::isWhitespace)
        if (compact.matches(Regex("\\d{4,10}"))) return false
        if (compact.matches(Regex("\\d{12,19}"))) return false
        if (Regex("(?i)otp|one[- ]time|verification code|رمز التحقق|كود التحقق").containsMatchIn(text)) return false
        return true
    }
}
