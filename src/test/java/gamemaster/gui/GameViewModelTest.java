package gamemaster.gui;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.game.GameEngine;
import gamemaster.game.GameState;
import gamemaster.game.Scenario;
import gamemaster.game.StoryEvent;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import gamemaster.model.Weapon;
import gamemaster.model.WizardClass;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import org.junit.jupiter.api.Test;

class GameViewModelTest {

    private GameEngine engine() {
        Party party = new Party();
        CharacterFactory f = new CharacterFactory();
        party.addCharacter(f.createFromClass("Brenna", new FighterClass()));
        party.addCharacter(f.createFromClass("Mira", new WizardClass()));
        return new GameEngine(new GameState(party), new RuleEngine(new FixedDice()));
    }

    @Test
    void anEmptySetupGameHasNoTitleOrScene() {
        GameViewModel m = GameViewModel.from(engine().getState());
        assertEquals("", m.title());
        assertEquals("", m.scene());
        assertEquals("Setup", m.phase());
        assertEquals(2, m.heroes().size());
        assertTrue(m.enemies().isEmpty());
        assertFalse(m.over());
    }

    @Test
    void heroAndEnemyCardsCarryTheirCurrentNumbers() {
        GameEngine e = engine();
        e.setScenario(new Scenario("Grove", "d"));
        e.setCurrentEvent(new StoryEvent("Wolves appear."));
        Monster wolf = new Monster("Wolf", 12, 13, Weapon.DAGGER);
        wolf.applyDamage(7);
        e.addMonster(wolf);
        e.getState().getParty().getCharacters().get(0).applyDamage(3);

        GameViewModel m = GameViewModel.from(e.getState());
        assertEquals("Grove", m.title());
        assertEquals("Combat", m.phase());
        assertEquals("Wolves appear.", m.scene());
        GameViewModel.Card hero = m.heroes().get(0);
        assertEquals("Brenna", hero.name());
        assertEquals(21, hero.health());
        assertEquals(24, hero.maxHealth());
        assertTrue(hero.detail().contains("Fighter"));
        GameViewModel.Card enemy = m.enemies().get(0);
        assertEquals(5, enemy.health());
        assertEquals(12, enemy.maxHealth());
        assertTrue(enemy.detail().contains("Dagger") && enemy.detail().contains("AC 13"));
        assertTrue(enemy.alive());
    }

    @Test
    void defeatedCombatantsAreMarkedAndTheGameIsOverWhenThePartyFalls() {
        GameEngine e = engine();
        e.setScenario(new Scenario("Grove", "d"));
        Monster wolf = new Monster("Wolf", 5, 10, Weapon.DAGGER);
        wolf.applyDamage(99);
        e.addMonster(wolf);
        assertFalse(GameViewModel.from(e.getState()).enemies().get(0).alive());

        e.getState().getParty().getCharacters().forEach(c -> c.applyDamage(999));
        assertTrue(GameViewModel.from(e.getState()).over());
    }

    @Test
    void aConcludedStoryIsOver() {
        GameEngine e = engine();
        e.setScenario(new Scenario("Grove", "d"));
        e.concludeStory();
        GameViewModel m = GameViewModel.from(e.getState());
        assertTrue(m.over());
        assertEquals("Ended", m.phase());
    }

    @Test
    void theModelIsASnapshotNotALiveView() {
        GameEngine e = engine();
        GameViewModel before = GameViewModel.from(e.getState());
        e.getState().getParty().getCharacters().get(0).applyDamage(10);
        assertEquals(24, before.heroes().get(0).health());
    }

    @Test
    void healthBarColoursFollowTheHealthRatio() {
        assertEquals(GuiTheme.HEALTH_OK, HealthBar.colourFor(20, 20));
        assertEquals(GuiTheme.HEALTH_MID, HealthBar.colourFor(8, 20));
        assertEquals(GuiTheme.HEALTH_LOW, HealthBar.colourFor(4, 20));
        assertEquals(GuiTheme.HEALTH_LOW, HealthBar.colourFor(0, 20));
    }

    @Test
    void heroCardsCarryTheirFourAttacksForTheTooltipAndEnemiesDoNot() {
        GameEngine e = engine();
        GameViewModel m = GameViewModel.from(e.getState());
        String tip = m.heroes().get(0).tooltip();
        assertTrue(tip.contains("Slash - d8, +0 to hit"), tip);
        assertTrue(tip.contains("Power Strike - d12, -3 to hit"), tip);
        assertTrue(GameViewModel.attacksLine(e.getState().getParty().getCharacters().get(0)).contains("Shield Bash d4 +2"));
    }

    @Test
    void theViewModelCarriesTheObjectiveTurnCounterAndEnding() {
        GameEngine e = engine();
        e.setScenario(new gamemaster.game.Scenario("T", "D", "Light the beacon."));
        e.getState().startTurn();
        GameViewModel m = GameViewModel.from(e.getState());
        assertEquals("Light the beacon.", m.objective());
        assertEquals(1, m.turn());
        assertEquals(20, m.turnLimit());
        assertEquals("", m.ending());
        e.concludeStory(true);
        assertTrue(GameViewModel.from(e.getState()).ending().startsWith("VICTORY"));
    }
}
