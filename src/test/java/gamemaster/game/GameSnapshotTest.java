package gamemaster.game;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.model.Character;
import gamemaster.model.CharacterClass;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import gamemaster.model.Stat;
import gamemaster.model.Weapon;
import gamemaster.model.WizardClass;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameSnapshotTest {

    private final CharacterFactory factory = new CharacterFactory();

    private GameEngine liveGame() {
        Party party = new Party();
        Character brenna = factory.createFromClass("Brenna", new FighterClass());
        Character mira = factory.createFromClass("Mira", new WizardClass());
        brenna.applyDamage(3);
        party.addCharacter(brenna);
        party.addCharacter(mira);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice()));
        e.setScenario(new Scenario("Grove", "A withering grove."));
        e.setCurrentEvent(new StoryEvent("Wolves appear."));
        Monster wolf = new Monster("Wolf", 12, 13, Weapon.DAGGER, Map.of(Stat.DEXTERITY, 14));
        wolf.applyDamage(5);
        e.addMonster(wolf);
        e.getState().addLog("Brenna hits Wolf.");
        return e;
    }

    private GameEngine blankGame() {
        Party party = new Party();
        party.addCharacter(factory.createFromClass("Placeholder", new FighterClass()));
        return new GameEngine(new GameState(party), new RuleEngine(new FixedDice()));
    }

    @Test
    void aSnapshotRestoresTheWholeGame() {
        GameSnapshot snap = liveGame().getState().createSnapshot("notes", List.of("turn one"));
        GameEngine fresh = blankGame();
        fresh.load(snap);
        GameState s = fresh.getState();

        assertEquals(2, s.getParty().size());
        Character brenna = s.getParty().getCharacters().get(0);
        assertEquals("Brenna", brenna.getName());
        assertEquals("Fighter", brenna.getCharacterClass().name());
        assertEquals(brenna.getMaxHealth() - 3, brenna.getHealth());
        assertEquals(15, brenna.getStat(Stat.STRENGTH));
        assertEquals("Mira", s.getParty().getCharacters().get(1).getName());

        Monster wolf = s.getMonsters().get(0);
        assertEquals("Wolf", wolf.getName());
        assertEquals(7, wolf.getHealth());
        assertEquals(12, wolf.getMaxHealth());
        assertEquals(13, wolf.getArmorClass());
        assertEquals(Weapon.DAGGER, wolf.getWeapon());
        assertEquals(14, wolf.getStat(Stat.DEXTERITY));

        assertEquals("Grove", s.getScenario().title());
        assertEquals("Wolves appear.", s.getCurrentEvent().description());
        assertEquals(List.of("Brenna hits Wolf."), s.getLog());
        assertEquals("Combat", s.getPhase().name());
        assertEquals("notes", snap.memorySummary());
        assertEquals(List.of("turn one"), snap.memoryRecent());
    }

    @Test
    void theSnapshotIsIndependentOfTheLiveGame() {
        GameEngine live = liveGame();
        GameSnapshot snap = live.getState().createSnapshot("", List.of());
        live.getState().getMonsters().get(0).applyDamage(100);
        live.getState().getParty().getCharacters().get(0).applyDamage(100);
        GameEngine fresh = blankGame();
        fresh.load(snap);
        assertEquals(7, fresh.getState().getMonsters().get(0).getHealth());
        assertTrue(fresh.getState().getParty().getCharacters().get(0).isAlive());
    }

    @Test
    void loadingMovesToTheCorrectPhase() {
        GameEngine live = liveGame();
        live.getState().getMonsters().get(0).applyDamage(100);
        GameEngine a = blankGame();
        a.load(live.getState().createSnapshot("", List.of()));
        assertEquals("Story", a.getState().getPhase().name());

        live.concludeStory();
        GameEngine b = blankGame();
        b.load(live.getState().createSnapshot("", List.of()));
        assertEquals("Ended", b.getState().getPhase().name());

        GameEngine defeated = liveGame();
        defeated.getState().getParty().getCharacters().forEach(c -> c.applyDamage(100));
        GameEngine c = blankGame();
        c.load(defeated.getState().createSnapshot("", List.of()));
        assertEquals("Ended", c.getState().getPhase().name());
    }

    @Test
    void loadingClearsUndoHistory() {
        GameEngine e = blankGame();
        e.setScenario(new Scenario("S", "d"));
        e.addMonster(new Monster("Rat", 2, 8, Weapon.DAGGER));
        e.getHistory().record(new FleeAction(e.getState().getParty().getCharacters().get(0)));
        assertEquals(1, e.getHistory().size());
        e.load(liveGame().getState().createSnapshot("", List.of()));
        assertTrue(e.getHistory().isEmpty());
    }

    @Test
    void viewsAreNotifiedWhenAGameIsLoaded() {
        GameEngine e = blankGame();
        int[] count = {0};
        e.getState().attach(s -> count[0]++);
        e.load(liveGame().getState().createSnapshot("", List.of()));
        assertTrue(count[0] > 0);
    }

    @Test
    void aDamagedSnapshotIsRejectedAndLeavesTheCurrentGameUntouched() {
        GameEngine e = liveGame();
        GameSnapshot good = e.getState().createSnapshot("", List.of());
        var badHero = new GameSnapshot.CharacterData("Ghost", "Fighter", good.party().get(0).stats(), 9999);
        GameSnapshot bad = new GameSnapshot(1, "", "T", "D", null, false, List.of(badHero), List.of(), List.of(), "", List.of());

        int hpBefore = e.getState().getParty().getCharacters().get(0).getHealth();
        assertThrows(IllegalArgumentException.class, () -> e.load(bad));
        assertEquals(2, e.getState().getParty().size());
        assertEquals(hpBefore, e.getState().getParty().getCharacters().get(0).getHealth());
        assertEquals("Wolf", e.getState().getMonsters().get(0).getName());
    }

    @Test
    void unknownClassesWeaponsAndStatsAreRejected() {
        Map<String, Integer> stats = liveGame().getState().createSnapshot("", List.of()).party().get(0).stats();
        assertThrows(IllegalArgumentException.class, () -> blankGame().load(new GameSnapshot(1, "", "T", "D", null, false,
                List.of(new GameSnapshot.CharacterData("X", "Paladin", stats, 10)), List.of(), List.of(), "", List.of())));
        assertThrows(IllegalArgumentException.class, () -> blankGame().load(new GameSnapshot(1, "", "T", "D", null, false,
                List.of(new GameSnapshot.CharacterData("X", "Fighter", Map.of("LUCK", 10), 10)), List.of(), List.of(), "", List.of())));
        var hero = new GameSnapshot.CharacterData("X", "Fighter", stats, 10);
        assertThrows(IllegalArgumentException.class, () -> blankGame().load(new GameSnapshot(1, "", "T", "D", null, false,
                List.of(hero), List.of(new GameSnapshot.MonsterData("Imp", 5, 5, 10, "laser", Map.of())),
                List.of(), "", List.of())));
    }

    @Test
    void aSnapshotWithNoPartyOrTooManyHeroesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> blankGame().load(
                new GameSnapshot(1, "", "T", "D", null, false, List.of(), List.of(), List.of(), "", List.of())));
        var hero = liveGame().getState().createSnapshot("", List.of()).party().get(0);
        assertThrows(IllegalArgumentException.class, () -> blankGame().load(new GameSnapshot(1, "", "T", "D", null, false,
                List.of(hero, hero, hero, hero, hero), List.of(), List.of(), "", List.of())));
    }

    @Test
    void lookupHelpersFindBuiltInsAndRejectUnknowns() {
        assertEquals("Wizard", CharacterClass.byName(" wizard ").name());
        assertEquals(Weapon.LONGSWORD, Weapon.byName("LONGSWORD"));
        assertThrows(IllegalArgumentException.class, () -> CharacterClass.byName("bard"));
        assertThrows(IllegalArgumentException.class, () -> CharacterClass.byName(null));
        assertThrows(IllegalArgumentException.class, () -> Weapon.byName("spoon"));
    }

    @Test
    void partyReplaceMembersValidatesBeforeChangingAnything() {
        Party p = new Party();
        Character a = factory.createFromClass("A", new FighterClass());
        p.addCharacter(a);
        assertThrows(IllegalArgumentException.class, () -> p.replaceMembers(List.of()));
        assertEquals(List.of(a), p.getCharacters());
        Character b = factory.createFromClass("B", new WizardClass());
        p.replaceMembers(List.of(b));
        assertEquals(List.of(b), p.getCharacters());
    }
}
