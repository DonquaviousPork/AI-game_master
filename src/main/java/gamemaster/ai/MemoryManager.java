package gamemaster.ai;

import gamemaster.llm.ChatMessage;
import gamemaster.llm.LLMClient;
import gamemaster.llm.LLMException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Story memory for the game master. The last few turns are kept word for word; once there are
 * too many, the oldest are folded into a short running summary written by the model itself.
 * Both pieces go into every prompt, so the story stays consistent over a long game while the
 * prompt stays small (the free model has a tight tokens-per-minute limit).
 * If the model cannot be reached when folding, a plain truncated fallback keeps the game running.
 */
public class MemoryManager {

    static final int RECENT_LIMIT = 8;      // fold when more than this many turns are stored
    static final int FOLD_COUNT = 4;        // how many of the oldest turns to fold at once
    static final int MAX_SUMMARY_CHARS = 1500;
    static final int MAX_NARRATION_CHARS = 400;
    static final int MAX_ACTIVITY_CHARS = 160;

    private static final String SUMMARY_PROMPT = """
            You keep the story notes for a fantasy text role-playing game. Merge the old notes with the
            new events into updated notes of at most 200 words. Keep names, places, discoveries, promises,
            injuries, items and unresolved threats. Do not invent anything that is not in the text.
            Reply with the notes only.
            """;

    private final LLMClient llm;
    private final List<String> recent = new ArrayList<>();
    private String summary = "";

    public MemoryManager(LLMClient llm) {
        this.llm = Objects.requireNonNull(llm, "llm");
    }

    /** Stores one finished turn; may make one model call to fold older turns into the summary. */
    public void record(String playerInput, String narration, List<String> toolActivity) {
        StringBuilder entry = new StringBuilder("Player: ").append(playerInput.trim());
        if (!toolActivity.isEmpty()) {
            entry.append(" | Rules: ");
            entry.append(String.join("; ", toolActivity.stream().map(a -> cut(a, MAX_ACTIVITY_CHARS)).toList()));
        }
        entry.append(" | Game master: ").append(cut(narration, MAX_NARRATION_CHARS));
        recent.add(entry.toString());
        if (recent.size() > RECENT_LIMIT) {
            fold();
        }
    }

    /** The text added to each prompt; empty when nothing has happened yet. */
    public String asPromptText() {
        if (summary.isBlank() && recent.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("STORY SO FAR\n");
        if (!summary.isBlank()) {
            sb.append("Earlier notes: ").append(summary).append('\n');
        }
        if (!recent.isEmpty()) {
            sb.append("Recent turns:\n");
            recent.forEach(r -> sb.append("  - ").append(r).append('\n'));
        }
        return sb.toString();
    }

    public String getSummary() { return summary; }

    public List<String> getRecentTurns() { return List.copyOf(recent); }

    /** Replaces the memory, used when a saved game is loaded. */
    public void restore(String savedSummary, List<String> savedRecent) {
        summary = savedSummary == null ? "" : cutAtSentence(savedSummary, MAX_SUMMARY_CHARS);
        recent.clear();
        recent.addAll(savedRecent);
    }

    private void fold() {
        List<String> oldest = new ArrayList<>(recent.subList(0, FOLD_COUNT));
        String updated;
        try {
            String request = (summary.isBlank() ? "" : "Old notes: " + summary + "\n")
                    + "New events:\n" + String.join("\n", oldest);
            updated = llm.complete(List.of(ChatMessage.system(SUMMARY_PROMPT), ChatMessage.user(request)));
        } catch (LLMException e) {
            // Fallback: keep the information as plain text rather than losing it.
            updated = (summary.isBlank() ? "" : summary + " ") + String.join(" ", oldest);
        }
        recent.subList(0, FOLD_COUNT).clear();
        summary = cutAtSentence(TextCleaner.clean(updated), MAX_SUMMARY_CHARS);
    }

    private static String cut(String text, int max) {
        String t = TextCleaner.clean(text);
        return t.length() <= max ? t : t.substring(0, max - 3) + "...";
    }

    /** Keeps the start of the text up to max characters, ending at a full sentence when possible. */
    private static String cutAtSentence(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        String head = text.substring(0, max);
        int lastStop = Math.max(head.lastIndexOf(". "), head.lastIndexOf('.'));
        return lastStop > max / 2 ? head.substring(0, lastStop + 1) : head;
    }
}
