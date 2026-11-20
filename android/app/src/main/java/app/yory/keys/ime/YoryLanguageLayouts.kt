package app.yory.keys.ime

/** Runtime keyboard layouts. A layout never implies that a prediction model is shipped. */
internal data class YoryKeyboardLayout(
    val tag: String,
    val name: String,
    val shortLabel: String,
    val rows: List<String>,
    val isRtl: Boolean = false,
    val hasBundledDictionary: Boolean = false
)

internal object YoryLanguageLayouts {
    val supported: List<YoryKeyboardLayout> = listOf(
        YoryKeyboardLayout("en-US", "English (US)", "English", listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"), hasBundledDictionary = true),
        YoryKeyboardLayout("ar", "العربية (مصر)", "العربية", listOf("ضصثقفغعهخح", "شسيبلاتنم", "ئءؤرىةوز"), isRtl = true, hasBundledDictionary = true),
        YoryKeyboardLayout("de", "Deutsch (DE)", "Deutsch", listOf("qwertzuiop", "asdfghjkl", "yxcvbnm")),
        YoryKeyboardLayout("es", "Español", "Español", listOf("qwertyuiop", "asdfghjklñ", "zxcvbnm")),
        YoryKeyboardLayout("fr", "Français", "Français", listOf("azertyuiop", "qsdfghjklm", "wxcvbn")),
        YoryKeyboardLayout("pt", "Português", "Português", listOf("qwertyuiop", "asdfghjklç", "zxcvbnm")),
        YoryKeyboardLayout("it", "Italiano", "Italiano", listOf("qwertyuiop", "asdfghjklò", "zxcvbnm"))
    )

    val supportedTags: List<String> get() = supported.map { it.tag }

    fun forTag(tag: String): YoryKeyboardLayout =
        supported.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
            ?: if (tag.lowercase().startsWith("en-")) supported.first()
            else supported.first()

    fun nextTag(current: String): String {
        val index = supported.indexOfFirst { it.tag.equals(current, ignoreCase = true) }
        return if (index < 0) supported.first().tag else supported[(index + 1) % supported.size].tag
    }
}
