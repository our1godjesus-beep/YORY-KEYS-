package app.yory.keys.theme

import android.content.Context

/** Local-only collection of custom YORY themes. Built-in presets remain separate. */
object YoryThemeLibraryStore {
    private const val PREFS = "yory_theme_library"
    private const val KEY_THEMES = "saved_theme_documents"

    fun list(context: Context): List<YoryTheme> {
        val documents = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_THEMES, emptySet())
            .orEmpty()
        return documents.mapNotNull { raw ->
            runCatching { YoryThemeFile.decode(raw) }.getOrNull()
        }.distinctBy { it.name.lowercase() }.sortedBy { it.name.lowercase() }
    }

    fun save(context: Context, theme: YoryTheme) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_THEMES, emptySet())
            .orEmpty()
            .mapNotNull { raw -> runCatching { YoryThemeFile.decode(raw) }.getOrNull() }
            .filterNot { it.name.equals(theme.name, ignoreCase = true) }
            .toMutableList()
        updated += theme
        prefs.edit()
            .putStringSet(KEY_THEMES, updated.map { YoryThemeFile.encode(it) }.toSet())
            .apply()
    }

    fun delete(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_THEMES, emptySet())
            .orEmpty()
            .mapNotNull { raw -> runCatching { YoryThemeFile.decode(raw) }.getOrNull() }
            .filterNot { it.name.equals(name, ignoreCase = true) }
        prefs.edit()
            .putStringSet(KEY_THEMES, updated.map { YoryThemeFile.encode(it) }.toSet())
            .apply()
    }
}
