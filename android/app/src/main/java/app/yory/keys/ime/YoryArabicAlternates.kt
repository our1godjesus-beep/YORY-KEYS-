package app.yory.keys.ime

/**
 * Arabic alternates exposed by long-pressing an Arabic letter.
 *
 * The first group contains letter variants; every Arabic letter also exposes
 * the complete common tashkeel set. Marks are inserted as combining characters.
 */
internal object YoryArabicAlternates {
    val tashkeelMarks: List<String> = listOf(
        "َ", // Fatha
        "ُ", // Damma
        "ِ", // Kasra
        "ْ", // Sukun
        "ّ", // Shadda
        "ً", // Fathatan
        "ٌ", // Dammatan
        "ٍ", // Kasratan
        "ٰ", // Dagger alif
        "ٓ"  // Madda above
    )

    private val variantsByKey = mapOf(
        "ا" to listOf("أ", "إ", "آ", "ٱ"),
        "و" to listOf("ؤ"),
        "ي" to listOf("ئ", "ى"),
        "ه" to listOf("ة")
    )

    private val markNames = mapOf(
        "َ" to "فتحة (Fatha)",
        "ُ" to "ضمة (Damma)",
        "ِ" to "كسرة (Kasra)",
        "ْ" to "سكون (Sukun)",
        "ّ" to "شدة (Shadda)",
        "ً" to "تنوين فتح (Fathatan)",
        "ٌ" to "تنوين ضم (Dammatan)",
        "ٍ" to "تنوين كسر (Kasratan)",
        "ٰ" to "ألف خنجرية (Dagger alif)",
        "ٓ" to "مدة فوق (Madda above)"
    )

    fun optionsForKey(key: String): List<String> =
        (variantsByKey[key] ?: emptyList()) + tashkeelMarks

    fun isTashkeel(value: String): Boolean = value in markNames

    fun displayLabel(value: String): String =
        if (isTashkeel(value)) "◌$value" else value

    fun accessibilityLabel(value: String, baseLetter: String? = null): String =
        when {
            isTashkeel(value) && !baseLetter.isNullOrBlank() ->
                "الحرف " + baseLetter + " مع " + markNames.getValue(value)
            isTashkeel(value) -> markNames.getValue(value)
            else -> "حرف بديل " + value
        }

    /** A diacritic long-press choice inserts its held base letter before the mark. */
    fun insertionText(baseLetter: String, option: String): String =
        if (isTashkeel(option)) baseLetter + option else option
}
