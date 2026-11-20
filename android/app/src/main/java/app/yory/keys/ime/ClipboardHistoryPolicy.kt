package app.yory.keys.ime

/** Pure validation and expiry rules for local clipboard data. */
internal object ClipboardHistoryPolicy {
    const val MAX_ITEMS = 50
    const val MAX_TEXT_LENGTH = 2000
    const val EXPIRY_MS = 24L * 60L * 60L * 1000L

    fun isSafeToStore(text: String): Boolean {
        val clean = text.trim()
        if (clean.isBlank() || clean.length > MAX_TEXT_LENGTH) return false
        val compact = clean.filterNot { it.isWhitespace() || it in "-()." }
        if (compact.matches(Regex("\\d{4,10}"))) return false
        if (compact.matches(Regex("\\d{12,19}"))) return false
        if (Regex("(?i)otp|one[- ]time|verification code|رمز التحقق|كود التحقق").containsMatchIn(clean)) return false
        if (Regex("(?i)(password|passcode|secret|token)\\s*[:=]").containsMatchIn(clean)) return false
        return true
    }

    fun matchesSearch(entry: YoryClipboardEntry, query: String): Boolean {
        val clean = query.trim()
        return clean.isEmpty() || entry.text.contains(clean, ignoreCase = true)
    }

    fun retainActive(entries: List<YoryClipboardEntry>, now: Long): List<YoryClipboardEntry> =
        entries.filter { it.isPinned || it.expiresAt > now }
            .sortedWith(compareByDescending<YoryClipboardEntry> { it.isPinned }.thenByDescending { it.copiedAt })
            .take(MAX_ITEMS)

    fun setPinned(entry: YoryClipboardEntry, pinned: Boolean, now: Long): YoryClipboardEntry =
        entry.copy(isPinned = pinned, expiresAt = if (pinned) Long.MAX_VALUE else now + EXPIRY_MS)
}
