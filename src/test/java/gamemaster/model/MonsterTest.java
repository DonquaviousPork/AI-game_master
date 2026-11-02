package gamemaster.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MonsterTest {

    private Monster goblin() {
        return new Monster("Goblin", 7, 13, Weapon.DAGGER, Map.of(Stat.DEXTERITY, 14));
    }

    @Test
    void startsAtFullHealthAndAlive() {
        Monster m = goblin();
        assertEquals(7, m.getHealth());
        assertEquals(7, m.getMaxHealth());
        assertTrue(m.isAlive());
    }

    @Test
    void missingStatsDefaultToTenWithZeroModifier() {
        Monster m = goblin();
        assertEquals(10, m.getStat(Stat.STRENGTH));
        assertEquals(0, m.getModifier(Stat.STRENGTH));
        assertEquals(14, m.getStat(Stat.DEXTERITY));
        assertEquals(2, m.getModifier(Stat.DEXTERITY));
    }

    @Test
    void shortConstructorUsesAllDefaultStats() {
        Monster m = new Monster("Rat", 3, 10, Weapon.DAGGER);
        for (Stat s : Stat.values()) {
            assertEquals(0, m.getModifier(s));
        }
    }

    @Test
    void damageFloorsHealthAtZeroAndDefeatsTheMonster() {
        Monster m = goblin();
        m.applyDamage(100);
        assertEquals(0, m.getHealth());
        assertFalse(m.isAlive());
    }

    @Test
    void healingIsCappedAtMaxHealth() {
        Monster m = goblin();
        m.applyDamage(3);
        m.heal(50);
        assertEquals(7, m.getHealth());
    }

    @Test
    void exposesItsWeapon() {
        assertEquals(Weapon.DAGGER, goblin().getWeapon());
    }

    @Test
    void invalidConstructionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Monster(" ", 5, 10, Weapon.DAGGER));
        assertThrows(IllegalArgumentException.class, () -> new Monster("X", 0, 10, Weapon.DAGGER));
        assertThrows(IllegalArgumentException.class, () -> new Monster("X", 5, 0, Weapon.DAGGER));
        assertThrows(NullPointerException.class, () -> new Monster("X", 5, 10, null));
    }
}
