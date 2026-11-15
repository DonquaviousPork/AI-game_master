package gamemaster.game;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.model.Character;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Party;
import gamemaster.model.RogueClass;
import gamemaster.model.WizardClass;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

class HealActionTest {

    private final CharacterFactory factory = new CharacterFactory();
    private final Character fighter = factory.createFromClass("Brenna", new FighterClass());
    private final Character rogue = factory.createFromClass("Rook", new RogueClass());
    private final Character wizard = factory.createFromClass("Mira", new WizardClass());

    private GameEngine engine(int... rolls) {
        Party party = new Party();
        party.addCharacter(fighter);
        party.addCharacter(rogue);
        party.addCharacter(wizard);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(rolls)));
        e.setScenario(new Scenario("Cave", "d"));
        return e;
    }

    @Test
    void healingLightHealsEveryLivingHeroAndCapsAtMaximum() {
        GameEngine e = engine(3);            // d6 3 + WIS 14 (+2) = 5
        fighter.applyDamage(10);
        rogue.applyDamage(2);
        e.resolveAction(new HealAction(wizard));
        assertEquals(fighter.getMaxHealth() - 5, fighter.getHealth());
        assertEquals(rogue.getMaxHealth(), rogue.getHealth());
        assertEquals(1, e.getState().healsLeft());
    }

    @Test
    void aDefeatedHeroIsNotRevived() {
        GameEngine e = engine(6);
        rogue.applyDamage(100);
        fighter.applyDamage(8);
        e.resolveAction(new HealAction(wizard));
        assertFalse(rogue.isAlive());
        assertEquals(fighter.getMaxHealth(), fighter.getHealth());
    }

    @Test
    void onlyWizardsCanCast() {
        GameEngine e = engine(3);
        assertThrows(IllegalStateException.class, () -> e.resolveAction(new HealAction(fighter)));
        assertThrows(IllegalStateException.class, () -> e.resolveAction(new HealAction(rogue)));
        assertEquals(GameState.HEALS_PER_ADVENTURE, e.getState().healsLeft());
    }

    @Test
    void itCanBeCastTwiceAndThenNoMore() {
        GameEngine e = engine(3, 3, 3);
        e.resolveAction(new HealAction(wizard));
        e.resolveAction(new HealAction(wizard));
        assertThrows(IllegalStateException.class, () -> e.resolveAction(new HealAction(wizard)));
        assertEquals(0, e.getState().healsLeft());
    }

    @Test
    void healingIsAtLeastOne() {
        GameEngine e = engine(1);            // 1 + WIS mod 2 = 3, never below 1 even with a low modifier
        fighter.applyDamage(5);
        e.resolveAction(new HealAction(wizard));
        assertTrue(fighter.getHealth() > fighter.getMaxHealth() - 5);
    }

    @Test
    void undoTakesTheHealingBackAndGivesTheUseBack() {
        GameEngine e = engine(4);
        fighter.applyDamage(10);
        e.resolveAction(new HealAction(wizard));
        assertEquals(fighter.getMaxHealth() - 4, fighter.getHealth());
        assertTrue(e.undoLast());
        assertEquals(fighter.getMaxHealth() - 10, fighter.getHealth());
        assertEquals(GameState.HEALS_PER_ADVENTURE, e.getState().healsLeft());
    }

    @Test
    void usesLeftAreSavedWithTheGame() {
        GameEngine e = engine(3);
        e.resolveAction(new HealAction(wizard));
        GameSnapshot snap = e.getState().createSnapshot("", List.of());
        assertEquals(1, snap.healsUsed());
        GameEngine other = engine();
        other.load(snap);
        assertEquals(1, other.getState().healsLeft());
    }

    @Test
    void failedChecksInAnActAreSavedWithTheGame() {
        GameEngine e = engine(2);
        e.setScenario(new Scenario("Cave", "d", "Defeat the Wisp.", ObjectiveType.DEFEAT_TARGET, "Wisp",
                List.of(new Milestone("The Clue", "A map.", MilestoneKind.CHALLENGE))));
        e.resolveAction(new AbilityCheckAction(fighter, gamemaster.model.Stat.STRENGTH));
        GameSnapshot snap = e.getState().createSnapshot("", List.of());
        assertEquals(1, snap.beatFailures());
    }

    @Test
    void healingIsAllowedInTheStoryPhaseAndNotInSetup() {
        GameEngine e = engine(3);
        assertEquals("Story", e.getState().getPhase().name());
        e.resolveAction(new HealAction(wizard));
        GameEngine setup = new GameEngine(new GameState(new Party()), new RuleEngine(new FixedDice(3)));
        assertThrows(IllegalStateException.class, () -> setup.resolveAction(new HealAction(wizard)));
    }
}
