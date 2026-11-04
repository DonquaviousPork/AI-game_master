package gamemaster.gui;

import java.awt.Color;
import java.util.Locale;
import java.util.Map;

/**
 * Pixel-art sprites drawn in code, one 12 by 14 grid per look. Each character in a grid names a colour:
 * H hat/hood/helmet, h its highlight, S skin, E eyes, C clothing, c dark clothing, W metal or bone,
 * B brown (wood, boots, hair), G gold, and '.' for empty. Heroes get a palette chosen from their name,
 * so two fighters do not look identical.
 */
final class Sprites {

    static final int WIDTH = 12;
    static final int HEIGHT = 14;

    static final String FIGHTER = "Fighter";
    static final String ROGUE = "Rogue";
    static final String WIZARD = "Wizard";
    static final String ADVENTURER = "Adventurer";
    static final String SOLDIER = "Soldier";
    static final String BEAST = "Beast";
    static final String UNDEAD = "Undead";
    static final String ROBED = "Robed";

    private static final Map<String, String[]> GRIDS = Map.of(
            FIGHTER, new String[] {
                "....hhhh....", "...HHHHHH...", "...HSSSSH...", "...SESSES...", "....SSSS....",
                "..CCCCCCCC..", "W.CCCCCCCCGG", "W.CCCCCCCCGG", "WSCCCCCCCSGG", "...CCCCCC...",
                "...cc..cc...", "...cc..cc...", "...BB..BB...", "..BBB..BBB.."},
            ROGUE, new String[] {
                "....HHHH....", "...HHHHHH...", "...HSSSSH...", "...HSESEH...", "...HHSSHH...",
                "..HHCCCCHH..", "..HCCCCCCH.W", "..HCCCCCCHSW", "...CCCCCC.G.", "...cCCCCc...",
                "...cc..cc...", "...cc..cc...", "...BB..BB...", "..BBB..BBB.."},
            WIZARD, new String[] {
                ".....HH....G", "....HHHH...B", "....HHHH...B", "...HHHHHH..B", "..HHHHHHHH.B",
                "...HSSSSH..B", "...SESSES..B", "...SWWWWS..B", "..CCWWWWCCSB", "..CCCCCCCC.B",
                "..CCCCCCCC.B", "..CCCCCCCC.B", "..cCCCCCCc.B", "..cccccccc.."},
            ADVENTURER, new String[] {
                "...BBBBBB...", "..BBBBBBBB..", "..BSSSSSSB..", "..BSESSESB..", "...SSSSSS...",
                "...CCCCCC...", "..CCCCCCCC..", "..SCCCCCCS..", "..SCCCCCCS..", "...CCCCCC...",
                "...cc..cc...", "...cc..cc...", "...BB..BB...", "..BBB..BBB.."},
            SOLDIER, new String[] {
                "...HHHHHH...", "..HHHHHHHH..", "..HHSSSSHH..", "..HHSESEHH..", "...HSSSSH...",
                "..CCCCCCCC.W", ".SCCCCCCCCSW", ".SCCCCCCCC.W", "..CCCCCCCC.G", "...CCCCCC...",
                "...cc..cc...", "...cc..cc...", "...BB..BB...", "..BBB..BBB.."},
            BEAST, new String[] {
                "............", "............", "..H......H..", "..HH....HH..", "..HHHHHHHH..",
                ".HHEHHHHEH..", ".HHHHHHHHHH.", ".HHHWHHWHHH.", "..HHHWWHHH..", "..HHHHHHHH..",
                ".HHHHHHHHHH.", ".HH.HHHH.HH.", ".HH......HH.", ".WW......WW."},
            UNDEAD, new String[] {
                "...WWWWWW...", "..WWWWWWWW..", "..WEEWWEEW..", "..WEEWWEEW..", "...WWWWWW...",
                "....W..W....", "..WWWWWWWW..", "....WWWW....", "..W.WWWW.W..", "..W.WWWW.W..",
                "....WWWW....", "...W.WW.W...", "...W....W...", "..WW....WW.."},
            ROBED, new String[] {
                "....HHHH....", "...HHHHHH...", "..HHHHHHHH..", "..HHEHHEHH..", "..HHHHHHHH..",
                "..HHCCCCHH..", ".HHCCCCCCHH.", ".HHCCCCCCHHG", ".HHHCCCCHHHB", ".HHHHHHHHHHB",
                ".HHHHHHHHHHB", ".HHHHHHHHHHB", ".HHHHHHHHHH.", ".cccccccccc."});

