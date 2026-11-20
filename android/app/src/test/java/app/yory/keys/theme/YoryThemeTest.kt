package app.yory.keys.theme

import org.junit.Assert.assertEquals
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
}
