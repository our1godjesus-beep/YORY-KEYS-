package app.yory.keys.ime

import java.util.Locale
import kotlin.math.abs

/**
 * Conservative local spelling correction.
 *
 * Only curated known errors or one-edit-distance candidates with a unique
 * best match are corrected. Ambiguous words are deliberately left untouched.
 */
internal object LocalAutocorrectMatcher {
    private val explicitCorrections: Map<String, Map<String, String>> = mapOf(
        "en" to mapOf(
            "teh" to "the", "adn" to "and", "thier" to "their",
            "becuase" to "because", "recieve" to "receive", "freind" to "friend",
            "definately" to "definitely", "seperate" to "separate",
            "wierd" to "weird", "untill" to "until", "goverment" to "government",
            "occured" to "occurred", "adress" to "address", "langauge" to "language",
            "keybaord" to "keyboard", "taht" to "that", "whcih" to "which",
            "hlelo" to "hello", "wrold" to "world", "becasue" to "because"
        ),
        "ar" to mapOf(
            "لاكن" to "لكن", "هاذا" to "هذا", "هاذه" to "هذه",
            "الغه" to "اللغة", "الكيبور" to "الكيبورد",
            "التصمبم" to "التصميم", "المشروغ" to "المشروع",
            "بسرعه" to "بسرعة", "الان" to "الآن"
        )
    )

    fun correct(
        rawWord: String,
        languageTag: String,
        candidates: List<String>,
        normalize: (String) -> String = { it.lowercase(Locale.ROOT) }
    ): String? {
        val word = rawWord.trim()
        if (word.codePointCount(0, word.length) !in 3..32) return null

        val language = languageTag.substringBefore('-').lowercase(Locale.ROOT)
        val explicit = explicitCorrections[language]?.get(word.lowercase(Locale.ROOT))
        if (explicit != null) return preserveCase(word, explicit)

        val normalizedInput = normalize(word)
        val normalizedCandidates = candidates
            .asSequence()
            .map { it to normalize(it) }
            .filter { it.second.isNotEmpty() }
            .distinctBy { it.second }
            .toList()

        // Never "correct" a word already represented in the active dictionary.
        if (normalizedCandidates.any { it.second == normalizedInput }) return null

        var bestDistance = 2
        var bestCandidate: String? = null
        var ambiguousBest = false

        for ((candidate, normalizedCandidate) in normalizedCandidates) {
            val left = normalizedInput.codePoints().toArray()
            val right = normalizedCandidate.codePoints().toArray()
            if (abs(left.size - right.size) > 1) continue

            val distance = editDistanceAtMostOne(left, right) ?: continue
            if (distance < bestDistance) {
                bestDistance = distance
                bestCandidate = candidate
                ambiguousBest = false
            } else if (distance == bestDistance && distance == 1) {
                ambiguousBest = true
            }
            if (bestDistance == 1 && ambiguousBest) break
        }

        if (bestDistance != 1 || ambiguousBest || bestCandidate == null) return null
        return preserveCase(word, bestCandidate)
    }

    private fun editDistanceAtMostOne(first: IntArray, second: IntArray): Int? {
        if (first.contentEquals(second)) return 0
        if (abs(first.size - second.size) > 1) return null

        if (first.size == second.size) {
            val differences = first.indices.filter { first[it] != second[it] }
            if (differences.size == 1) return 1
            if (differences.size == 2) {
                val a = differences[0]
                val b = differences[1]
                if (b == a + 1 && first[a] == second[b] && first[b] == second[a]) return 1
            }
            return null
        }

        val shorter = if (first.size < second.size) first else second
        val longer = if (first.size < second.size) second else first
        var shortIndex = 0
        var longIndex = 0
        var edits = 0
        while (shortIndex < shorter.size && longIndex < longer.size) {
            if (shorter[shortIndex] == longer[longIndex]) {
                shortIndex++
                longIndex++
            } else {
                edits++
                if (edits > 1) return null
                longIndex++
            }
        }
        if (longIndex < longer.size || shortIndex < shorter.size) edits++
        return if (edits == 1) 1 else null
    }

    private fun preserveCase(original: String, replacement: String): String = when {
        original.isNotEmpty() && original.all { !it.isLetter() || it.isUpperCase() } ->
            replacement.uppercase(Locale.ROOT)
        original.firstOrNull()?.isUpperCase() == true ->
            replacement.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        else -> replacement
    }
}
