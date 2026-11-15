package gamemaster.game;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.model.Character;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import gamemaster.model.Weapon;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import org.junit.jupiter.api.Test;

class ObjectiveTest {

    private final Character fighter = new CharacterFactory().createFromClass("Brenna", new FighterClass());

    private GameEngine engine(Scenario scenario, int... rolls) {
        Party party = new Party();
        party.addCharacter(fighter);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(rolls)));
        e.setScenario(scenario);
        return e;
    }

    private Monster monster(String name) { return new Monster(name, 7, 13, Weapon.DAGGER); }

    private void kill(GameEngine e, int index) {
        e.resolveAction(new AttackAction(fighter, Weapon.LONGSWORD, e.getState().getMonsters().get(index)));
    }

    @Test
    void parseReadsTypesAndTreatsUnknownTextAsNarrative() {
        assertEquals(ObjectiveType.DEFEAT_TARGET, ObjectiveType.parse("defeat_target"));
        assertEquals(ObjectiveType.DEFEAT_ALL, ObjectiveType.parse(" Defeat-All "));
        assertEquals(ObjectiveType.SURVIVE, ObjectiveType.parse("SURVIVE"));
        assertEquals(ObjectiveType.NARRATIVE, ObjectiveType.parse("rescue the cat"));
        assertEquals(ObjectiveType.NARRATIVE, ObjectiveType.parse(null));
    }

    @Test
    void theThreeArgumentScenarioIsNarrative() {
        Scenario s = new Scenario("T", "d", "o");
        assertEquals(ObjectiveType.NARRATIVE, s.type());
        assertEquals("", s.target());
    }

    @Test
    void defeatingTheNamedTargetWinsWithoutTheAi() {
        GameEngine e = engine(new Scenario("Cave", "d", "Defeat the Goblin King.", ObjectiveType.DEFEAT_TARGET, "Goblin King"),
                19, 8);
        e.addMonster(monster("Goblin King"));
        kill(e, 0);
        assertTrue(e.getState().isConcluded());
        assertTrue(e.getState().isObjectiveAchieved());
    }

    @Test
    void defeatingADifferentEnemyDoesNotWinATargetObjective() {
        GameEngine e = engine(new Scenario("Cave", "d", "Defeat the Goblin King.", ObjectiveType.DEFEAT_TARGET, "Goblin King"),
                19, 8);
        e.addMonster(monster("Goblin King"));
        e.addMonster(monster("Rat"));
        kill(e, 1);
        assertFalse(e.getState().isConcluded());
        assertFalse(e.objectiveMet());
    }

    @Test
    void defeatAllNeedsEveryEnemyDown() {
        GameEngine e = engine(new Scenario("Cave", "d", "Defeat them all.", ObjectiveType.DEFEAT_ALL, ""), 19, 8, 19, 8);
        e.addMonster(monster("Rat"));
        e.addMonster(monster("Bat"));
        kill(e, 0);
        assertFalse(e.getState().isConcluded());
        kill(e, 1);
        assertTrue(e.getState().isObjectiveAchieved());
    }

    @Test
    void aNarrativeObjectiveIsNeverMetByTheEngine() {
        GameEngine e = engine(new Scenario("Cave", "d", "Find the key."), 19, 8);
        e.addMonster(monster("Rat"));
        kill(e, 0);
        assertFalse(e.objectiveMet());
        assertFalse(e.getState().isObjectiveAchieved());
    }

    @Test
    void typedObjectiveSurvivesASaveAndLoad() {
        GameEngine e = engine(new Scenario("Cave", "d", "Defeat the Goblin King.", ObjectiveType.DEFEAT_TARGET, "Goblin King"));
        GameSnapshot snap = e.getState().createSnapshot("", java.util.List.of());
        assertEquals("DEFEAT_TARGET", snap.objectiveType());
        GameEngine other = engine(new Scenario("X", "y"));
        other.load(snap);
        assertEquals(ObjectiveType.DEFEAT_TARGET, other.getState().getScenario().type());
        assertEquals("Goblin King", other.getState().getScenario().target());
    }

    @Test
    void anOldSaveWithoutTheTypeLoadsAsNarrative() {
        GameSnapshot old = new GameSnapshot(1, "t", "Cave", "d", "", false, java.util.List.of(), java.util.List.of(),
                java.util.List.of(), "", java.util.List.of());
        assertEquals("NARRATIVE", old.objectiveType());
    }
}
