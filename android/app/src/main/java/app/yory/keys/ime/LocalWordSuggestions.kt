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
        val candidates = personal + bundled
        return YorySuggestionText.rankByTypedPrefix(candidates, clean)
            .take(limit.coerceIn(1, 5))
    }

    fun correct(rawWord: String, languageTag: String): String? {
        val word = rawWord.trim()
        if (word.length !in 3..32) return null
        val language = languageTag.substringBefore('-').lowercase(Locale.ROOT)
        val folded = word.lowercase(Locale.ROOT)

        val explicit = when (language) {
            "en" -> mapOf(
                "teh" to "the", "adn" to "and", "thier" to "their",
                "becuase" to "because", "recieve" to "receive", "freind" to "friend",
                "definately" to "definitely", "seperate" to "separate",
                "wierd" to "weird", "untill" to "until", "goverment" to "government",
                "occured" to "occurred", "adress" to "address", "langauge" to "language",
                "keybaord" to "keyboard", "taht" to "that", "whcih" to "which",
                "hlelo" to "hello", "wrold" to "world", "becasue" to "because"
            )
            "ar" -> mapOf(
                "لاكن" to "لكن", "هاذا" to "هذا", "هاذه" to "هذه",
                "الغه" to "اللغة", "الكيبور" to "الكيبورد",
                "التصمبم" to "التصميم", "المشروغ" to "المشروع",
                "بسرعه" to "بسرعة", "الان" to "الآن"
            )
            else -> emptyMap()
        }
        explicit[folded]?.let { correction ->
            return preserveCase(word, correction)
        }

        val candidates = dictionaries[language].orEmpty()
        if (candidates.isEmpty()) return null
        val normalizedInput = normalize(word)
        if (candidates.any { normalize(it) == normalizedInput }) return null

        val best = ArrayList<String>(2)
        var bestDistance = 2
        for (candidate in candidates) {
            if (kotlin.math.abs(candidate.length - word.length) > 1) continue
            val distance = editDistanceAtMostOne(folded, candidate.lowercase(Locale.ROOT)) ?: continue
            if (distance < bestDistance) {
                bestDistance = distance
                best.clear()
                best.add(candidate)
            } else if (distance == bestDistance && best.none { it.equals(candidate, true) }) {
                best.add(candidate)
            }
            if (best.size > 1 && bestDistance == 1) break
        }
        if (bestDistance != 1 || best.size != 1) return null
        return preserveCase(word, best.single())
    }

    private fun normalize(value: String): String =
        YorySuggestionText.normalizeForMatch(value)

    private fun editDistanceAtMostOne(first: String, second: String): Int? {
        if (first == second) return 0
        if (kotlin.math.abs(first.length - second.length) > 1) return null
        if (first.length == second.length) {
            val differences = first.indices.filter { first[it] != second[it] }
            if (differences.size == 1) return 1
            if (differences.size == 2) {
                val a = differences[0]
                val b = differences[1]
                if (b == a + 1 && first[a] == second[b] && first[b] == second[a]) return 1
            }
            return null
        }
        val shorter = if (first.length < second.length) first else second
        val longer = if (first.length < second.length) second else first
        var i = 0
        var j = 0
        var edits = 0
        while (i < shorter.length && j < longer.length) {
            if (shorter[i] == longer[j]) {
                i++; j++
            } else {
                edits++
                if (edits > 1) return null
                j++
            }
        }
        if (j < longer.length || i < shorter.length) edits++
        return if (edits == 1) 1 else null
    }

    private fun preserveCase(original: String, replacement: String): String = when {
        original.all { !it.isLetter() || it.isUpperCase() } -> replacement.uppercase(Locale.ROOT)
        original.firstOrNull()?.isUpperCase() == true ->
            replacement.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        else -> replacement
    }

    private fun loadWords(context: Context, path: String): List<String> = runCatching {
        context.assets.open(path).bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.map(String::trim).filter { it.length in 2..32 }.toList()
        }
    }.getOrDefault(emptyList())
}
