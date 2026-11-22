package app.yory.keys.ime

/**
 * Secondary legends and long-press choices for the physical position of each key.
 * Position-based mapping keeps the shortcuts useful on QWERTZ, AZERTY and RTL layouts.
 */
internal object YoryKeyLegends {
    private val westernDigits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val arabicDigits = listOf(
        "1 ١", "2 ٢", "3 ٣", "4 ٤", "5 ٥", "6 ٦",
        "7 ٧", "8 ٨", "9 ٩", "0 ٠", "-", "="
    )

    private val latinSymbols = mapOf(
        1 to listOf("@", "$", "&", "-", "+", "(", ")", "/", ":"),
        2 to listOf("!", "?", "'", "\"", ";", "_", ".")
    )

    private val arabicSymbols = mapOf(
        1 to listOf("@", "#", "&", "پ", "لا", "أ", "=", "(", ")", "گ", "*"),
        2 to listOf("−", "~", "−", "+", "؛", ":", "/", "ژ", "”", "◌")
    )

    fun forRow(languageTag: String, rowIndex: Int, keys: String): Map<String, String> {
        val isArabic = languageTag.startsWith("ar", ignoreCase = true)
        val legends = when {
            isArabic && rowIndex == 0 -> arabicDigits
            isArabic -> arabicSymbols[rowIndex].orEmpty()
            rowIndex == 0 -> westernDigits
            else -> latinSymbols[rowIndex].orEmpty()
        }
        return keys.mapIndexedNotNull { index, key ->
            legends.getOrNull(index)?.let { key.toString() to it }
        }.toMap()
    }

    fun optionsForKey(
        key: String,
        secondaryLegend: String?,
        languageTag: String,
        showSymbols: Boolean,
        showAccents: Boolean
    ): List<String> {
        val options = mutableListOf<String>()
        if (showSymbols) {
            secondaryLegend
                ?.split(Regex("\\s+"))
                ?.filter { it.isNotBlank() && it != "◌" }
                ?.let(options::addAll)
        }
        if (showAccents) {
            val normalized = key.lowercase()
            val accents = when {
                languageTag.startsWith("ar", ignoreCase = true) ->
                    YoryArabicAlternates.optionsForKey(normalized)
                languageTag.startsWith("de", ignoreCase = true) -> when (normalized) {
                    "a" -> listOf("ä")
                    "o" -> listOf("ö")
                    "u" -> listOf("ü")
                    "s" -> listOf("ß")
                    else -> emptyList()
                }
                languageTag.startsWith("fr", ignoreCase = true) -> when (normalized) {
                    "a" -> listOf("à", "â", "æ")
                    "e" -> listOf("é", "è", "ê", "ë")
                    "i" -> listOf("î", "ï")
                    "o" -> listOf("ô", "œ")
                    "u" -> listOf("ù", "û", "ü")
                    "c" -> listOf("ç")
                    else -> emptyList()
                }
                languageTag.startsWith("es", ignoreCase = true) -> when (normalized) {
                    "a" -> listOf("á")
                    "e" -> listOf("é")
                    "i" -> listOf("í")
                    "o" -> listOf("ó")
                    "u" -> listOf("ú", "ü")
                    "n" -> listOf("ñ")
                    else -> emptyList()
                }
                languageTag.startsWith("pt", ignoreCase = true) -> when (normalized) {
                    "a" -> listOf("á", "à", "â", "ã")
                    "e" -> listOf("é", "ê")
                    "i" -> listOf("í")
                    "o" -> listOf("ó", "ô", "õ")
                    "u" -> listOf("ú")
                    "c" -> listOf("ç")
                    else -> emptyList()
                }
                languageTag.startsWith("it", ignoreCase = true) -> when (normalized) {
                    "a" -> listOf("à")
                    "e" -> listOf("è", "é")
                    "i" -> listOf("ì")
                    "o" -> listOf("ò")
                    "u" -> listOf("ù")
                    else -> emptyList()
                }
                else -> when (normalized) {
                    "a" -> listOf("á", "à", "â", "ä", "ã", "å")
                    "e" -> listOf("é", "è", "ê", "ë")
                    "i" -> listOf("í", "ì", "î", "ï")
                    "o" -> listOf("ó", "ò", "ô", "ö", "õ")
                    "u" -> listOf("ú", "ù", "û", "ü")
                    "n" -> listOf("ñ")
                    "c" -> listOf("ç")
                    "y" -> listOf("ý", "ÿ")
                    else -> emptyList()
                }
            }
            options.addAll(accents)
        }
        return options.distinct()
    }
}