    private static final Color[] PRIMARY = {
        new Color(0x6C7A89), new Color(0x8D6BE0), new Color(0xC2474A),
        new Color(0x3FA796), new Color(0xD9A441), new Color(0x4C7BD9)};
    private static final Color[] CLOTHING = {
        new Color(0x4A6FA5), new Color(0x5E3FA8), new Color(0x7A3B3B),
        new Color(0x2F7D6B), new Color(0x8C6A2A), new Color(0x2E4E99)};
    private static final Color[] SKIN = {
        new Color(0xF1C9A5), new Color(0xD9A47A), new Color(0xA9714B), new Color(0x7B4B2E)};

    private static final Color METAL = new Color(0xC9D1D9);
    private static final Color BROWN = new Color(0x6B4A2F);
    private static final Color GOLD = new Color(0xE0B341);
    private static final Color DARK_EYES = new Color(0x15171A);

    private Sprites() {}

    static String[] grid(String look) {
        return GRIDS.getOrDefault(look, GRIDS.get(ADVENTURER));
    }

    static String lookForHero(String className) {
        return switch (className == null ? "" : className) {
            case FIGHTER, ROGUE, WIZARD -> className;
            default -> ADVENTURER;
        };
    }

    static String lookForEnemy(String name, String weapon) {
        String n = (name == null ? "" : name).toLowerCase(Locale.ROOT);
        if (hasAny(n, "skelet", "zombie", "ghoul", "ghost", "wraith", "lich", "undead", "bone", "spectre", "specter", "mummy")) {
            return UNDEAD;
        }
        if (hasAny(n, "wolf", "dog", "hound", "bear", "rat", "boar", "spider", "snake", "serpent", "drake",
                "dragon", "cat", "lion", "tiger", "bat", "beast", "jackal", "panther", "wyrm")) {
            return BEAST;
        }
        if (hasAny(n, "mage", "wizard", "witch", "cultist", "priest", "sorcer", "warlock", "necro", "shaman",
                "druid", "arcane", "caster", "monk")
                || (weapon != null && weapon.toLowerCase(Locale.ROOT).contains("staff"))) {
            return ROBED;
        }
        return SOLDIER;
    }

    private static boolean hasAny(String text, String... parts) {
        for (String p : parts) {
            if (text.contains(p)) {
                return true;
            }
        }
        return false;
    }

    /** The colour for one grid character, or null for an empty cell. Dead characters are drawn in grey. */
    static Color colour(char key, String look, String seed, boolean alive) {
        Color c = raw(key, look, seed);
        if (c == null || alive) {
            return c;
        }
        int grey = (int) Math.round((0.3 * c.getRed() + 0.59 * c.getGreen() + 0.11 * c.getBlue()) * 0.55);
        return new Color(grey, grey, grey);
    }

    private static Color raw(char key, String look, String seed) {
        int h = Math.abs((seed == null ? "" : seed).hashCode());
        boolean hero = look.equals(FIGHTER) || look.equals(ROGUE) || look.equals(WIZARD) || look.equals(ADVENTURER);
        Color primary = hero ? PRIMARY[h % PRIMARY.length] : enemyPrimary(look);
        Color clothing = hero ? CLOTHING[h % CLOTHING.length] : enemyClothing(look);
        switch (key) {
            case 'H': return primary;
            case 'h': return primary.brighter();
            case 'C': return clothing;
            case 'c': return clothing.darker();
            case 'S': return hero || look.equals(SOLDIER) ? SKIN[(h / 7) % SKIN.length] : new Color(0x9A8F7A);
            case 'E': return hero ? DARK_EYES : enemyEyes(look);
            case 'W': return METAL;
            case 'B': return BROWN;
            case 'G': return GOLD;
            default: return null;
        }
    }

    private static Color enemyPrimary(String look) {
        return switch (look) {
            case BEAST -> new Color(0x7A6A5A);
            case ROBED -> new Color(0x4A2D6B);
            case UNDEAD -> METAL;
            default -> new Color(0x5B6B4A);
        };
    }

    private static Color enemyClothing(String look) {
        return switch (look) {
            case ROBED -> new Color(0x2A1A40);
            default -> new Color(0x3F4A35);
        };
    }

    private static Color enemyEyes(String look) {
        return switch (look) {
            case UNDEAD -> new Color(0x111111);
            case ROBED -> new Color(0x7CFFB0);
            default -> new Color(0xE04848);
        };
    }
}
