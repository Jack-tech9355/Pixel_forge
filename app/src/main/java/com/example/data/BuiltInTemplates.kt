package com.example.data

import com.example.model.TemplateItem

object BuiltInTemplates {

    val ALL: List<TemplateItem> = listOf(
        TemplateItem(
            id = "sword_legend",
            title = "Blade of Legend",
            category = "Swords & Tools",
            width = 16,
            height = 16,
            description = "A mythical enchanted pixel broadsword with glowing cyan edges"
        ) { generateSword() },

        TemplateItem(
            id = "heart_8bit",
            title = "Retro Life Heart",
            category = "Hearts & Symbols",
            width = 16,
            height = 16,
            description = "Classic 8-bit glowing health heart with glossy highlight"
        ) { generateHeart() },

        TemplateItem(
            id = "potion_mana",
            title = "Mana Elixir",
            category = "Fantasy Characters",
            width = 16,
            height = 16,
            description = "Magic bubbling glass potion flask filled with celestial potion"
        ) { generatePotion() },

        TemplateItem(
            id = "game_shield",
            title = "Knight's Crest Shield",
            category = "Gaming Icons",
            width = 16,
            height = 16,
            description = "Sturdy steel and gold heraldic shield for retro RPGs"
        ) { generateShield() },

        TemplateItem(
            id = "pixel_mushroom",
            title = "Super Mushroom",
            category = "Food",
            width = 16,
            height = 16,
            description = "Spotted 16-bit red mushroom with glossy cap"
        ) { generateMushroom() },

        TemplateItem(
            id = "pixel_slime",
            title = "Dungeon Slime",
            category = "Fantasy Characters",
            width = 16,
            height = 16,
            description = "Bouncy green slime monster with cute expressive eyes"
        ) { generateSlime() },

        TemplateItem(
            id = "space_ufo",
            title = "Cosmic UFO",
            category = "Space",
            width = 16,
            height = 16,
            description = "Retro alien flying saucer beaming neon particles"
        ) { generateUfo() },

        TemplateItem(
            id = "game_tile_grass",
            title = "RPG Grass & Dirt Tile",
            category = "Game Tiles",
            width = 16,
            height = 16,
            description = "Seamless top-down RPG landscape dirt and grass cliff tile"
        ) { generateGrassTile() },

        TemplateItem(
            id = "pixel_cottage",
            title = "Pixel Forest Cottage",
            category = "Houses",
            width = 16,
            height = 16,
            description = "Cozy stone cabin with thatched roof and chimney smoke"
        ) { generateCottage() },

        TemplateItem(
            id = "pixel_cat",
            title = "Calico Kitty",
            category = "Animals",
            width = 16,
            height = 16,
            description = "Charming sleeping pixel kitten with pointed ears"
        ) { generateCat() },

        TemplateItem(
            id = "pixel_tree",
            title = "Ancient Oak",
            category = "Nature",
            width = 16,
            height = 16,
            description = "Lush leafy pixel tree with shaded foliage and sturdy trunk"
        ) { generateTree() },

        TemplateItem(
            id = "pixel_gem",
            title = "Electric Cyan Crystal",
            category = "Gaming Icons",
            width = 16,
            height = 16,
            description = "Faceted shining gemstone gleaming with pixel highlights"
        ) { generateCrystal() }
    )

    // Helper to build 16x16 grid easily from ASCII art or mapped chars
    private fun buildFromMap(
        width: Int,
        height: Int,
        paletteMap: Map<Char, Int>,
        art: List<String>
    ): IntArray {
        val pixels = IntArray(width * height)
        for (y in 0 until minOf(height, art.size)) {
            val line = art[y]
            for (x in 0 until minOf(width, line.length)) {
                val char = line[x]
                pixels[y * width + x] = paletteMap[char] ?: 0
            }
        }
        return pixels
    }

