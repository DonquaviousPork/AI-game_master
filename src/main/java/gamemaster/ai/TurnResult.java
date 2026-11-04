package gamemaster.ai;

import java.util.List;

/**
 * The outcome of one player turn: what the game master said, what the game
 * actually did (one line per tool run), and whether the game is now over.
 */
public record TurnResult(String narration, List<String> toolActivity, boolean gameOver) {
    public TurnResult {
        toolActivity = List.copyOf(toolActivity);
    }
}
