package gamemaster.ai;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.game.GameEngine;
import gamemaster.game.GameState;
import gamemaster.game.Scenario;
import gamemaster.game.StoryEvent;
import gamemaster.llm.ChatMessage;
import gamemaster.llm.LLMResponse;
import gamemaster.llm.ToolCall;
import gamemaster.model.Character;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Party;
import gamemaster.model.WizardClass;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentOrchestratorTest {

    private static final String WOLF = "{\"name\":\"Wolf\",\"health\":10,\"armor_class\":12,\"weapon\":\"dagger\"}";
    private static final String CHECK = "{\"character\":\"Brenna\",\"stat\":\"STRENGTH\"}";

    private final CharacterFactory factory = new CharacterFactory();
    private final Character fighter = factory.createFromClass("Brenna", new FighterClass());
    private final Character wizard = factory.createFromClass("Mira", new WizardClass());

    private GameEngine engine(int... rolls) {
        Party party = new Party();
        party.addCharacter(fighter);
        party.addCharacter(wizard);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(rolls)));
        e.setScenario(new Scenario("Grove", "A withering grove."));
        e.setCurrentEvent(new StoryEvent("You stand at the grove's edge."));
        return e;
    }

    @Test
    void narrationOnlyTurnMakesOneCallAndOffersAllTools() {
        GameEngine e = engine();
        FakeLLM llm = new FakeLLM("The wind rustles the leaves.");
        TurnResult r = new AgentOrchestrator(llm, e).takeTurn("I look around");
        assertEquals("The wind rustles the leaves.", r.narration());
        assertEquals(1, llm.calls.size());
        assertEquals(5, llm.toolsOffered.get(0).size());
        assertTrue(r.toolActivity().isEmpty());
        assertFalse(r.gameOver());
    }

    @Test
    void narrationBecomesTheCurrentScene() {
        GameEngine e = engine();
        new AgentOrchestrator(new FakeLLM("A cold mist rises."), e).takeTurn("I wait");
        assertEquals("A cold mist rises.", e.getState().getCurrentEvent().description());
    }

    @Test
    void theModelIsShownTheRealStateAndThePlayersAction() {
        GameEngine e = engine();
        FakeLLM llm = new FakeLLM("ok");
        new AgentOrchestrator(llm, e).takeTurn("I search the roots");
        var first = llm.calls.get(0);
        assertEquals("system", first.get(0).role());
        assertTrue(first.get(0).content().contains("Never invent dice rolls"));
        String user = first.get(1).content();
        assertTrue(user.contains("Brenna") && user.contains("Mira"));
        assertTrue(user.contains("Phase: Story"));
        assertTrue(user.contains("I search the roots"));
    }

    @Test
    void toolRequestIsRunAndTheResultIsSentBackThenModelNarrates() {
        GameEngine e = engine(15); // 15 + 2 = 17 vs 12
        FakeLLM llm = new FakeLLM().thenTool("roll_check", CHECK).thenText("Brenna heaves the log aside.");
        TurnResult r = new AgentOrchestrator(llm, e).takeTurn("I lift the fallen log");

        assertEquals("Brenna heaves the log aside.", r.narration());
        assertEquals(1, e.getHistory().size());
        assertEquals(1, r.toolActivity().size());
        assertTrue(r.toolActivity().get(0).startsWith("roll_check:"));
        assertTrue(r.toolActivity().get(0).contains("success"));

        List<ChatMessage> second = llm.calls.get(1);
        ChatMessage assistant = second.get(second.size() - 2);
        ChatMessage tool = second.get(second.size() - 1);
        assertEquals(1, assistant.toolCalls().size());
        assertEquals("tool", tool.role());
        assertEquals(assistant.toolCalls().get(0).id(), tool.toolCallId());
        assertTrue(tool.content().contains("success"));
    }

    @Test
    void anInvalidToolRequestIsReturnedAsAnErrorAndTheModelCanRetry() {
        GameEngine e = engine(15);
        FakeLLM llm = new FakeLLM()
                .thenTool("roll_check", "{\"character\":\"Nobody\",\"stat\":\"STRENGTH\"}")
                .thenTool("roll_check", CHECK)
                .thenText("Done.");
        TurnResult r = new AgentOrchestrator(llm, e).takeTurn("I try something");
        List<ChatMessage> second = llm.calls.get(1);
        assertTrue(second.get(second.size() - 1).content().startsWith("Error:"));
        assertEquals(1, e.getHistory().size()); // only the valid retry changed the game
        assertEquals("Done.", r.narration());
    }

    @Test
    void severalToolCallsInOneReplyRunInOrder() {
        GameEngine e = engine(10, 10);
        FakeLLM llm = new FakeLLM().then(new LLMResponse("", List.of(
                new ToolCall("a", "roll_check", CHECK),
                new ToolCall("b", "roll_check", "{\"character\":\"Mira\",\"stat\":\"INTELLIGENCE\"}"))))
                .thenText("Both try.");
        TurnResult r = new AgentOrchestrator(llm, e).takeTurn("We both try");
        assertEquals(2, r.toolActivity().size());
        assertTrue(r.toolActivity().get(0).contains("Brenna"));
        assertTrue(r.toolActivity().get(1).contains("Mira"));
    }

    @Test
    void aModelThatNeverStopsCallingToolsIsCutOffAndAskedToNarrate() {
        GameEngine e = engine(10, 10, 10, 10, 10, 10, 10, 10);
        FakeLLM llm = new FakeLLM();
        for (int i = 0; i < AgentOrchestrator.MAX_STEPS; i++) {
            llm.thenTool("roll_check", CHECK);
        }
        llm.thenText("Enough searching.");
        TurnResult r = new AgentOrchestrator(llm, e).takeTurn("I keep trying");
        assertEquals("Enough searching.", r.narration());
        assertEquals(AgentOrchestrator.MAX_STEPS + 1, llm.calls.size());
        assertTrue(llm.toolsOffered.get(llm.toolsOffered.size() - 1).isEmpty());
    }

    @Test
    void toolRunsPerTurnAreCapped() {
        int[] rolls = new int[20];
        java.util.Arrays.fill(rolls, 10);
        GameEngine e = engine(rolls);
        List<ToolCall> many = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) {
            many.add(new ToolCall("c" + i, "roll_check", CHECK));
        }
        FakeLLM llm = new FakeLLM().then(new LLMResponse("", many)).thenText("Whew.");
        TurnResult r = new AgentOrchestrator(llm, e).takeTurn("spam");
        assertEquals(AgentOrchestrator.MAX_TOOL_CALLS, e.getHistory().size());
        List<ChatMessage> second = llm.calls.get(1);
        assertTrue(second.get(second.size() - 1).content().contains("too many actions"));
        assertEquals(AgentOrchestrator.MAX_TOOL_CALLS, r.toolActivity().size());
    }

    @Test
    void aFullCombatRoundGoesThroughTheEngine() {
        // spawn wolf; Brenna hits (15+2=17 vs 12, d8=4+2=6); wolf hits Brenna (18 vs AC 11, d4=3)
        GameEngine e = engine(15, 4, 18, 3);
        FakeLLM llm = new FakeLLM()
                .thenTool("spawn_monster", WOLF)
                .thenTool("attack", "{\"attacker\":\"Brenna\",\"target\":\"Wolf\"}")
                .thenTool("attack", "{\"attacker\":\"Wolf\",\"target\":\"Brenna\"}")
                .thenText("Steel and fangs meet.");
        TurnResult r = new AgentOrchestrator(llm, e).takeTurn("A wolf leaps out and I strike");
        assertEquals(4, e.getState().getMonsters().get(0).getHealth());
        assertEquals(fighter.getMaxHealth() - 3, fighter.getHealth());
        assertEquals("Combat", e.getState().getPhase().name());
        assertEquals(3, r.toolActivity().size());
    }

    @Test
    void narrationAloneNeverChangesTheGame() {
        GameEngine e = engine();
        int hp = fighter.getHealth();
        new AgentOrchestrator(new FakeLLM("Brenna takes 50 damage and dies!"), e).takeTurn("I wait");
        assertEquals(hp, fighter.getHealth());
        assertTrue(fighter.isAlive());
    }

    @Test
    void endStoryMarksTheGameOver() {
        GameEngine e = engine();
        FakeLLM llm = new FakeLLM().thenTool("end_story", "{\"ending\":\"The grove blooms.\"}")
                .thenText("And so it ends.");
        TurnResult r = new AgentOrchestrator(llm, e).takeTurn("I cleanse the spring");
        assertTrue(r.gameOver());
        assertEquals("Ended", e.getState().getPhase().name());
    }

    @Test
    void turnsAreRefusedOnceTheGameIsOverOrInputIsBlank() {
        GameEngine e = engine();
        AgentOrchestrator agent = new AgentOrchestrator(new FakeLLM("x"), e);
        assertThrows(IllegalArgumentException.class, () -> agent.takeTurn("  "));
        e.concludeStory();
        assertThrows(IllegalStateException.class, () -> agent.takeTurn("hello"));
    }

    @Test
    void typographicCharactersInNarrationArePlainified() {
        GameEngine e = engine();
        TurnResult r = new AgentOrchestrator(new FakeLLM("A once‑vibrant grove—withered. “Run,” it’s said."), e)
                .takeTurn("look");
        assertEquals("A once-vibrant grove-withered. \"Run,\" it's said.", r.narration());
    }

    @Test
    void stateDescriptionListsEnemiesAndRecentEvents() {
        GameEngine e = engine(15, 4);
        ToolRegistry reg = ToolRegistry.defaultFor(e);
        reg.execute(new ToolCall("1", "spawn_monster", WOLF));
        reg.execute(new ToolCall("2", "attack", "{\"attacker\":\"Brenna\",\"target\":\"Wolf\"}"));
        String text = StateDescriber.describe(e.getState());
        assertTrue(text.contains("Phase: Combat"));
        assertTrue(text.contains("name \"Wolf\", HP 4/10"), text);
        assertTrue(text.contains("Recent game events"));
    }

    @Test
    void theFirstTurnHasNoMemorySectionAndLaterTurnsRememberEarlierOnes() {
        GameEngine e = engine();
        FakeLLM llm = new FakeLLM("A raven watches from the branches.", "The raven flies off.");
        AgentOrchestrator agent = new AgentOrchestrator(llm, e);
        agent.takeTurn("I study the trees");
        agent.takeTurn("I wave at the bird");

        assertFalse(llm.calls.get(0).get(1).content().contains("STORY SO FAR"));
        String second = llm.calls.get(1).get(1).content();
        assertTrue(second.contains("STORY SO FAR"));
        assertTrue(second.contains("I study the trees"));
        assertTrue(second.contains("A raven watches from the branches."));
        assertEquals(2, agent.getMemory().getRecentTurns().size());
    }

    @Test
    void whatTheRulesDidIsRememberedToo() {
        GameEngine e = engine(15);
        FakeLLM llm = new FakeLLM().thenTool("roll_check", CHECK).thenText("The log moves.").thenText("Next.");
        AgentOrchestrator agent = new AgentOrchestrator(llm, e);
        agent.takeTurn("I lift the log");
        agent.takeTurn("I look under it");
        assertTrue(llm.calls.get(2).get(1).content().contains("Rules: roll_check:"));
    }

    @Test
    void aFailedTurnRollsBackDamageThatAlreadyHappened() {
        GameEngine e = engine(20, 15); // a hit for damage, then the model call fails (no reply scripted)
        e.addMonster(new gamemaster.model.Monster("Wolf", 10, 12, gamemaster.model.Weapon.DAGGER));
        int before = fighter.getHealth();
        String log = String.valueOf(e.getState().getLog());
        FakeLLM llm = new FakeLLM().thenTool("attack", "{\"attacker\":\"Wolf\",\"target\":\"Brenna\"}");
        assertThrows(IllegalStateException.class, () -> new AgentOrchestrator(llm, e).takeTurn("I wait"));
        assertEquals(before, e.getState().getParty().getCharacters().get(0).getHealth());
        assertEquals(log, String.valueOf(e.getState().getLog()));
        assertEquals("You stand at the grove's edge.", e.getState().getCurrentEvent().description());
        assertEquals("Combat", e.getState().getPhase().name());
    }

    @Test
    void afterARollbackTheSameActionCanBeTriedAgain() {
        GameEngine e = engine();
        AgentOrchestrator agent = new AgentOrchestrator(new FakeLLM().thenTool("roll_check", CHECK), e);
        assertThrows(IllegalStateException.class, () -> agent.takeTurn("I push the door"));
        AgentOrchestrator again = new AgentOrchestrator(new FakeLLM("The door gives way."), e);
        assertEquals("The door gives way.", again.takeTurn("I push the door").narration());
    }

    // ---- objective and turn limit ----

    private GameEngine engineWithObjective() {
        GameEngine e = engine();
        e.setScenario(new Scenario("Grove", "A withering grove.", "Cleanse the spring at the heart of the grove."));
        return e;
    }

    @Test
    void everyTurnIsCountedAndShownToTheModelWithTheObjective() {
        GameEngine e = engineWithObjective();
        FakeLLM llm = new FakeLLM("The wind rustles.", "A bird sings.");
        AgentOrchestrator agent = new AgentOrchestrator(llm, e);
        agent.takeTurn("I look around");
        assertEquals(1, e.getState().getTurnsTaken());
        String prompt = llm.calls.get(0).get(1).content();
        assertTrue(prompt.contains("Cleanse the spring at the heart of the grove."), prompt);
        assertTrue(prompt.contains("Turn 1 of 20"), prompt);
        agent.takeTurn("I walk on");
        assertEquals(2, e.getState().getTurnsTaken());
    }

    @Test
    void aFailedTurnDoesNotUseUpAPlayerTurn() {
        GameEngine e = engineWithObjective();
        AgentOrchestrator agent = new AgentOrchestrator(new FakeLLM().thenTool("roll_check", CHECK), e);
        assertThrows(IllegalStateException.class, () -> agent.takeTurn("I push the door"));
        assertEquals(0, e.getState().getTurnsTaken());
    }

    @Test
    void theModelIsToldWhenTurnsAreRunningOutAndOnTheFinalTurn() {
        GameEngine e = engineWithObjective();
        for (int i = 0; i < 16; i++) {
            e.getState().startTurn();            // 16 turns used; the next is turn 17, so 3 remain after it
        }
        FakeLLM llm = new FakeLLM("a", "b", "c", "d");
        AgentOrchestrator agent = new AgentOrchestrator(llm, e);
        agent.takeTurn("go");
        assertTrue(llm.calls.get(0).get(1).content().contains("turns remain after this one"));
        agent.takeTurn("go");
        agent.takeTurn("go");
        agent.takeTurn("go");
        assertTrue(llm.calls.get(3).get(1).content().contains("THIS IS THE FINAL TURN"));
    }

    @Test
    void theEngineEndsTheAdventureAtTheTurnLimitEvenIfTheModelDoesNot() {
        GameEngine e = engineWithObjective();
        for (int i = 0; i < GameState.TURN_LIMIT - 1; i++) {
            e.getState().startTurn();
        }
        TurnResult r = new AgentOrchestrator(new FakeLLM("The grove stays quiet."), e).takeTurn("I wait");
        assertTrue(r.gameOver());
        assertEquals("Ended", e.getState().getPhase().name());
        assertFalse(e.getState().isObjectiveAchieved());
        assertTrue(r.narration().contains("Time has run out"), r.narration());
        assertTrue(r.toolActivity().get(r.toolActivity().size() - 1).startsWith("turn limit:"));
        assertTrue(e.getState().endingText().contains("without the objective"));
    }

    @Test
    void endingTheStoryWithTheObjectiveAchievedIsAVictory() {
        GameEngine e = engineWithObjective();
        FakeLLM llm = new FakeLLM().thenTool("end_story", "{\"ending\":\"The spring runs clear.\",\"objective_achieved\":true}")
                .thenText("The grove blooms.");
        TurnResult r = new AgentOrchestrator(llm, e).takeTurn("I cleanse the spring");
        assertTrue(r.gameOver());
        assertTrue(e.getState().isObjectiveAchieved());
        assertTrue(e.getState().endingText().startsWith("VICTORY"));
        assertTrue(r.toolActivity().get(0).contains("Objective achieved."), r.toolActivity().get(0));
    }

    @Test
    void endingWithoutAchievingTheObjectiveIsNotAVictory() {
        GameEngine e = engineWithObjective();
        FakeLLM llm = new FakeLLM().thenTool("end_story", "{\"ending\":\"They give up.\",\"objective_achieved\":false}")
                .thenText("The party leaves.");
        new AgentOrchestrator(llm, e).takeTurn("We leave");
        assertFalse(e.getState().isObjectiveAchieved());
    }

    @Test
    void aTurnRolledBackAfterTheLimitIsReachedStillEndsOnTheNextSuccess() {
        GameEngine e = engineWithObjective();
        for (int i = 0; i < GameState.TURN_LIMIT - 1; i++) {
            e.getState().startTurn();
        }
        AgentOrchestrator bad = new AgentOrchestrator(new FakeLLM().thenTool("roll_check", CHECK), e);
        assertThrows(IllegalStateException.class, () -> bad.takeTurn("x"));
        assertEquals(GameState.TURN_LIMIT - 1, e.getState().getTurnsTaken());
        assertFalse(e.checkEndCondition());
        assertTrue(new AgentOrchestrator(new FakeLLM("ok"), e).takeTurn("y").gameOver());
    }
}
