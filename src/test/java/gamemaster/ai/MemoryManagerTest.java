package gamemaster.ai;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.llm.LLMClient;
import gamemaster.llm.LLMException;
import gamemaster.llm.LLMResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryManagerTest {

    private static void fill(MemoryManager m, int turns) {
        for (int i = 1; i <= turns; i++) {
            m.record("action " + i, "story " + i, List.of());
        }
    }

    @Test
    void startsEmptyWithNoPromptText() {
        MemoryManager m = new MemoryManager(new FakeLLM());
        assertEquals("", m.asPromptText());
        assertEquals("", m.getSummary());
    }

    @Test
    void recentTurnsAppearInOrderWithRulesActivity() {
        MemoryManager m = new MemoryManager(new FakeLLM());
        m.record("I attack", "The wolf yelps.", List.of("attack: Brenna hits Wolf for 6 damage."));
        m.record("I rest", "Night falls.", List.of());
        String text = m.asPromptText();
        assertTrue(text.startsWith("STORY SO FAR"));
        assertTrue(text.indexOf("I attack") < text.indexOf("I rest"));
        assertTrue(text.contains("Rules: attack: Brenna hits Wolf for 6 damage."));
        assertTrue(text.contains("Game master: The wolf yelps."));
    }

    @Test
    void noModelCallIsMadeUntilTheRecentLimitIsExceeded() {
        FakeLLM llm = new FakeLLM();
        MemoryManager m = new MemoryManager(llm);
        fill(m, MemoryManager.RECENT_LIMIT);
        assertTrue(llm.calls.isEmpty());
        assertEquals(MemoryManager.RECENT_LIMIT, m.getRecentTurns().size());
    }

    @Test
    void exceedingTheLimitFoldsTheOldestTurnsIntoTheSummary() {
        FakeLLM llm = new FakeLLM("The party fought wolves in the grove.");
        MemoryManager m = new MemoryManager(llm);
        fill(m, MemoryManager.RECENT_LIMIT + 1);

        assertEquals(1, llm.calls.size());
        assertEquals("The party fought wolves in the grove.", m.getSummary());
        assertEquals(MemoryManager.RECENT_LIMIT + 1 - MemoryManager.FOLD_COUNT, m.getRecentTurns().size());
        assertTrue(m.getRecentTurns().get(0).contains("action " + (MemoryManager.FOLD_COUNT + 1)));

        String request = llm.calls.get(0).get(1).content();
        assertTrue(request.contains("action 1") && request.contains("action " + MemoryManager.FOLD_COUNT));
        assertFalse(request.contains("action " + (MemoryManager.FOLD_COUNT + 1)));
        assertTrue(m.asPromptText().contains("Earlier notes: The party fought wolves in the grove."));
    }

    @Test
    void aLaterFoldIsGivenThePreviousSummaryToMerge() {
        FakeLLM llm = new FakeLLM("First notes.", "Merged notes.");
        MemoryManager m = new MemoryManager(llm);
        fill(m, MemoryManager.RECENT_LIMIT + 1);       // first fold
        fill(m, MemoryManager.FOLD_COUNT);             // pushes past the limit again
        assertEquals(2, llm.calls.size());
        assertTrue(llm.calls.get(1).get(1).content().contains("Old notes: First notes."));
        assertEquals("Merged notes.", m.getSummary());
    }

    @Test
    void ifTheModelFailsTheFoldedTurnsAreKeptAsPlainText() {
        LLMClient failing = (messages, tools) -> { throw new LLMException("down"); };
        MemoryManager m = new MemoryManager(failing);
        fill(m, MemoryManager.RECENT_LIMIT + 1);
        assertTrue(m.getSummary().contains("action 1"));
        assertEquals(MemoryManager.RECENT_LIMIT + 1 - MemoryManager.FOLD_COUNT, m.getRecentTurns().size());
    }

    @Test
    void theSummaryNeverExceedsItsLimitEvenIfTheModelRamblesOn() {
        String sentence = "The heroes walked on and on through the grove. ";
        FakeLLM llm = new FakeLLM(sentence.repeat(200));
        MemoryManager m = new MemoryManager(llm);
        fill(m, MemoryManager.RECENT_LIMIT + 1);
        assertTrue(m.getSummary().length() <= MemoryManager.MAX_SUMMARY_CHARS);
        assertTrue(m.getSummary().endsWith("."));
    }

    @Test
    void longNarrationAndToolTextAreTrimmedBeforeStoring() {
        MemoryManager m = new MemoryManager(new FakeLLM());
        m.record("look", "x".repeat(2000), List.of("roll_check: " + "y".repeat(2000)));
        String entry = m.getRecentTurns().get(0);
        assertTrue(entry.length() < 2000);
        assertTrue(entry.contains("..."));
    }

    @Test
    void restoreReplacesEverythingAndRoundTrips() {
        MemoryManager a = new MemoryManager(new FakeLLM());
        a.record("one", "uno", List.of());
        MemoryManager b = new MemoryManager(new FakeLLM());
        b.record("stale", "stale", List.of());
        b.restore("Old notes here.", a.getRecentTurns());
        assertEquals("Old notes here.", b.getSummary());
        assertEquals(a.getRecentTurns(), b.getRecentTurns());
        b.restore(null, List.of());
        assertEquals("", b.asPromptText());
    }

    @Test
    void foldedTextHasTypographicCharactersCleaned() {
        FakeLLM llm = new FakeLLM("A once‑vibrant grove’s spring.");
        MemoryManager m = new MemoryManager(llm);
        fill(m, MemoryManager.RECENT_LIMIT + 1);
        assertEquals("A once-vibrant grove's spring.", m.getSummary());
    }
}
