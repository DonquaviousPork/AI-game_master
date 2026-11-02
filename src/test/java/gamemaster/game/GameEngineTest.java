package gamemaster.game;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.model.Character;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import gamemaster.model.Stat;
import gamemaster.model.Weapon;
import gamemaster.model.WizardClass;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameEngineTest {

    private final CharacterFactory factory = new CharacterFactory();
    private final Character fighter = factory.createFromClass("Brenna", new FighterClass()); // STR +2, DEX +1, AC 11
    private final Character wizard = factory.createFromClass("Mira", new WizardClass());

    private GameEngine engine(int... rolls) {
        Party party = new Party();
        party.addCharacter(fighter);
        party.addCharacter(wizard);
        return new GameEngine(new GameState(party), new RuleEngine(new FixedDice(rolls)));
    }

    private Monster goblin() { return new Monster("Goblin", 7, 13, Weapon.DAGGER); }

    private void start(GameEngine e) {
        e.setScenario(new Scenario("Cave", "A dark cave."));
    }

    // ---- phases ----

    @Test
    void startsInSetupAndMovesToStoryOnceScenarioIsSet() {
        GameEngine e = engine();
        assertEquals("Setup", e.getState().getPhase().name());
        start(e);
        assertEquals("Story", e.getState().getPhase().name());
    }

    @Test
    void addingAMonsterStartsCombat() {
        GameEngine e = engine();
        start(e);
        e.addMonster(goblin());
        assertEquals("Combat", e.getState().getPhase().name());
    }

    @Test
    void killingTheLastMonsterReturnsToStory() {
        GameEngine e = engine(20, 8); // hit, d8 8 + 2 = 10 damage
        start(e);
        e.addMonster(goblin());
        e.resolveAction(new AttackAction(fighter, Weapon.LONGSWORD, e.getState().getMonsters().get(0)));
        assertEquals("Story", e.getState().getPhase().name());
    }

    @Test
    void anEmptyPartyCannotLeaveSetup() {
        GameEngine e = new GameEngine(new GameState(new Party()), new RuleEngine(new FixedDice()));
        start(e);
        assertEquals("Setup", e.getState().getPhase().name());
    }

    @Test
    void defeatingTheWholePartyEndsTheGame() {
        GameEngine e = engine();
        start(e);
        e.addMonster(goblin());
        fighter.applyDamage(100);
        wizard.applyDamage(100);
        e.advancePhase();
        assertEquals("Ended", e.getState().getPhase().name());
        assertTrue(e.checkEndCondition());
    }

    @Test
    void concludingTheStoryEndsTheGame() {
        GameEngine e = engine();
        start(e);
        e.concludeStory();
        assertEquals("Ended", e.getState().getPhase().name());
        assertTrue(e.checkEndCondition());
    }

    // ---- validation ----

    @Test
    void attackIsRejectedOutsideCombat() {
        GameEngine e = engine();
        start(e);
        Action attack = new AttackAction(fighter, Weapon.LONGSWORD, goblin());
        assertThrows(IllegalStateException.class, () -> e.resolveAction(attack));
        assertEquals(0, e.getHistory().size());
    }

    @Test
    void nothingIsAllowedInSetupOrEnded() {
        GameEngine e = engine();
        assertThrows(IllegalStateException.class,
                () -> e.validate(new AbilityCheckAction(fighter, Stat.STRENGTH)));
        start(e);
        e.concludeStory();
        assertThrows(IllegalStateException.class,
                () -> e.validate(new AbilityCheckAction(fighter, Stat.STRENGTH)));
    }

    @Test
    void abilityCheckIsAllowedInStory() {
        GameEngine e = engine(15); // 15 + 2 = 17 vs 12
        start(e);
        ActionResult r = e.resolveAction(new AbilityCheckAction(fighter, Stat.STRENGTH));
        assertTrue(r.summary().contains("success"));
    }

    // ---- actions ----

    @Test
    void attackThatHitsReducesMonsterHealthAndIsLogged() {
        GameEngine e = engine(12, 5); // 12 + 2 = 14 vs AC 13 hits; d8 5 + 2 = 7
        start(e);
        Monster g = new Monster("Ogre", 20, 13, Weapon.LONGSWORD);
        e.addMonster(g);
        e.resolveAction(new AttackAction(fighter, Weapon.LONGSWORD, g));
        assertEquals(13, g.getHealth());
        assertTrue(e.getState().getLog().get(0).contains("7 damage"));
    }

    @Test
    void attackThatMissesChangesNothing() {
        GameEngine e = engine(2);
        start(e);
        Monster g = goblin();
        e.addMonster(g);
        e.resolveAction(new AttackAction(fighter, Weapon.LONGSWORD, g));
        assertEquals(7, g.getHealth());
    }

    @Test
    void monsterCanAttackThePartyThroughTheSameEngine() {
        GameEngine e = engine(18, 3); // goblin +0 vs fighter AC 11 hits; d4 3
        start(e);
        Monster g = goblin();
        e.addMonster(g);
        e.resolveAction(new AttackAction(g, g.getWeapon(), fighter));
        assertEquals(fighter.getMaxHealth() - 3, fighter.getHealth());
    }

    @Test
    void successfulFleeEndsTheEncounter() {
        GameEngine e = engine(15); // DEX +1 -> 16 vs 12
        start(e);
        e.addMonster(goblin());
        e.resolveAction(new FleeAction(fighter));
        assertEquals("Story", e.getState().getPhase().name());
        assertTrue(e.getState().getMonsters().isEmpty());
    }

    @Test
    void failedFleeKeepsTheFight() {
        GameEngine e = engine(2); // 2 + 1 = 3 vs 12
        start(e);
        e.addMonster(goblin());
        e.resolveAction(new FleeAction(fighter));
        assertEquals("Combat", e.getState().getPhase().name());
        assertEquals(1, e.getState().getMonsters().size());
    }

    // ---- undo ----

    @Test
    void undoRestoresDamageFromAHit() {
        GameEngine e = engine(12, 5);
        start(e);
        Monster g = new Monster("Ogre", 20, 13, Weapon.LONGSWORD);
        e.addMonster(g);
        e.resolveAction(new AttackAction(fighter, Weapon.LONGSWORD, g));
        assertTrue(e.undoLast());
        assertEquals(20, g.getHealth());
        assertEquals(0, e.getHistory().size());
    }

    @Test
    void undoingTheKillingBlowReturnsToCombat() {
        GameEngine e = engine(20, 8);
        start(e);
        Monster g = goblin();
        e.addMonster(g);
        e.resolveAction(new AttackAction(fighter, Weapon.LONGSWORD, g));
        assertEquals("Story", e.getState().getPhase().name());
        e.undoLast();
        assertEquals("Combat", e.getState().getPhase().name());
        assertEquals(7, g.getHealth());
    }

    @Test
    void undoingASuccessfulFleeBringsTheMonstersBack() {
        GameEngine e = engine(15);
        start(e);
        e.addMonster(goblin());
        e.resolveAction(new FleeAction(fighter));
        e.undoLast();
        assertEquals(1, e.getState().getMonsters().size());
        assertEquals("Combat", e.getState().getPhase().name());
    }

    @Test
    void undoWithEmptyHistoryReturnsFalse() {
        GameEngine e = engine();
        start(e);
        assertFalse(e.undoLast());
    }

    @Test
    void undoingAnAbilityCheckChangesNothing() {
        GameEngine e = engine(15);
        start(e);
        e.resolveAction(new AbilityCheckAction(fighter, Stat.STRENGTH));
        int hp = fighter.getHealth();
        assertTrue(e.undoLast());
        assertEquals(hp, fighter.getHealth());
    }

    // ---- observer ----

    @Test
    void attachedViewsAreNotifiedOnEveryChange() {
        GameEngine e = engine();
        List<String> seen = new ArrayList<>();
        e.getState().attach(s -> seen.add(s.getPhase().name()));
        start(e);
        assertFalse(seen.isEmpty());
        assertEquals("Story", seen.get(seen.size() - 1));
    }

    @Test
    void detachedViewsStopReceivingUpdates() {
        GameEngine e = engine();
        List<String> seen = new ArrayList<>();
        GameView view = s -> seen.add("x");
        e.getState().attach(view);
        e.getState().detach(view);
        start(e);
        assertTrue(seen.isEmpty());
    }

    @Test
    void currentEventIsStoredAndNotified() {
        GameEngine e = engine();
        List<String> seen = new ArrayList<>();
        e.getState().attach(s -> seen.add(s.getCurrentEvent() == null ? "none" : s.getCurrentEvent().description()));
        e.setCurrentEvent(new StoryEvent("A door creaks open."));
        assertEquals("A door creaks open.", e.getState().getCurrentEvent().description());
        assertEquals("A door creaks open.", seen.get(0));
    }

    // ---- value objects ----

    @Test
    void blankScenarioTitleAndEventAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Scenario(" ", "x"));
        assertThrows(IllegalArgumentException.class, () -> new StoryEvent(" "));
    }
}
