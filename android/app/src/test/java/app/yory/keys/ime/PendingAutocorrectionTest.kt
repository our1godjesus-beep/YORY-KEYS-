package app.yory.keys.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingAutocorrectionTest {
    @Test
    fun immediateBackspaceTargetMatchesCorrectedWordAndBoundary() {
        val pending = PendingAutocorrection(
            original = "teh",
            corrected = "the",
            boundary = " "
        )
        assertEquals("the ", pending.suffix)
        assertTrue(pending.matchesBeforeCursor("Please type the "))
    }

    @Test
    fun doesNotUndoAfterTheUserHasContinuedTypingOrMovedTheCursor() {
        val pending = PendingAutocorrection(
            original = "teh",
            corrected = "the",
            boundary = " "
        )
        assertFalse(pending.matchesBeforeCursor("Please type the n"))
        assertFalse(pending.matchesBeforeCursor("Please type the"))
    }

    @Test
    fun punctuationBoundaryIsPartOfTheUndoMatch() {
        val pending = PendingAutocorrection(
            original = "teh",
            corrected = "the",
            boundary = "."
        )
        assertTrue(pending.matchesBeforeCursor("This is the."))
        assertFalse(pending.matchesBeforeCursor("This is the "))
    }
}
