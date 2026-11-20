package app.yory.keys.ime

import android.content.Context
import java.util.Locale

/** Offline prefix-completion baseline. This is not a neural prediction model. */
class LocalWordSuggestions(private val context: Context) {
    private val dictionaries = mapOf(
        "en" to loadWords(context, "dictionaries/en-US.txt"),
        "ar" to loadWords(context, "dictionaries/ar.txt")
    )

    fun suggest(prefix: String, languageTag: String, limit: Int = 3): List<String> {
        val clean = prefix.trim().lowercase(Locale.ROOT)
        if (clean.length < 2) return emptyList()
        val language = languageTag.substringBefore('-').lowercase(Locale.ROOT)
        val personal = PersonalDictionaryStore.suggestions(context, clean, languageTag, limit)
        val bundled = dictionaries[language].orEmpty()
            .asSequence()
            .filter { it.startsWith(clean) && !it.equals(clean, ignoreCase = true) }
            .distinct()
            .take(limit.coerceIn(1, 5))
            .toList()
        return (personal + bundled).distinct().take(limit.coerceIn(1, 5))
    }

    private fun loadWords(context: Context, path: String): List<String> = runCatching {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map(String::trim).filter { it.length in 2..32 }.toList()
        }
    }.getOrDefault(emptyList())
}
