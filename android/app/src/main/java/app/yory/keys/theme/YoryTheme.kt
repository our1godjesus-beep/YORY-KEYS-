package app.yory.keys.theme

data class YoryTheme(
    val name: String,
    val backgroundArgb: Int,
    val keyArgb: Int,
    val textArgb: Int,
    val accentArgb: Int,
    val opacity: Float = 0.92f,
    val cornerRadiusDp: Float = 13f,
    val keyHeightDp: Float = 40f,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val clipboardEnabled: Boolean = false,
    val keyPressedArgb: Int = accentArgb,
    val keySpacingDp: Float = 4f,
    val keyShape: String = "rounded",
    val fontSizeSp: Float = 18f,
    val fontFamily: String = "system"
) {
    companion object {
        val presets = listOf(
            YoryTheme("Obsidian Luxe", 0xFF141414.toInt(), 0xFF252525.toInt(), 0xFFF7F7F7.toInt(), 0xFFB9B9B9.toInt(), 1.0f, 12f, 46f, true, true, false, 0xFF626262.toInt(), 4f, "squircle", 18f, "system"),
            YoryTheme("Tinted Glass", 0xD91A1D2B.toInt(), 0xE8F1F3FA.toInt(), 0xFF171923.toInt(), 0xFF78B8F8.toInt(), 0.92f, 14f, 46f),
            YoryTheme("Midnight", 0xEF10121B.toInt(), 0xFF292D3B.toInt(), 0xFFF6F7FC.toInt(), 0xFF9B8CFF.toInt(), 0.98f, 12f, 46f),
            YoryTheme("Arctic", 0xD9DDE7F5.toInt(), 0xF7FFFFFF.toInt(), 0xFF202432.toInt(), 0xFF5598DB.toInt(), 0.88f, 16f, 47f),
            YoryTheme("Neon", 0xE8171928.toInt(), 0xFF252B3B.toInt(), 0xFFF7F8FF.toInt(), 0xFF51E5DC.toInt(), 0.95f, 11f, 46f),
            YoryTheme("Rose", 0xD9F3DDE5.toInt(), 0xF7FFFFFF.toInt(), 0xFF32212B.toInt(), 0xFFDE7297.toInt(), 0.90f, 18f, 47f)
        )
        val default = presets.first()
    }
}
