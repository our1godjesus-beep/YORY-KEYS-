package app.yory.keys.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class YoryThemeTest {
    @Test
    fun presetsHaveDistinctNames() {
        val names = YoryTheme.presets.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun presetGeometryStaysWithinStudioBounds() {
        YoryTheme.presets.forEach { theme ->
            assertTrue(theme.cornerRadiusDp in 4f..28f)
            assertTrue(theme.keyHeightDp in 36f..60f)
            assertTrue(theme.opacity in 0.35f..1f)
        }
    }

    @Test
    fun defaultThemeIsObsidianLuxe() {
        assertEquals("Obsidian Luxe", YoryTheme.default.name)
        assertEquals(0xFF090A0C.toInt(), YoryTheme.default.backgroundArgb)
        assertEquals(0xFF202328.toInt(), YoryTheme.default.keyArgb)
        assertEquals(0xFFF2EEE7.toInt(), YoryTheme.default.textArgb)
        assertEquals(0xFFC9AE80.toInt(), YoryTheme.default.accentArgb)
    }

    @Test
    fun themeFileRoundTrips() {
        val original = YoryTheme(
            name = "My Custom Theme",
            backgroundArgb = 0xDD112233.toInt(),
            keyArgb = 0xFFEEEEEE.toInt(),
            textArgb = 0xFF101010.toInt(),
            accentArgb = 0xFF77AAFF.toInt(),
            opacity = 0.73f,
            cornerRadiusDp = 17f,
            keyHeightDp = 52f,
            soundEnabled = false,
            hapticsEnabled = true,
            keyPressedArgb = 0xFF223344.toInt(),
            keySpacingDp = 6f,
            keyShape = "pill",
            fontSizeSp = 20f
        )
        val encoded = YoryThemeFile.encode(original)
        assertTrue(encoded.contains("#112233DD"))
        assertEquals(original, YoryThemeFile.decode(encoded))
    }

    @Test
    fun themeFileRejectsUnsupportedVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            YoryThemeFile.decode("""{"schemaVersion":99,"name":"Bad","appearance":{}}""")
        }
    }

    @Test
    fun themeFileRejectsInvalidColors() {
        val invalid = """
            {
              "schemaVersion": 1,
              "name": "Broken",
              "appearance": {
                "background": "not-a-color",
                "key": "#FFFFFFFF",
                "text": "#FF000000",
                "accent": "#FF00FF00"
              }
            }
        """.trimIndent()
        assertThrows(IllegalArgumentException::class.java) {
            YoryThemeFile.decode(invalid)
        }
    }
}
