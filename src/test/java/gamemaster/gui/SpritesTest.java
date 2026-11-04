package gamemaster.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpritesTest {

    private static final List<String> LOOKS = List.of(Sprites.FIGHTER, Sprites.ROGUE, Sprites.WIZARD,
            Sprites.ADVENTURER, Sprites.SOLDIER, Sprites.BEAST, Sprites.UNDEAD, Sprites.ROBED);

    @Test
    void everySpriteIsTheRightSizeAndUsesOnlyKnownColours() {
        for (String look : LOOKS) {
            String[] rows = Sprites.grid(look);
            assertEquals(Sprites.HEIGHT, rows.length, look + " height");
            for (String row : rows) {
                assertEquals(Sprites.WIDTH, row.length(), look + " width: " + row);
                for (char c : row.toCharArray()) {
                    assertTrue(c == '.' || Sprites.colour(c, look, "x", true) != null, look + " unknown key " + c);
                }
            }
        }
    }

    @Test
    void heroClassesMapToTheirSpriteAndCustomHeroesGetTheDefault() {
        assertEquals(Sprites.FIGHTER, Sprites.lookForHero("Fighter"));
        assertEquals(Sprites.WIZARD, Sprites.lookForHero("Wizard"));
        assertEquals(Sprites.ADVENTURER, Sprites.lookForHero("Custom"));
        assertEquals(Sprites.ADVENTURER, Sprites.lookForHero(null));
    }

    @Test
    void enemiesAreSortedByNameThenWeapon() {
        assertEquals(Sprites.BEAST, Sprites.lookForEnemy("Dire Wolf", "Dagger"));
        assertEquals(Sprites.UNDEAD, Sprites.lookForEnemy("Skeleton", "Dagger"));
        assertEquals(Sprites.ROBED, Sprites.lookForEnemy("Arcane Grenadier", "Quarterstaff"));
        assertEquals(Sprites.ROBED, Sprites.lookForEnemy("Hedge Thing", "Quarterstaff"));
        assertEquals(Sprites.SOLDIER, Sprites.lookForEnemy("Empowered Soldier", "Dagger"));
    }

    @Test
    void defeatedSpritesAreGreyAndDarkerThanTheLivingOnes() {
        Color alive = Sprites.colour('C', Sprites.FIGHTER, "Brenna", true);
        Color dead = Sprites.colour('C', Sprites.FIGHTER, "Brenna", false);
        assertEquals(dead.getRed(), dead.getGreen());
        assertEquals(dead.getGreen(), dead.getBlue());
        assertTrue(dead.getRed() < (alive.getRed() + alive.getGreen() + alive.getBlue()) / 3 + 1);
        assertTrue(Sprites.colour('.', Sprites.FIGHTER, "Brenna", false) == null);
    }

    @Test
    void sameNameAlwaysGetsTheSamePaletteButNamesCanDiffer() {
        assertEquals(Sprites.colour('H', Sprites.FIGHTER, "Brenna", true), Sprites.colour('H', Sprites.FIGHTER, "Brenna", true));
        boolean differs = false;
        for (String other : List.of("Aldo", "Mira", "Tess", "Orin", "Kael", "Zed")) {
            differs |= !Sprites.colour('H', Sprites.FIGHTER, "Brenna", true).equals(Sprites.colour('H', Sprites.FIGHTER, other, true));
        }
        assertTrue(differs);
    }

    @Test
    void onlyLivingHeroesInTheRedZoneGlow() {
        assertTrue(HealthBar.isLow(5, 20));
        assertFalse(HealthBar.isLow(6, 20));
        assertFalse(HealthBar.isLow(0, 0));
        assertTrue(new SpriteIcon(Sprites.FIGHTER, "A", true, true).isGlowing());
        assertFalse(new SpriteIcon(Sprites.FIGHTER, "A", true, false).isGlowing());
        assertFalse(new SpriteIcon(Sprites.FIGHTER, "A", false, true).isGlowing());
    }
}
