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
    fun defaultThemeIsTintedGlass() {
        assertEquals("Tinted Glass", YoryTheme.default.name)
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
            hapticsEnabled = true
        )
        assertEquals(original, YoryThemeFile.decode(YoryThemeFile.encode(original)))
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
