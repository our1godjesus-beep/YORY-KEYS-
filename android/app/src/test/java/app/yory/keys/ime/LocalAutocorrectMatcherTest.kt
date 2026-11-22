package app.yory.keys.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalAutocorrectMatcherTest {
    private val normalizeArabic: (String) -> String = YorySuggestionText::normalizeForMatch

    @Test
    fun appliesCuratedEnglishAndArabicCorrections() {
        assertEquals("the", LocalAutocorrectMatcher.correct("teh", "en-US", emptyList()))
        assertEquals("لكن", LocalAutocorrectMatcher.correct("لاكن", "ar", emptyList()))
    }

    @Test
    fun preservesEnglishCapitalization() {
        assertEquals("The", LocalAutocorrectMatcher.correct("Teh", "en-US", emptyList()))
        assertEquals("THE", LocalAutocorrectMatcher.correct("TEH", "en-US", emptyList()))
    }

    @Test
    fun correctsOnlyAUniqueSingleEditCandidate() {
        assertEquals(
            "hello",
            LocalAutocorrectMatcher.correct("hlelo", "en-US", listOf("hello"))
        )
        assertEquals(
            "Hello",
            LocalAutocorrectMatcher.correct("Hlelo", "en-US", listOf("hello"))
        )
    }

    @Test
    fun leavesAmbiguousWordsUntouched() {
        assertNull(LocalAutocorrectMatcher.correct("dat", "en-US", listOf("cat", "dot")))
    }

    @Test
    fun leavesExistingDictionaryWordsUntouched() {
        assertNull(LocalAutocorrectMatcher.correct("hello", "en-US", listOf("hello", "help")))
    }

    @Test
    fun usesArabicSearchNormalizationWithoutChangingCandidateText() {
        assertNull(
            LocalAutocorrectMatcher.correct(
                "احتاج",
                "ar",
                listOf("أحتاج"),
                normalizeArabic
            )
        )
        assertEquals(
            "إعداد",
            LocalAutocorrectMatcher.correct(
                "اعدا",
                "ar",
                listOf("إعداد"),
                normalizeArabic
            )
        )
    }

    @Test
    fun ignoresWordsOutsideSupportedLengthAndUnknownLanguages() {
        assertNull(LocalAutocorrectMatcher.correct("ab", "en-US", listOf("abc")))
        assertNull(LocalAutocorrectMatcher.correct("xyz", "fr", listOf("abc", "def")))
    }
}
