package gamemaster.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CharacterFactoryTest {

    private final CharacterFactory factory = new CharacterFactory();

    private Map<Stat, Integer> alloc(int str, int dex, int con, int intel, int wis, int cha) {
        Map<Stat, Integer> m = new EnumMap<>(Stat.class);
        m.put(Stat.STRENGTH, str);
        m.put(Stat.DEXTERITY, dex);
        m.put(Stat.CONSTITUTION, con);
        m.put(Stat.INTELLIGENCE, intel);
        m.put(Stat.WISDOM, wis);
        m.put(Stat.CHARISMA, cha);
        return m;
    }

    @Test
    void everyClassSpendsTheSameTotalAsACustomBuild() {
        int expected = 6 * CharacterFactory.BASE_STAT + CharacterFactory.POINT_POOL;
        List<CharacterClass> classes = List.of(new FighterClass(), new WizardClass(), new RogueClass());
        for (CharacterClass cc : classes) {
            int total = cc.defaultStats().values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(expected, total, cc.name());
            cc.defaultStats().values().forEach(v -> assertTrue(v >= CharacterFactory.BASE_STAT && v <= CharacterFactory.MAX_STAT));
        }
    }

    @Test
    void createFromClassUsesDefaultStats() {
        Character c = factory.createFromClass("Mira", new WizardClass());
        assertEquals(15, c.getStat(Stat.INTELLIGENCE));
        assertEquals(8, c.getStat(Stat.STRENGTH));
        assertEquals("Wizard", c.getCharacterClass().name());
    }

    @Test
    void validCustomAllocationIsAccepted() {
        // 5 + 5 + 4 + 2 + 2 + 2 = 20 points on top of 8 each
        Character c = factory.createCustom("Tor", new FighterClass(), alloc(5, 5, 4, 2, 2, 2));
        assertEquals(13, c.getStat(Stat.STRENGTH));
        assertEquals(12, c.getStat(Stat.CONSTITUTION));
    }

    @Test
    void wrongTotalIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.createCustom("Tor", new FighterClass(), alloc(5, 5, 4, 2, 2, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> factory.createCustom("Tor", new FighterClass(), alloc(5, 5, 4, 2, 2, 3)));
    }

    @Test
    void scoreAboveMaximumIsRejected() {
        // STRENGTH would be 8 + 8 = 16
        assertThrows(IllegalArgumentException.class,
                () -> factory.createCustom("Tor", new FighterClass(), alloc(8, 4, 4, 2, 2, 0)));
    }

    @Test
    void negativePointsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.createCustom("Tor", new FighterClass(), alloc(-1, 7, 7, 7, 0, 0)));
    }
}
