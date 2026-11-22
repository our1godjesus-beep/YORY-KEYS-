package app.yory.keys.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoryKeyLegendsTest {
    @Test
    fun arabicRowsExposeEasternAndWesternDigitsAndShortcuts() {
        val first = YoryKeyLegends.forRow("ar", 0, "ضصثقفغعهخحجد")
        assertEquals("1 ١", first["ض"])
        assertEquals("0 ٠", first["ح"])
        assertEquals("-", first["ج"])
        assertEquals("=", first["د"])

        val second = YoryKeyLegends.forRow("ar", 1, "شسيبلاتنمكذ")
        assertEquals("@", second["ش"])
        assertEquals("لا", second["ل"])
    }

    @Test
    fun latinRowsUsePhysicalPositionsForQwertzAndAzerty() {
        val qwertz = YoryKeyLegends.forRow("de", 0, "qwertzuiop")
        assertEquals("1", qwertz["q"])
        assertEquals("6", qwertz["z"])
        assertEquals("0", qwertz["p"])

        val azerty = YoryKeyLegends.forRow("fr", 0, "azertyuiop")
        assertEquals("1", azerty["a"])
        assertEquals("2", azerty["z"])
        assertEquals("3", azerty["e"])
    }

    @Test
    fun longPressOptionsIncludeLegendsAndLanguageAccentsWithoutDuplicates() {
        val options = YoryKeyLegends.optionsForKey(
            key = "a",
            secondaryLegend = "@",
            languageTag = "de",
            showSymbols = true,
            showAccents = true
        )
        assertTrue(options.contains("@"))
        assertTrue(options.contains("ä"))
        assertEquals(options.size, options.distinct().size)
    }

    @Test
    fun disabledPreferencesDoNotExposeAlternates() {
        assertTrue(
            YoryKeyLegends.optionsForKey(
                key = "a",
                secondaryLegend = "@",
                languageTag = "en-US",
                showSymbols = false,
                showAccents = false
            ).isEmpty()
        )
    }
}
