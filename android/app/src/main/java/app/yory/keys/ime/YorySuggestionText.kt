package app.yory.keys.ime

import java.text.Normalizer
import java.util.Locale

/**
 * Search-only normalization for local suggestions.
 *
 * This never changes text committed to the editor. It only lets a user find
 * an existing candidate when Arabic alef/hamza or tashkeel differs.
 */
internal object YorySuggestionText {
    fun normalizeForMatch(value: String): String {
        val source = Normalizer.normalize(value, Normalizer.Form.NFC)
        return buildString(source.length) {
            source.forEach { char ->
                val code = char.code
                val isArabicMark = code in 0x064B..0x065F ||
                    code == 0x0670 ||
                    code in 0x06D6..0x06ED
                if (char == '\u0640' || isArabicMark) return@forEach
                append(
                    when (char) {
                        'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                        'ى', 'ی' -> 'ي'
                        'ک' -> 'ك'
                        else -> char
                    }
                )
            }
        }.lowercase(Locale.ROOT)
    }

    fun matchesPrefix(candidate: String, prefix: String): Boolean {
        val normalizedCandidate = normalizeForMatch(candidate)
        val normalizedPrefix = normalizeForMatch(prefix)
        return normalizedPrefix.isNotEmpty() &&
            normalizedCandidate.length > normalizedPrefix.length &&
            normalizedCandidate.startsWith(normalizedPrefix)
    }

    fun rankByTypedPrefix(candidates: List<String>, typedPrefix: String): List<String> {
        val direct = ArrayList<String>()
        val normalized = ArrayList<String>()
        val seen = HashSet<String>()
        candidates.forEach { candidate ->
            val key = candidate.lowercase(Locale.ROOT)
            if (seen.add(key) && matchesPrefix(candidate, typedPrefix)) {
                if (candidate.startsWith(typedPrefix, ignoreCase = true)) direct.add(candidate)
                else normalized.add(candidate)
            }
        }
        return direct + normalized
    }
}
