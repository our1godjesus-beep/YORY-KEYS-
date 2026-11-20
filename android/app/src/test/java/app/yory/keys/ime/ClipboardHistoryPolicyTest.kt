package app.yory.keys.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardHistoryPolicyTest {
    @Test
    fun rejectsCommonSensitivePatterns() {
        assertFalse(ClipboardHistoryPolicy.isSafeToStore("123456"))
        assertFalse(ClipboardHistoryPolicy.isSafeToStore("4111 1111 1111 1111"))
        assertFalse(ClipboardHistoryPolicy.isSafeToStore("Your verification code is 482910"))
        assertFalse(ClipboardHistoryPolicy.isSafeToStore("password=example-secret"))
    }

    @Test
    fun acceptsOrdinaryTextAndLimitsLength() {
        assertTrue(ClipboardHistoryPolicy.isSafeToStore("YORY KEYS is a keyboard."))
        assertFalse(ClipboardHistoryPolicy.isSafeToStore("x".repeat(ClipboardHistoryPolicy.MAX_TEXT_LENGTH + 1)))
        assertFalse(ClipboardHistoryPolicy.isSafeToStore("   "))
    }

    @Test
    fun unpinnedEntriesExpireButPinnedEntriesRemain() {
        val expired = YoryClipboardEntry("a", "old", 1L, false, 100L)
        val pinned = YoryClipboardEntry("b", "keep", 2L, true, 100L)
        val active = YoryClipboardEntry("c", "new", 3L, false, 500L)
        val result = ClipboardHistoryPolicy.retainActive(listOf(active, expired, pinned), 200L)
        assertEquals(listOf(pinned, active), result)
    }

    @Test
    fun pinningClearsExpiryAndUnpinningStartsFreshWindow() {
        val entry = YoryClipboardEntry("a", "text", 10L, false, 20L)
        val pinned = ClipboardHistoryPolicy.setPinned(entry, true, 30L)
        assertTrue(pinned.isPinned)
        assertEquals(Long.MAX_VALUE, pinned.expiresAt)
        val unpinned = ClipboardHistoryPolicy.setPinned(pinned, false, 40L)
        assertFalse(unpinned.isPinned)
        assertEquals(40L + ClipboardHistoryPolicy.EXPIRY_MS, unpinned.expiresAt)
    }

    @Test
    fun searchIsCaseInsensitiveAndTrimsWhitespace() {
        val entry = YoryClipboardEntry("a", "YORY Keyboard Settings", 10L, false, 1000L)
        assertTrue(ClipboardHistoryPolicy.matchesSearch(entry, " yory "))
        assertTrue(ClipboardHistoryPolicy.matchesSearch(entry, "SETTINGS"))
        assertFalse(ClipboardHistoryPolicy.matchesSearch(entry, "clipboard"))
        assertTrue(ClipboardHistoryPolicy.matchesSearch(entry, "  "))
    }

    @Test
    fun retentionIsCappedAtFiftyEntries() {
        val entries = (1..60).map { YoryClipboardEntry(it.toString(), "item-$it", it.toLong(), false, 1000L) }
        assertEquals(ClipboardHistoryPolicy.MAX_ITEMS, ClipboardHistoryPolicy.retainActive(entries, 1L).size)
    }
}
