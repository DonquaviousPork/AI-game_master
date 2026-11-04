package gamemaster.cli;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.model.Stat;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatInputTest {

    @Test
    void readsPairsInSeveralStyles() {
        Map<Stat, Integer> m = StatInput.parse("STR 4 dex=3 con:5, Int 2 wisdom 3 CHA 3");
        assertEquals(4, m.get(Stat.STRENGTH));
        assertEquals(3, m.get(Stat.DEXTERITY));
        assertEquals(5, m.get(Stat.CONSTITUTION));
        assertEquals(2, m.get(Stat.INTELLIGENCE));
        assertEquals(3, m.get(Stat.WISDOM));
        assertEquals(3, m.get(Stat.CHARISMA));
    }

    @Test
    void fullNamesWork() {
        assertEquals(7, StatInput.parse("strength 7").get(Stat.STRENGTH));
    }

    @Test
    void emptyInputGivesNoPoints() {
        assertTrue(StatInput.parse("").isEmpty());
        assertTrue(StatInput.parse(null).isEmpty());
    }

    @Test
    void unknownDuplicateAndGarbledInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> StatInput.parse("LUCK 3"));
        assertThrows(IllegalArgumentException.class, () -> StatInput.parse("ST 3"));
        assertThrows(IllegalArgumentException.class, () -> StatInput.parse("STR 3 STR 2"));
        assertThrows(IllegalArgumentException.class, () -> StatInput.parse("STR 3 and then some"));
    }
}
