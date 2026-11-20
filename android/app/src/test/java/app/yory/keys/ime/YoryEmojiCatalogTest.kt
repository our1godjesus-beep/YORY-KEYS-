package app.yory.keys.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoryEmojiCatalogTest {
    @Test
    fun allCategoriesHaveUniqueStableIds() {
        val ids = YoryEmojiCatalog.categories.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.containsAll(listOf("Recent", "Favorites", "Smile", "People", "Nature", "Food", "Travel", "Symbols")))
    }

    @Test
    fun recentItemsMoveToFrontAndAreBounded() {
        val input = (1..30).map { "emoji-$it" }
        val updated = YoryEmojiCatalog.addRecent(input, "emoji-5")
        assertEquals("emoji-5", updated.first())
        assertEquals(24, updated.size)
        assertEquals(1, updated.count { it == "emoji-5" })
    }

    @Test
    fun favoritesToggleWithoutDuplicates() {
        val added = YoryEmojiCatalog.toggleFavorite(emptyList(), "🔥")
        assertEquals(listOf("🔥"), added)
        assertEquals(emptyList<String>(), YoryEmojiCatalog.toggleFavorite(added, "🔥"))
        assertEquals(listOf("🔥", "❤️"), YoryEmojiCatalog.toggleFavorite(listOf("❤️"), "🔥"))
    }

    @Test
    fun storedValuesAreTrimmedDistinctAndBounded() {
        assertEquals(listOf("😀", "❤️"), YoryEmojiCatalog.parseStored("😀\n 😀 \n\n❤️"))
        assertEquals("😀\n❤️", YoryEmojiCatalog.encodeStored(listOf("😀", "😀", "❤️"), 8))
    }

    @Test
    fun skinToneModifierFollowsFirstUnicodeScalar() {
        assertEquals("👍🏽", YoryEmojiCatalog.applySkinTone("👍", "🏽"))
        assertEquals("😀", YoryEmojiCatalog.applySkinTone("😀", ""))
        assertEquals("", YoryEmojiCatalog.applySkinTone("", "🏽"))
    }

    @Test
    fun favoritesCategoryReturnsOnlyFavorites() {
        assertEquals(listOf("❤️"), YoryEmojiCatalog.values("Favorites", listOf("😀"), listOf("❤️")))
        assertTrue(YoryEmojiCatalog.values("Favorites", emptyList(), emptyList()).isEmpty())
    }
}
