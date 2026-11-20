package app.yory.keys.ime

/** Pure local emoji catalogue and preference-list operations used by the Android IME. */
internal object YoryEmojiCatalog {
    data class Category(val id: String, val icon: String)

    val categories = listOf(
        Category("Recent", "◷"),
        Category("Favorites", "★"),
        Category("Smile", "☺"),
        Category("People", "👥"),
        Category("Nature", "🌿"),
        Category("Food", "🍕"),
        Category("Travel", "✈"),
        Category("Symbols", "♡")
    )

    fun values(category: String, recent: List<String>, favorites: List<String>): List<String> =
        when (category) {
            "Recent" -> recent.ifEmpty { listOf("😀", "😂", "🥰", "❤️", "✨", "🙏", "👍", "🔥") }
            "Favorites" -> favorites
            "Smile" -> listOf("😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "😉", "😊", "😍", "🥰")
            "People" -> listOf("👋", "👌", "✌", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇", "☝", "👍", "👎")
            "Nature" -> listOf("🐶", "🐱", "🐻", "🦊", "🐼", "🐸", "🦋", "🌸", "🌹", "🌻", "🌈", "🌙", "⭐", "🌍")
            "Food" -> listOf("🍎", "🍓", "🍉", "🍕", "🍔", "🍟", "🍿", "🍩", "🍪", "🎂", "☕", "🧋", "🥗", "🍣")
            "Travel" -> listOf("🚗", "🚕", "🚌", "🚆", "✈️", "🚀", "🚲", "⛵", "🏝️", "🏖️", "🏙️", "🗽", "🏰", "🌋")
            "Symbols" -> listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "💕", "💞", "💯", "✅", "✨")
            else -> emptyList()
        }.distinct().take(80)

    fun parseStored(raw: String?): List<String> =
        raw.orEmpty().lineSequence().map(String::trim).filter { it.isNotEmpty() }.distinct().take(80).toList()

    fun encodeStored(items: List<String>, limit: Int): String =
        items.map(String::trim).filter { it.isNotEmpty() }.distinct().take(limit.coerceAtLeast(0)).joinToString("\n")

    fun addRecent(current: List<String>, emoji: String, limit: Int = 24): List<String> {
        if (emoji.isBlank()) return current.distinct().take(limit.coerceAtLeast(0))
        return (listOf(emoji) + current.filterNot { it == emoji }).distinct().take(limit.coerceAtLeast(0))
    }

    fun toggleFavorite(current: List<String>, emoji: String, limit: Int = 80): List<String> {
        if (emoji.isBlank()) return current.distinct().take(limit.coerceAtLeast(0))
        val without = current.filterNot { it == emoji }
        return if (current.any { it == emoji }) without.distinct().take(limit.coerceAtLeast(0))
        else (listOf(emoji) + without).distinct().take(limit.coerceAtLeast(0))
    }

    fun applySkinTone(emoji: String, tone: String): String {
        if (emoji.isBlank() || tone.isBlank()) return emoji
        val firstCodePoint = emoji.codePointAt(0)
        val firstLength = Character.charCount(firstCodePoint)
        return emoji.substring(0, firstLength) + tone + emoji.substring(firstLength)
    }
}
