package app.yory.keys.ime

/**
 * Short-lived state used only to undo the immediately preceding autocorrection.
 * The boundary is removed when the original spelling is restored.
 */
internal data class PendingAutocorrection(
    val original: String,
    val corrected: String,
    val boundary: String
) {
    val suffix: String
        get() = corrected + boundary

    fun matchesBeforeCursor(textBeforeCursor: String): Boolean =
        textBeforeCursor.endsWith(suffix)
}
