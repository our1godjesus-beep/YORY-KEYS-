package app.yory.keys.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YorySuggestionTextTest {
    @Test
    fun arabicAlefVariantsMatchWithoutChangingInsertedCandidate() {
        assertTrue(YorySuggestionText.matchesPrefix("أحتاج", "احت"))
        assertTrue(YorySuggestionText.matchesPrefix("إعداد", "اعد"))
        assertTrue(YorySuggestionText.matchesPrefix("آمن", "ام"))
        assertEquals("أحتاج", YorySuggestionText.rankByTypedPrefix(listOf("أحتاج"), "ا").first())
    }

    @Test
    fun arabicTashkeelAndTatweelAreIgnoredOnlyForLookup() {
        assertTrue(YorySuggestionText.matchesPrefix("مُدَرِّس", "مدر"))
        assertTrue(YorySuggestionText.matchesPrefix("الـلغة", "الل"))
        assertFalse(YorySuggestionText.matchesPrefix("مُدَرِّس", "مطب"))
    }

    @Test
    fun distinctArabicLettersAreNotFoldedTogether() {
        assertFalse(YorySuggestionText.matchesPrefix("مدرسة", "مدرسه"))
        assertFalse(YorySuggestionText.matchesPrefix("على", "علي"))
        assertFalse(YorySuggestionText.matchesPrefix("بداية", "م"))
    }

    @Test
    fun exactTypedPrefixIsRankedBeforeNormalizedMatches() {
        val ranked = YorySuggestionText.rankByTypedPrefix(
            listOf("أحتاج", "الآن", "ازيك", "إعداد"),
            "ا"
        )
        assertEquals(listOf("الآن", "ازيك", "أحتاج", "إعداد"), ranked)
    }

    @Test
    fun candidatesAreDeduplicatedWithoutChangingOriginalSpelling() {
        val ranked = YorySuggestionText.rankByTypedPrefix(
            listOf("YORY", "yory", "أحتاج"),
            "y"
        )
        assertEquals(listOf("YORY"), ranked)
    }
}
