package gamemaster.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The same health rules must hold for every Combatant implementation. */
class CombatantContractTest {

    private List<Combatant> implementations() {
        return List.of(
                new CharacterFactory().createFromClass("Brenna", new FighterClass()),
                new Monster("Ogre", 20, 11, Weapon.LONGSWORD));
    }

    @Test
    void damageReducesHealthButNeverBelowZero() {
        for (Combatant c : implementations()) {
            int before = c.getHealth();
            c.applyDamage(1);
            assertEquals(before - 1, c.getHealth(), c.getName());
            c.applyDamage(1000);
            assertEquals(0, c.getHealth(), c.getName());
            assertFalse(c.isAlive(), c.getName());
        }
    }

    @Test
    void healingNeverExceedsMaxHealth() {
        for (Combatant c : implementations()) {
            c.applyDamage(2);
            c.heal(1000);
            assertEquals(c.getMaxHealth(), c.getHealth(), c.getName());
        }
    }

    @Test
    void negativeAmountsAreRejected() {
        for (Combatant c : implementations()) {
            assertThrows(IllegalArgumentException.class, () -> c.applyDamage(-1), c.getName());
            assertThrows(IllegalArgumentException.class, () -> c.heal(-1), c.getName());
        }
    }
}
