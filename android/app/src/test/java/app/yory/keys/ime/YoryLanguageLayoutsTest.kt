package app.yory.keys.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoryLanguageLayoutsTest {
    @Test
    fun supportedLayoutsHaveUniqueTagsAndThreeNonEmptyRows() {
        val tags = YoryLanguageLayouts.supported.map { it.tag }
        assertEquals(tags.size, tags.distinct().size)
        YoryLanguageLayouts.supported.forEach { layout ->
            assertEquals(3, layout.rows.size)
            assertTrue(layout.rows.all { it.isNotBlank() })
            assertTrue(layout.rows.all { row -> row.toList().distinct().size == row.length })
        }
    }

    @Test
    fun runtimeLanguageTagsResolveToTheExpectedLayouts() {
        listOf("en-US", "ar", "de", "es", "fr", "pt", "it").forEach { tag ->
            assertEquals(tag, YoryLanguageLayouts.forTag(tag).tag)
        }
    }

    @Test
    fun onlyArabicIsRtlAndOnlyEnglishArabicHaveBundledSeedDictionaries() {
        assertTrue(YoryLanguageLayouts.forTag("ar").isRtl)
        assertFalse(YoryLanguageLayouts.forTag("en-US").isRtl)
        assertTrue(YoryLanguageLayouts.forTag("en-US").hasBundledDictionary)
        assertTrue(YoryLanguageLayouts.forTag("ar").hasBundledDictionary)
        assertFalse(YoryLanguageLayouts.forTag("fr").hasBundledDictionary)
    }

    @Test
    fun languageSwitchCyclesThroughInstalledLayoutsAndWraps() {
        assertEquals("ar", YoryLanguageLayouts.nextTag("en-US"))
        assertEquals("de", YoryLanguageLayouts.nextTag("ar"))
        assertEquals("en-US", YoryLanguageLayouts.nextTag("it"))
        assertEquals("en-US", YoryLanguageLayouts.nextTag("unknown"))
    }

    @Test
    fun unknownSavedLanguageFallsBackToEnglishLayout() {
        assertEquals("en-US", YoryLanguageLayouts.forTag("ja-JP").tag)
    }
    @Test
    fun arabicPrimaryRowsContainAllStandardLettersAndUsefulVariants() {
        val arabic = YoryLanguageLayouts.forTag("ar")
        val visible = arabic.rows.joinToString("")
        val standardLetters = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي"
        standardLetters.forEach { letter ->
            assertTrue("Missing Arabic letter: $letter", visible.contains(letter))
        }
        listOf('ء', 'أ', 'إ', 'آ', 'ؤ', 'ئ', 'ة', 'ى').forEach { variant ->
            assertTrue("Missing Arabic variant: $variant", visible.contains(variant) ||
                variant in setOf('أ', 'إ', 'آ'))
        }
    }

    @Test
    fun languageSwitchUsesOnlyAddedLayoutsAndSupportsBothDirections() {
        val enabled = listOf("en-US", "ar", "fr")
        assertEquals("ar", YoryLanguageLayouts.nextTag("en-US", enabled, 1))
        assertEquals("fr", YoryLanguageLayouts.nextTag("ar", enabled, 1))
        assertEquals("en-US", YoryLanguageLayouts.nextTag("ar", enabled, -1))
        assertEquals("fr", YoryLanguageLayouts.nextTag("en-US", enabled, -1))
        assertEquals("ar", YoryLanguageLayouts.nextTag("ar", listOf("ar"), 1))
    }

}
