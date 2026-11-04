package gamemaster.app;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.ai.FakeLLM;
import gamemaster.ai.TurnResult;
import gamemaster.llm.LLMException;
import gamemaster.model.Stat;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import gamemaster.save.SaveRepository;
import gamemaster.save.SaveService;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameFacadeTest {

    static final String SCENARIO = """
            {"title":"The Hollow Crypt","description":"A cold wind blows from an old crypt.",
             "opening_event":"A skeleton rises from a tomb.",
             "monsters":[{"name":"Skeleton","health":9,"armor_class":12,"weapon":"dagger"}]}""";
    static final String CALM = """
            {"title":"Quiet Road","description":"A long road.","opening_event":"Birds sing.","monsters":[]}""";

    private GameFacade facade(FakeLLM llm, int... rolls) throws Exception {
        return new GameFacade(llm, new SaveService(new SaveRepository(Files.createTempDirectory("facade"))),
                new RuleEngine(new FixedDice(rolls)));
    }

    @Test
    void partySetupAddsCustomAndRemovesCharacters() throws Exception {
        GameFacade g = facade(new FakeLLM());
        g.addCharacter("Brenna", "Fighter");
        g.addCustomCharacter("Mira", "wizard", Map.of(Stat.INTELLIGENCE, 7, Stat.WISDOM, 7, Stat.DEXTERITY, 6));
        assertEquals(2, g.getParty().size());
        assertEquals(15, g.getParty().get(1).getStat(Stat.INTELLIGENCE));
        g.removeCharacter("brenna");
        assertEquals(List.of("Mira"), g.getParty().stream().map(c -> c.getName()).toList());
    }

    @Test
    void partyRulesAreEnforced() throws Exception {
        GameFacade g = facade(new FakeLLM());
        g.addCharacter("A", "Fighter");
        assertThrows(IllegalArgumentException.class, () -> g.addCharacter("a", "Rogue"));       // duplicate name
        assertThrows(IllegalArgumentException.class, () -> g.addCharacter("B", "Paladin"));     // unknown class
        assertThrows(IllegalArgumentException.class, () -> g.addCustomCharacter("C", "Rogue", Map.of(Stat.STRENGTH, 3)));
        assertThrows(IllegalArgumentException.class, () -> g.removeCharacter("Nobody"));
        assertThrows(IllegalStateException.class, () -> g.removeCharacter("A"));                // last member
        g.addCharacter("B", "Rogue");
        g.addCharacter("C", "Wizard");
        g.addCharacter("D", "Fighter");
        assertThrows(IllegalStateException.class, () -> g.addCharacter("E", "Fighter"));        // full
        assertEquals(4, g.getParty().size());
    }

    @Test
    void startingNeedsAPartyAndIsOnlyAllowedOnce() throws Exception {
        FakeLLM llm = new FakeLLM(SCENARIO);
        GameFacade g = facade(llm);
        assertThrows(IllegalStateException.class, () -> g.startAdventure(null));
        assertTrue(llm.calls.isEmpty());
        g.addCharacter("Brenna", "Fighter");
        var s = g.startAdventure("crypt");
        assertEquals("The Hollow Crypt", s.scenario().title());
        assertTrue(g.isStarted());
        assertEquals("Combat", g.getState().getPhase().name());
        assertThrows(IllegalStateException.class, () -> g.startAdventure(null));
        assertThrows(IllegalStateException.class, () -> g.addCharacter("Late", "Rogue"));
    }

    @Test
    void actionsAndUndoNeedAStartedGame() throws Exception {
        GameFacade g = facade(new FakeLLM());
        g.addCharacter("Brenna", "Fighter");
        assertThrows(IllegalStateException.class, () -> g.submitAction("hello"));
        assertThrows(IllegalStateException.class, g::undo);
    }

    @Test
    void aTurnRunsThroughTheAgentAndTheEngine() throws Exception {
        FakeLLM llm = new FakeLLM(SCENARIO)
                .thenTool("attack", "{\"attacker\":\"Brenna\",\"target\":\"Skeleton\"}")
                .thenText("Brenna's blade bites deep.");
        GameFacade g = facade(llm, 15, 4); // hit, 4 + 2 = 6 damage
        g.addCharacter("Brenna", "Fighter");
        g.startAdventure(null);
        TurnResult r = g.submitAction("I attack the skeleton");
        assertEquals("Brenna's blade bites deep.", r.narration());
        assertEquals(3, g.getState().getMonsters().get(0).getHealth());
    }

    @Test
    void undoReversesTheLastRulesAction() throws Exception {
        FakeLLM llm = new FakeLLM(SCENARIO)
                .thenTool("attack", "{\"attacker\":\"Brenna\",\"target\":\"Skeleton\"}")
                .thenText("Hit.");
        GameFacade g = facade(llm, 15, 4);
        g.addCharacter("Brenna", "Fighter");
        g.startAdventure(null);
        g.submitAction("attack");
        assertTrue(g.undo());
        assertEquals(9, g.getState().getMonsters().get(0).getHealth());
        assertFalse(g.undo());
    }

    @Test
    void saveAndLoadRoundTripThroughTheFacade() throws Exception {
        FakeLLM llm = new FakeLLM(CALM).thenText("The road winds on.");
        GameFacade g = facade(llm);
        g.addCharacter("Brenna", "Fighter");
        g.startAdventure(null);
        g.submitAction("walk");
        g.save("trip");
        assertEquals(List.of("trip"), g.listSaves());
        g.getParty().get(0).applyDamage(5);
        g.load("trip");
        assertEquals(g.getParty().get(0).getMaxHealth(), g.getParty().get(0).getHealth());
        assertEquals("The road winds on.", g.getState().getCurrentEvent().description());
    }

    @Test
    void savingWithoutAPartyIsRefused() throws Exception {
        assertThrows(IllegalStateException.class, () -> facade(new FakeLLM()).save("x"));
    }

    @Test
    void viewsSeeChangesMadeThroughTheFacade() throws Exception {
        GameFacade g = facade(new FakeLLM(CALM));
        int[] seen = {0};
        g.attach(s -> seen[0]++);
        g.addCharacter("Brenna", "Fighter");
        g.startAdventure(null);
        assertTrue(seen[0] > 0);
    }

    @Test
    void aModelFailureWhileStartingLeavesTheGameInSetup() throws Exception {
        GameFacade g = new GameFacade((m, t) -> { throw new LLMException("offline"); },
                new SaveService(new SaveRepository(Files.createTempDirectory("facade"))), new RuleEngine(new FixedDice()));
        g.addCharacter("Brenna", "Fighter");
        assertThrows(LLMException.class, () -> g.startAdventure(null));
        assertFalse(g.isStarted());
        g.addCharacter("Mira", "Wizard"); // still in setup
    }
}
