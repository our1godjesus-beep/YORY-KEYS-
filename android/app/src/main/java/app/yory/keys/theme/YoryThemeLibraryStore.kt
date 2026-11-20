package app.yory.keys.theme

import android.content.Context

/**
 * Device-local collection and metadata for user-created YORY themes.
 * Presets are not stored here and can never be deleted through this API.
 */
object YoryThemeLibraryStore {
    private const val PREFS = "yory_theme_library"
    private const val KEY_THEMES = "saved_theme_documents"
    private const val KEY_FAVORITES = "favorite_theme_names"

    fun list(context: Context): List<YoryTheme> {
        val documents = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_THEMES, emptySet())
            .orEmpty()
        return documents.mapNotNull { raw ->
            runCatching { YoryThemeFile.decode(raw) }.getOrNull()
        }.distinctBy { it.name.lowercase() }.sortedBy { it.name.lowercase() }
    }

    fun find(context: Context, name: String): YoryTheme? =
        list(context).firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun save(context: Context, theme: YoryTheme) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_THEMES, emptySet())
            .orEmpty()
            .mapNotNull { raw -> runCatching { YoryThemeFile.decode(raw) }.getOrNull() }
            .filterNot { it.name.equals(theme.name, ignoreCase = true) }
            .toMutableList()
        updated += theme.copy(name = theme.name.trim().take(48))
        persist(prefs, updated)
    }

    fun rename(context: Context, oldName: String, newName: String): Boolean {
        val cleanName = newName.trim().take(48)
        if (cleanName.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = list(context).firstOrNull { it.name.equals(oldName, ignoreCase = true) } ?: return false
        if (!oldName.equals(cleanName, ignoreCase = true) &&
            list(context).any { it.name.equals(cleanName, ignoreCase = true) }
        ) return false
        val updated = list(context).filterNot { it.name.equals(oldName, ignoreCase = true) }
            .toMutableList()
        updated += current.copy(name = cleanName)
        persist(prefs, updated)
        val favorites = favorites(context).toMutableSet()
        if (favorites.remove(oldName.lowercase())) favorites.add(cleanName.lowercase())
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
        return true
    }

    fun duplicate(context: Context, source: YoryTheme, newName: String): Boolean {
        val cleanName = newName.trim().take(48)
        if (cleanName.isBlank() || list(context).any { it.name.equals(cleanName, ignoreCase = true) }) return false
        save(context, source.copy(name = cleanName))
        return true
    }

    fun delete(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = list(context).filterNot { it.name.equals(name, ignoreCase = true) }
        persist(prefs, updated)
        val favorites = favorites(context).toMutableSet()
        favorites.remove(name.lowercase())
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }

    fun favorites(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_FAVORITES, emptySet())
            .orEmpty()
            .map { it.lowercase() }
            .toSet()

    fun isFavorite(context: Context, name: String): Boolean =
        favorites(context).contains(name.lowercase())

    fun setFavorite(context: Context, name: String, favorite: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val updated = favorites(context).toMutableSet()
        if (favorite) updated.add(name.lowercase()) else updated.remove(name.lowercase())
        prefs.edit().putStringSet(KEY_FAVORITES, updated).apply()
    }

    private fun persist(
        prefs: android.content.SharedPreferences,
        themes: List<YoryTheme>
    ) {
        prefs.edit()
            .putStringSet(KEY_THEMES, themes.map { YoryThemeFile.encode(it) }.toSet())
            .apply()
    }
}
