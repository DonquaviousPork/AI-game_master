package gamemaster.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CardReactionTest {

    private static GameViewModel.Card card(String name, int health, boolean alive) {
        return new GameViewModel.Card(name, "d", health, 12, alive, "FIGHTER", "", List.of(), "", 12);
    }

    @Test
    void lostHealthIsAHitAndShowsTheDamage() {
        CardReaction r = CardReaction.detect(card("Brenna", 6, true), 12, true, List.of(), false, true);
        assertEquals(CardReaction.Kind.HIT, r.kind());
        assertEquals(6, r.amount());
        assertEquals("-6", r.floating());
        assertTrue(!r.bubble().isEmpty());
    }

    @Test
    void aCriticalHitLineMakesItACrit() {
        List<String> lines = List.of("Gruk hits Brenna with Axe (rolled 25 vs AC 12) for 9 damage. CRITICAL HIT! (natural 20, double damage dice)");
        CardReaction r = CardReaction.detect(card("Brenna", 3, true), 12, true, lines, false, true);
        assertEquals(CardReaction.Kind.CRIT, r.kind());
        assertEquals("-9!", r.floating());
    }

    @Test
    void aMissLineMakesAMissWhenHealthIsUnchanged() {
        List<String> lines = List.of("Brenna attacks Gruk with Slash (rolled 4 vs AC 12) and misses.");
        CardReaction r = CardReaction.detect(card("Gruk", 5, true), 5, true, lines, false, false);
        assertEquals(CardReaction.Kind.MISS, r.kind());
        assertEquals("miss", r.floating());
    }

    @Test
    void goingToZeroIsADefeatAndHealingIsAHeal() {
        assertEquals(CardReaction.Kind.DEFEAT,
                CardReaction.detect(card("Gruk", 0, false), 3, true, List.of(), false, false).kind());
        CardReaction heal = CardReaction.detect(card("Brenna", 9, true), 6, true, List.of(), false, true);
        assertEquals(CardReaction.Kind.HEAL, heal.kind());
        assertEquals("+3", heal.floating());
    }

    @Test
    void nothingHappensWithoutAChangeOrWithoutAPreviousUpdate() {
        assertTrue(CardReaction.detect(card("Brenna", 6, true), 6, true, List.of(), false, true) == null);
        assertTrue(CardReaction.detect(card("Brenna", 6, true), null, true, List.of(), false, true) == null);
    }

    @Test
    void onlyLivingHeroesCheerOnVictory() {
        assertEquals(CardReaction.Kind.CHEER,
                CardReaction.detect(card("Brenna", 6, true), 6, true, List.of(), true, true).kind());
        assertTrue(CardReaction.detect(card("Gruk", 0, false), 0, false, List.of(), true, false) == null);
    }

    @Test
    void theSameEventAlwaysGivesTheSameLine() {
        CardReaction a = CardReaction.detect(card("Brenna", 6, true), 12, true, List.of(), false, true);
        CardReaction b = CardReaction.detect(card("Brenna", 6, true), 12, true, List.of(), false, true);
        assertEquals(a, b);
    }
}
