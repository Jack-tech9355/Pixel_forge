package com.example.data

import com.example.model.PixelPalette

object BuiltInPalettes {

    val CLASSIC_8BIT = PixelPalette(
        id = "classic_8bit",
        name = "Classic 8-Bit",
        colors = listOf(
            0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF888888.toInt(), 0xFFC0C0C0.toInt(),
            0xFFFF0000.toInt(), 0xFF880000.toInt(), 0xFFFFFF00.toInt(), 0xFF888800.toInt(),
            0xFF00FF00.toInt(), 0xFF008800.toInt(), 0xFF00FFFF.toInt(), 0xFF008888.toInt(),
            0xFF0000FF.toInt(), 0xFF000088.toInt(), 0xFFFF00FF.toInt(), 0xFF880088.toInt()
        )
    )

    val GAMEBOY = PixelPalette(
        id = "gameboy",
        name = "Game Boy DMG",
        colors = listOf(
            0xFF0F380F.toInt(), // Darkest green
            0xFF306230.toInt(), // Dark green
            0xFF8BAC0F.toInt(), // Light green
            0xFF9BBC0F.toInt()  // Pale yellow-green
        )
    )

    val CYBER_NEON = PixelPalette(
        id = "cyber_neon",
        name = "Cyberpunk Neon",
        colors = listOf(
            0xFF080B18.toInt(), 0xFF19233B.toInt(), 0xFF22D3EE.toInt(), 0xFF06B6D4.toInt(),
            0xFF8B5CF6.toInt(), 0xFFA855F7.toInt(), 0xFFEC4899.toInt(), 0xFFF43F5E.toInt(),
            0xFFF59E0B.toInt(), 0xFFEAB308.toInt(), 0xFF10B981.toInt(), 0xFF34D399.toInt(),
            0xFF38BDF8.toInt(), 0xFFF87171.toInt(), 0xFFE2E8F0.toInt(), 0xFFFFFFFF.toInt()
        )
    )

    val PASTEL_DREAMS = PixelPalette(
        id = "pastel_dreams",
        name = "Pastel Dreams",
        colors = listOf(
            0xFFFFB5E8.toInt(), 0xFFFFC9DE.toInt(), 0xFFFFDDE4.toInt(), 0xFFC5A3FF.toInt(),
            0xFFB28DFF.toInt(), 0xFFAFEEFF.toInt(), 0xFFBFFCC6.toInt(), 0xFFD5AAFF.toInt(),
            0xFFFFC6FF.toInt(), 0xFFF3C4FB.toInt(), 0xFFFFFFD1.toInt(), 0xFFE7FFAC.toInt(),
            0xFF4A4E69.toInt(), 0xFF22223B.toInt(), 0xFF9A8C98.toInt(), 0xFFF2E9E4.toInt()
        )
    )

    val EARTH_TONES = PixelPalette(
        id = "earth_tones",
        name = "Earth Tones",
        colors = listOf(
            0xFF2B1D0C.toInt(), 0xFF4A3525.toInt(), 0xFF6F4E37.toInt(), 0xFF967140.toInt(),
            0xFFBFA054.toInt(), 0xFFDFD195.toInt(), 0xFF2D3E20.toInt(), 0xFF465B2B.toInt(),
            0xFF6B8C42.toInt(), 0xFF99B858.toInt(), 0xFF3F3A37.toInt(), 0xFF655D57.toInt(),
            0xFF91867E.toInt(), 0xFFC1B5AB.toInt(), 0xFFEFE8DF.toInt(), 0xFFFFFFFF.toInt()
        )
    )

    val FANTASY_RPG = PixelPalette(
        id = "fantasy_rpg",
        name = "Fantasy RPG",
        colors = listOf(
            0xFF1A1016.toInt(), 0xFF271B24.toInt(), 0xFF3D253B.toInt(), 0xFF854C30.toInt(),
            0xFFD18B47.toInt(), 0xFFFFC36B.toInt(), 0xFFE2442D.toInt(), 0xFF9B1032.toInt(),
            0xFF265C42.toInt(), 0xFF438A5E.toInt(), 0xFF73C365.toInt(), 0xFF1B3B6F.toInt(),
            0xFF21689C.toInt(), 0xFF52A8DB.toInt(), 0xFFEFE6DD.toInt(), 0xFFFFFFFF.toInt()
        )
    )

    val SUNSET_GLOW = PixelPalette(
        id = "sunset_glow",
        name = "Sunset Glow",
        colors = listOf(
            0xFF1B143F.toInt(), 0xFF392257.toInt(), 0xFF632B6C.toInt(), 0xFF983275.toInt(),
            0xFFC93D64.toInt(), 0xFFEC554E.toInt(), 0xFFFA7F4B.toInt(), 0xFFFFB053.toInt(),
            0xFFFFD770.toInt(), 0xFFFFF3B0.toInt(), 0xFF140D2B.toInt(), 0xFF291B48.toInt(),
            0xFF4A2A5E.toInt(), 0xFF823C70.toInt(), 0xFFCC6778.toInt(), 0xFFFEE180.toInt()
        )
    )

    val GRAYSCALE = PixelPalette(
        id = "grayscale",
        name = "Monochrome",
        colors = listOf(
            0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF333333.toInt(), 0xFF4D4D4D.toInt(),
            0xFF666666.toInt(), 0xFF808080.toInt(), 0xFF999999.toInt(), 0xFFB3B3B3.toInt(),
            0xFFCCCCCC.toInt(), 0xFFE6E6E6.toInt(), 0xFFF2F2F2.toInt(), 0xFFFFFFFF.toInt()
        )
    )

    val PICO_8 = PixelPalette(
        id = "pico_8",
        name = "PICO-8",
        colors = listOf(
            0xFF000000.toInt(), 0xFF1D2B53.toInt(), 0xFF7E2553.toInt(), 0xFF008751.toInt(),
            0xFFAB5236.toInt(), 0xFF5F574F.toInt(), 0xFFC2C3C7.toInt(), 0xFFFFF1E8.toInt(),
            0xFFFF004D.toInt(), 0xFFFFA300.toInt(), 0xFFFFEC27.toInt(), 0xFF00E436.toInt(),
            0xFF29ADFF.toInt(), 0xFF83769C.toInt(), 0xFFFF77A8.toInt(), 0xFFFFCCAA.toInt()
        )
    )

    val ALL_PRESETS = listOf(
        CYBER_NEON,
        CLASSIC_8BIT,
        GAMEBOY,
        PICO_8,
        FANTASY_RPG,
        SUNSET_GLOW,
        PASTEL_DREAMS,
        EARTH_TONES,
        GRAYSCALE
    )

    fun getById(id: String): PixelPalette {
        return ALL_PRESETS.find { it.id == id } ?: CYBER_NEON
    }
}
