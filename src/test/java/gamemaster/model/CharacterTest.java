package gamemaster.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CharacterTest {

    private Character fighter() {
        return new CharacterFactory().createFromClass("Brenna", new FighterClass());
    }

    @Test
    void modifierRoundsDown() {
        assertEquals(0, Character.modifier(10));
        assertEquals(2, Character.modifier(15));
        assertEquals(-1, Character.modifier(9));
        assertEquals(-1, Character.modifier(8));
    }

    @Test
    void derivedStatsFollowScores() {
        Character c = fighter();
        assertEquals(24, c.getMaxHealth());   // 10 + CON 14
        assertEquals(11, c.getArmorClass());  // 10 + mod(DEX 12)
        assertEquals(c.getMaxHealth(), c.getHealth());
    }

    @Test
    void damageNeverDropsHealthBelowZero() {
        Character c = fighter();
        c.applyDamage(1000);
        assertEquals(0, c.getHealth());
        assertFalse(c.isAlive());
    }

    @Test
    void negativeDamageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> fighter().applyDamage(-1));
    }

    @Test
    void blankNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new CharacterFactory().createFromClass("  ", new WizardClass()));
    }
}
