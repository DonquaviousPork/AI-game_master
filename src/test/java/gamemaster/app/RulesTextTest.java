package gamemaster.app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RulesTextTest {

    @Test
    void coversTheLoopTheActsAndCombat() {
        String all = RulesText.plain(200);
        for (String must : new String[] {"THE GOAL", "HOW A TURN WORKS", "THE STORY ACTS", "ABILITY CHECKS",
                "SAVING THROWS", "COMBAT", "HEALING", "DEMO MODE", "natural 20", "armor class", "Healing Light"}) {
            assertTrue(all.contains(must), "missing: " + must);
        }
    }

    @Test
    void plainTextIsWrappedToTheRequestedWidth() {
        for (String line : RulesText.plain(60).split("\n")) {
            assertTrue(line.length() <= 60, line);
        }
    }

    @Test
    void everySectionHasATitleAndText() {
        for (RulesText.Section s : RulesText.sections()) {
            assertFalse(s.title().isBlank());
            assertFalse(s.paragraphs().isEmpty());
        }
    }
}