    private fun generateSword(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'B' to 0xFF22D3EE.toInt(), // Cyan blade
            'W' to 0xFFE0F2FE.toInt(), // Blade shine
            'D' to 0xFF0284C7.toInt(), // Blade shadow
            'G' to 0xFFFBBF24.toInt(), // Gold crossguard
            'R' to 0xFFB45309.toInt(), // Brown hilt
            'P' to 0xFF8B5CF6.toInt()  // Pommel gem
        )
        val art = listOf(
            "              BW",
            "             BWD",
            "            BWD ",
            "           BWD  ",
            "          BWD   ",
            "         BWD    ",
            "        BWD     ",
            "       BWD      ",
            "      BWD       ",
            "     BWD        ",
            "   GGGG         ",
            "  GGBG          ",
            "   G R          ",
            "    R           ",
            "   P            ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateHeart(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'O' to 0xFF881337.toInt(), // Outline dark red
            'R' to 0xFFE11D48.toInt(), // Red fill
            'W' to 0xFFFFFFFF.toInt(), // Shine
            'P' to 0xFFFDA4AF.toInt()  // Light red
        )
        val art = listOf(
            "                ",
            "                ",
            "   OOO    OOO   ",
            "  OWWRO  ORRRO  ",
            " ORWPPROORRRRRO ",
            " ORRRRRRRRRRRRO ",
            " ORRRRRRRRRRRRO ",
            "  ORRRRRRRRRRO  ",
            "  ORRRRRRRRRRO  ",
            "   ORRRRRRRRO   ",
            "    ORRRRRRO    ",
            "     ORRRRO     ",
            "      ORRO      ",
            "       OO       ",
            "                ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generatePotion(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'K' to 0xFF92400E.toInt(), // Cork
            'G' to 0xFFCBD5E1.toInt(), // Glass outline
            'L' to 0xFF8B5CF6.toInt(), // Purple potion
            'M' to 0xFFC084FC.toInt(), // Light purple
            'W' to 0xFFFFFFFF.toInt(), // Bubble & shine
            'S' to 0xFF581C87.toInt()  // Potion shadow
        )
        val art = listOf(
            "                ",
            "      KKKK      ",
            "      KKKK      ",
            "     GGGGGG     ",
            "      GGGG      ",
            "     GG  GG     ",
            "    GG    GG    ",
            "   GG W    GG   ",
            "  GG WWLL   GG  ",
            "  GG WLLLM  GG  ",
            "  GG LLLLM  GG  ",
            "  GG LLLLS  GG  ",
            "  GG SSSSS  GG  ",
            "   GGGGGGGGGG   ",
            "                ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateShield(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'G' to 0xFFF59E0B.toInt(), // Gold rim
            'S' to 0xFF3B82F6.toInt(), // Blue shield
            'L' to 0xFF60A5FA.toInt(), // Light blue
            'W' to 0xFFFFFFFF.toInt(), // White cross
            'D' to 0xFF1D4ED8.toInt()  // Dark blue
        )
        val art = listOf(
            "                ",
            " GGGGGGGGGGGGGG ",
            " GLLLLWWLLLLLLG ",
            " GLLLLWWLLLLLLG ",
            " GLLLLWWLLLLLLG ",
            " GWWWWWWWWWWWWG ",
            " GWWWWWWWWWWWWG ",
            " GSSSSWWSSSSSSG ",
            "  GSSSWWSSSSSG  ",
            "  GSSSWWSSSSSG  ",
            "   GDDWWDDDDG   ",
            "   GDDWWDDDDG   ",
            "    GDDDDDDG    ",
            "     GDDDDG     ",
            "      GGGG      ",
            "       GG       "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateMushroom(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'O' to 0xFF1E293B.toInt(), // Outline
            'R' to 0xFFDC2626.toInt(), // Red
            'W' to 0xFFFFFFFF.toInt(), // White spot
            'S' to 0xFFFEF08A.toInt(), // Skin
            'E' to 0xFF1E293B.toInt()  // Eyes
        )
        val art = listOf(
            "     OOOOOO     ",
            "   OORRRRRROO   ",
            "  ORRRWWWRRRRO  ",
            " ORRRWWWWWWRRRO ",
            " ORWWWWWWWWWRRO ",
            "ORRWWWWWWWWWWRRO",
            "ORRRWWWWWWWWRRRO",
            "ORRRRWWWWWRRRRRO",
            " OOOOOOOOOOOOOO ",
            "   OSSSSSSSSO   ",
            "   OSEESSEESO   ",
            "   OSEESSEESO   ",
            "   OSSSSSSSSO   ",
            "   OSSSSSSSSO   ",
            "    OOOOOOOO    ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateSlime(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'O' to 0xFF064E3B.toInt(), // Dark outline
            'G' to 0xFF10B981.toInt(), // Green body
            'L' to 0xFF6EE7B7.toInt(), // Highlight
            'W' to 0xFFFFFFFF.toInt(), // Eye shine
            'E' to 0xFF022C22.toInt()  // Eye pupil
        )
        val art = listOf(
            "                ",
            "                ",
            "       OO       ",
            "      OGGO      ",
            "     OGGGGO     ",
            "    OGLLGGGGO   ",
            "   OGLLGGGGGGO  ",
            "  OGGGGGGGGGGGO ",
            "  OG WE GG WE GO",
            " OGG EE GG EE GGO",
            " OGGGGGGGGGGGGGGO",
            " OGGGGGGGGGGGGGGO",
            " OGGGGGGGGGGGGGGO",
            "  OOOOOOOOOOOOOO ",
            "                ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateUfo(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'D' to 0xFF38BDF8.toInt(), // Dome cyan
            'W' to 0xFFE0F2FE.toInt(), // Shine
            'M' to 0xFF94A3B8.toInt(), // Saucer metal
            'L' to 0xFFFACC15.toInt(), // Glow lights
            'B' to 0xFF818CF8.toInt()  // Beam
        )
        val art = listOf(
            "                ",
            "      WWDD      ",
            "     WDDDDD     ",
            "    WDDDDDDD    ",
            "   MMMMMMMMMM   ",
            "  MMMMMMMMMMMM  ",
            " MMMMLMMLMMLMMMM",
            "  MMMMMMMMMMMM  ",
            "   MMMMMMMMMM   ",
            "     BBBBBB     ",
            "    BBBBBBBB    ",
            "   BBBBBBBBBB   ",
            "  BBBBBBBBBBBB  ",
            " BBBBBBBBBBBBBB ",
            "                ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateGrassTile(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'G' to 0xFF22C55E.toInt(), // Grass top
            'L' to 0xFF86EFAC.toInt(), // Grass light
            'D' to 0xFF78350F.toInt(), // Dirt
            'S' to 0xFF92400E.toInt(), // Dirt light
            'R' to 0xFF451A03.toInt()  // Rock/shadow
        )
        val art = listOf(
            "LLGGLLGGGGLLGGLL",
            "GGGGGGGGGGGGGGGG",
            "GGLLGGGGGGLLGGGG",
            " G  G  G   G  G ",
            "DDDDDDDDDDDDDDDD",
            "DDSSDDDDDDSSDDDD",
            "DDDDDRRDDDDDDDDR",
            "DDDDDDDDDDDDDDDD",
            "SSDDDDDDSSDDDDDD",
            "DDDDDDRRDDDDDDDD",
            "DDDDDDDDDDDDSSDD",
            "DDSSDDDDDDDDDDDD",
            "DDDDDDDDRRDDDDDD",
            "RDDDDDDDDDDDDDDR",
            "DDDDSSDDDDDDSSDD",
            "DDDDDDDDDDDDDDDD"
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateCottage(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'R' to 0xFF991B1B.toInt(), // Roof red
            'B' to 0xFFD97706.toInt(), // Wood wall
            'D' to 0xFF451A03.toInt(), // Door
            'W' to 0xFF67E8F9.toInt(), // Window cyan
            'S' to 0xFF94A3B8.toInt()  // Smoke / stone
        )
        val art = listOf(
            "         SS     ",
            "        SS      ",
            "       RR       ",
            "      RRRR      ",
            "     RRRRRR     ",
            "    RRRRRRRR    ",
            "   RRRRRRRRRR   ",
            "  RRRRRRRRRRRR  ",
            "   BBBBBBBBBB   ",
            "   BBWBBBBWBB   ",
            "   BBWBBBBWBB   ",
            "   BBBBBDDBBB   ",
            "   BBBBBDDBBB   ",
            "   SSSSSSSSSS   ",
            "                ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateCat(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'O' to 0xFFEA580C.toInt(), // Orange fur
            'W' to 0xFFFFFFFF.toInt(), // White fur
            'B' to 0xFF18181B.toInt(), // Eyes / nose
            'P' to 0xFFF472B6.toInt()  // Pink inner ear
        )
        val art = listOf(
            "                ",
            "                ",
            "  OO        OO  ",
            "  OPO      OPO  ",
            "  OOOOOOOOOOOO  ",
            " OOOOOOOOOOOOOO ",
            " OOWOOOOOOOOWOO ",
            " OOBWOOOOOOWBOO ",
            " OOOOOOBBOOOOOO ",
            "  OOOOOOOOOOOO  ",
            "   OWWWWWWWWO   ",
            "  OOWWWWWWWWOO  ",
            "  OOOOOOOOOOOO  ",
            "   OOOOOOOOOO   ",
            "                ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateTree(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'L' to 0xFF4ADE80.toInt(), // Light leaf
            'G' to 0xFF16A34A.toInt(), // Medium leaf
            'D' to 0xFF14532D.toInt(), // Dark leaf
            'B' to 0xFF78350F.toInt(), // Bark
            'S' to 0xFF451A03.toInt()  // Bark shadow
        )
        val art = listOf(
            "     LLGGGG     ",
            "   LLLLGGGGGG   ",
            "  LLLLGGGGGGDD  ",
            " LLLLGGGGGGDDDD ",
            " LLLGGGGGGDDDDD ",
            " LLLGGGGGGDDDDD ",
            "  LLGGGGGGDDDD  ",
            "   GGGGGGDDDD   ",
            "     BBSSBB     ",
            "     BBSSBB     ",
            "     BBSSBB     ",
            "     BBSSBB     ",
            "    BBBSSBBB    ",
            "   BBBBSSBBBB   ",
            "                ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }

    private fun generateCrystal(): IntArray {
        val map = mapOf(
            ' ' to 0,
            'W' to 0xFFE0F2FE.toInt(), // White highlight
            'C' to 0xFF22D3EE.toInt(), // Cyan
            'M' to 0xFF0891B2.toInt(), // Medium cyan
            'D' to 0xFF164E63.toInt()  // Deep cyan
        )
        val art = listOf(
            "       WW       ",
            "      WCCM      ",
            "     WCCCCM     ",
            "    WCCCCCCM    ",
            "   WCCCCCCCCD   ",
            "  WCCCCCCCCCCD  ",
            "   MCCCCCCCCD   ",
            "   MMCCCCCCDD   ",
            "    MMCCCCDD    ",
            "     MMCCDD     ",
            "      MMCD      ",
            "       MD       ",
            "                ",
            "                ",
            "                ",
            "                "
        )
        return buildFromMap(16, 16, map, art)
    }
}
