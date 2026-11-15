package gamemaster.game;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.model.Character;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import gamemaster.model.Stat;
import gamemaster.model.Weapon;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

class MilestoneTest {

    private final Character fighter = new CharacterFactory().createFromClass("Brenna", new FighterClass());

    private static final List<Milestone> ACTS = List.of(
            new Milestone("The Clue", "A torn map is found.", MilestoneKind.CHALLENGE),
            new Milestone("The Ambush", "Cultists strike.", MilestoneKind.COMBAT));

    private GameEngine engine(int... rolls) {
        Party party = new Party();
        party.addCharacter(fighter);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(rolls)));
        e.setScenario(new Scenario("Lighthouse", "d", "Defeat the Warden.", ObjectiveType.DEFEAT_TARGET, "Warden", ACTS));
        return e;
    }

    private void check(GameEngine e) { e.resolveAction(new AbilityCheckAction(fighter, Stat.STRENGTH)); }

    private void kill(GameEngine e, int index) {
        e.resolveAction(new AttackAction(fighter, Weapon.LONGSWORD, e.getState().getMonsters().get(index)));
    }

    @Test
    void theStoryStartsInTheFirstAct() {
        GameEngine e = engine();
        assertEquals("Act 1 of 3: The Clue", e.getState().progressText());
        assertFalse(e.getState().inFinale());
    }

    @Test
    void aSuccessfulCheckEndsAChallengeAct() {
        GameEngine e = engine(20);
        check(e);
        assertEquals("Act 2 of 3: The Ambush", e.getState().progressText());
    }

    @Test
    void aFailedCheckDoesNotEndTheAct() {
        GameEngine e = engine(1);
        check(e);
        assertEquals(0, e.getState().getMilestoneIndex());
    }

    @Test
    void defeatingAnEnemyEndsACombatActButNotAChallengeAct() {
        GameEngine e = engine(19, 8, 20, 19, 8);
        e.addMonster(new Monster("Cultist", 5, 10, Weapon.DAGGER));
        kill(e, 0);                                   // act 1 is a challenge: a kill does not finish it
        assertEquals(0, e.getState().getMilestoneIndex());
        check(e);                                     // challenge done
        assertEquals(1, e.getState().getMilestoneIndex());
        e.addMonster(new Monster("Cultist 2", 5, 10, Weapon.DAGGER));
        kill(e, 1);                                   // combat done
        assertTrue(e.getState().inFinale());
        assertEquals("Act 3 of 3: the showdown", e.getState().progressText());
    }

    @Test
    void theBossDoesNotWinBeforeTheFinalAct() {
        GameEngine e = engine(19, 8);
        e.addMonster(new Monster("Warden", 5, 10, Weapon.DAGGER));
        kill(e, 0);
        assertFalse(e.getState().isConcluded());
        assertFalse(e.objectiveMet());
    }

    @Test
    void theBossWinsOnceEveryActIsDone() {
        GameEngine e = engine(20, 19, 8, 19, 8);
        check(e);
        e.addMonster(new Monster("Cultist", 5, 10, Weapon.DAGGER));
        kill(e, 0);
        assertTrue(e.getState().inFinale());
        e.addMonster(new Monster("Warden", 5, 10, Weapon.DAGGER));
        kill(e, 1);
        assertTrue(e.getState().isConcluded());
        assertTrue(e.getState().isObjectiveAchieved());
    }

    @Test
    void actsAndProgressSurviveASaveAndLoad() {
        GameEngine e = engine(20);
        check(e);
        GameSnapshot snap = e.getState().createSnapshot("", List.of());
        assertEquals(2, snap.milestones().size());
        assertEquals(1, snap.milestoneIndex());
        GameEngine other = engine();
        other.load(snap);
        assertEquals("Act 2 of 3: The Ambush", other.getState().progressText());
        assertEquals(MilestoneKind.COMBAT, other.getState().currentMilestone().kind());
    }

    @Test
    void anOldSaveHasNoActs() {
        GameSnapshot old = new GameSnapshot(1, "t", "Cave", "d", "", false, List.of(), List.of(), List.of(), "",
                List.of());
        assertTrue(old.milestones().isEmpty());
        assertEquals(0, old.milestoneIndex());
    }

    @Test
    void aScenarioWithoutActsIsAlwaysInTheFinale() {
        GameEngine e = engine();
        e.setScenario(new Scenario("Cave", "d", "Defeat the Warden.", ObjectiveType.DEFEAT_TARGET, "Warden"));
        assertTrue(e.getState().inFinale());
        assertEquals("", e.getState().progressText());
    }

    @Test
    void aTurnCountsTowardsTheCurrentActOnly() {
        GameEngine e = engine(20);
        e.getState().startTurn();
        e.getState().startTurn();
        assertEquals(2, e.getState().getBeatTurns());
        check(e);
        assertEquals(0, e.getState().getBeatTurns());
    }
}
