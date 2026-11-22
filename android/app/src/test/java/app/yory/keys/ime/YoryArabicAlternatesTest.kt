package app.yory.keys.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoryArabicAlternatesTest {
    @Test
    fun exposesTheFullCommonTashkeelSetOnEveryArabicLetter() {
        val expected = setOf("َ", "ُ", "ِ", "ْ", "ّ", "ً", "ٌ", "ٍ", "ٰ", "ٓ")
        assertEquals(expected, YoryArabicAlternates.tashkeelMarks.toSet())
        listOf("ا", "ب", "ت", "ي", "م").forEach { key ->
            assertTrue(YoryArabicAlternates.optionsForKey(key).containsAll(expected))
        }
    }

    @Test
    fun keepsHamzaAndAlifVariantsAvailableAlongsideTashkeel() {
        val alifOptions = YoryArabicAlternates.optionsForKey("ا")
        assertTrue(alifOptions.containsAll(listOf("أ", "إ", "آ", "ٱ")))
        assertTrue(YoryArabicAlternates.optionsForKey("و").contains("ؤ"))
        assertTrue(YoryArabicAlternates.optionsForKey("ي").containsAll(listOf("ئ", "ى")))
        assertTrue(YoryArabicAlternates.optionsForKey("ه").contains("ة"))
    }

    @Test
    fun diacriticsHaveVisibleAndAccessiblePreviewLabels() {
        assertTrue(YoryArabicAlternates.isTashkeel("َ"))
        assertEquals("◌َ", YoryArabicAlternates.displayLabel("َ"))
        assertEquals("فتحة (Fatha)", YoryArabicAlternates.accessibilityLabel("َ"))
        assertEquals("أ", YoryArabicAlternates.displayLabel("أ"))
    }
}
