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
        YoryKeyboardLayout("ar", "العربية (مصر)", "مصري", listOf("ضصثقفغعهخحجد", "شسيبلاتنمكذ", "طظئءؤرىةوز"), isRtl = true, hasBundledDictionary = true),
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

    /**
     * Cycles only through layouts explicitly added by the user.
     * Direction +1 means next (spacebar swipe left); -1 means previous (swipe right).
     */
    fun nextTag(
        current: String,
        availableTags: Collection<String> = supportedTags,
        direction: Int = 1
    ): String {
        val enabled = supported.filter { layout ->
            availableTags.any { it.equals(layout.tag, ignoreCase = true) }
        }
        if (enabled.isEmpty()) return supported.first().tag
        if (enabled.size == 1) return enabled.first().tag

        val index = enabled.indexOfFirst { it.tag.equals(current, ignoreCase = true) }
        if (index < 0) return enabled.first().tag
        val step = if (direction < 0) -1 else 1
        return enabled[(index + step + enabled.size) % enabled.size].tag
    }
}
