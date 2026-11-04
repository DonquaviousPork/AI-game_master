package gamemaster.cli;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.ai.FakeLLM;
import gamemaster.app.GameFacade;
import gamemaster.llm.LLMClient;
import gamemaster.llm.LLMException;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import gamemaster.save.SaveRepository;
import gamemaster.save.SaveService;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CliSessionTest {

    private static final String SCENARIO = """
            {"title":"The Hollow Crypt","description":"A cold wind blows from an old crypt.",
             "opening_event":"A skeleton rises from a tomb.",
             "monsters":[{"name":"Skeleton","health":9,"armor_class":12,"weapon":"dagger"}]}""";

    private Path saveDir;

    private String run(LLMClient llm, String input, int... rolls) throws Exception {
        saveDir = saveDir == null ? Files.createTempDirectory("cli") : saveDir;
        GameFacade game = new GameFacade(llm, new SaveService(new SaveRepository(saveDir)),
                new RuleEngine(new FixedDice(rolls)));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new CliSession(game, new BufferedReader(new StringReader(input)),
                new PrintStream(bytes, true, StandardCharsets.UTF_8)).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void aFullSessionFromSetupToSave() throws Exception {
        FakeLLM llm = new FakeLLM(SCENARIO)
                .thenTool("attack", "{\"attacker\":\"Brenna\",\"target\":\"Skeleton\"}")
                .thenText("Brenna's blade bites deep.");
        String out = run(llm, String.join("\n",
                "/add Fighter Brenna", "/add Wizard Mira", "/party", "/start haunted crypt",
                "I attack the skeleton", "/status", "/save run1", "/saves", "/quit"), 15, 4);

        assertTrue(out.contains("Added Brenna the Fighter"), out);
        assertTrue(out.contains("*** The Hollow Crypt ***"), out);
        assertTrue(out.contains("Enemy: Skeleton (HP 9/9, AC 12)"), out);
        assertTrue(out.contains("Brenna's blade bites deep."), out);
        assertTrue(out.contains("[rules] attack:"), out);
        assertTrue(out.contains("Skeleton 3/9"), out);
        assertTrue(out.contains("Saved as \"run1\"."), out);
        assertTrue(out.contains("Saved games: run1"), out);
        assertTrue(out.trim().endsWith("Goodbye."), out);
        assertTrue(Files.isRegularFile(saveDir.resolve("run1.json")));
    }

    @Test
    void customCharacterReadsAPointsLine() throws Exception {
        String out = run(new FakeLLM(), String.join("\n",
                "/custom Rogue Kest", "DEX 7 STR 3 CON 4 INT 2 WIS 2 CHA 2", "/party", "/quit"));
        assertTrue(out.contains("Added Kest the Rogue"), out);
        assertTrue(out.contains("DEX 15"), out);
    }

    @Test
    void customCharacterWithTheWrongPointTotalShowsTheRuleAndContinues() throws Exception {
        String out = run(new FakeLLM(), "/custom Rogue Kest\nSTR 3\n/party\n/quit");
        assertTrue(out.contains("Error: You must spend exactly 20 points"), out);
        assertTrue(out.contains("Your party is empty."), out);
    }

    @Test
    void mistakesPrintErrorsAndNeverEndTheSession() throws Exception {
        String out = run(new FakeLLM(), String.join("\n",
                "hello", "/add Paladin Bob", "/add Fighter", "/remove Nobody", "/start", "/fly", "/undo",
                "/save", "/load ghost", "/quit"));
        assertTrue(out.contains("Add characters with /add"), out);
        assertTrue(out.contains("Error: Unknown character class"), out);
        assertTrue(out.contains("Usage: /add"), out);
        assertTrue(out.contains("Error: There is no character named \"Nobody\"."), out);
        assertTrue(out.contains("Error: Add 1 to 4 characters before starting."), out);
        assertTrue(out.contains("Unknown command /fly"), out);
        assertTrue(out.contains("Error: Start the adventure first."), out);
        assertTrue(out.contains("Usage: /save <name>"), out);
        assertTrue(out.contains("There is no saved game named \"ghost\"."), out);
        assertTrue(out.trim().endsWith("Goodbye."), out);
    }

    @Test
    void aModelFailureIsReportedAndThePlayerCanTryAgain() throws Exception {
        int[] calls = {0};
        LLMClient flaky = (messages, tools) -> {
            if (calls[0]++ == 0) {
                throw new LLMException("Rate limit reached (HTTP 429). Wait a minute and try again.");
            }
            return gamemaster.llm.LLMResponse.text(SCENARIO);
        };
        String out = run(flaky, "/add Fighter Brenna\n/start\n/start\n/quit");
        assertTrue(out.contains("Error: Rate limit reached"), out);
        assertTrue(out.contains("*** The Hollow Crypt ***"), out);
    }

    @Test
    void undoAfterAnAttackRestoresHealth() throws Exception {
        FakeLLM llm = new FakeLLM(SCENARIO)
                .thenTool("attack", "{\"attacker\":\"Brenna\",\"target\":\"Skeleton\"}").thenText("Hit.");
        String out = run(llm, "/add Fighter Brenna\n/start\nattack\n/undo\n/undo\n/status\n/quit", 15, 4);
        assertTrue(out.contains("Last action undone"), out);
        assertTrue(out.contains("Nothing to undo."), out);
        assertTrue(out.contains("Enemy: Skeleton (HP 9/9, AC 12)"), out);
    }

    @Test
    void aSavedGameCanBeLoadedInAnotherSession() throws Exception {
        saveDir = Files.createTempDirectory("cli");
        FakeLLM first = new FakeLLM(SCENARIO);
        run(first, "/add Fighter Brenna\n/start\n/save keep\n/quit");
        String out = run(new FakeLLM(), "/load keep\n/quit");
        assertTrue(out.contains("Loaded \"keep\"."), out);
        assertTrue(out.contains("Adventure: The Hollow Crypt   Phase: Combat"), out);
        assertTrue(out.contains("Brenna the Fighter"), out);
    }

    @Test
    void theStoryEndingIsAnnouncedAndFurtherTurnsAreRefused() throws Exception {
        FakeLLM llm = new FakeLLM(SCENARIO).thenTool("end_story", "{\"ending\":\"Peace returns.\"}")
                .thenText("And so it ends.");
        String out = run(llm, "/add Fighter Brenna\n/start\nI finish it\nagain\n/quit");
        assertTrue(out.contains("*** The story has ended. ***"), out);
        assertTrue(out.contains("The story has ended. Use /load"), out);
    }

    @Test
    void blankLinesAreIgnoredAndEndOfInputEndsCleanly() throws Exception {
        String out = run(new FakeLLM(), "\n   \n/help\n");
        assertTrue(out.contains("/start [theme]"), out);
        assertTrue(out.trim().endsWith("Goodbye."), out);
    }

    @Test
    void longTextIsWrapped() {
        String wrapped = CliSession.wrap("word ".repeat(60));
        for (String line : wrapped.split("\n")) {
            assertTrue(line.length() <= CliSession.WIDTH, line);
        }
        assertEquals("", CliSession.wrap(""));
    }
}
