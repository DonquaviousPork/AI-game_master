package gamemaster.ai;

import gamemaster.game.GameState;
import gamemaster.model.Character;
import gamemaster.model.Monster;
import java.util.List;

/**
 * Turns the real game state into text for the model. The model is told the truth about
 * health and phase every turn, so it never has to remember or guess numbers.
 */
final class StateDescriber {

    static final int RECENT_LOG_LINES = 5;

    private StateDescriber() {}

    static String describe(GameState state) {
        StringBuilder sb = new StringBuilder();
        if (state.getScenario() != null) {
            sb.append("Adventure: ").append(state.getScenario().title()).append(" - ")
                    .append(state.getScenario().description()).append('\n');
        }
        sb.append("Phase: ").append(state.getPhase().name()).append('\n');
        sb.append("Party:\n");
        for (Character c : state.getParty().getCharacters()) {
            sb.append("  - ").append(c).append(c.isAlive() ? "" : " [DEFEATED]").append('\n');
        }
        List<Monster> monsters = state.getMonsters();
        if (monsters.isEmpty()) {
            sb.append("Enemies: none\n");
        } else {
            sb.append("Enemies:\n");
            for (Monster m : monsters) {
                sb.append("  - ").append(m).append(m.isAlive() ? "" : " [DEFEATED]").append('\n');
            }
        }
        if (state.getCurrentEvent() != null) {
            sb.append("Current scene: ").append(state.getCurrentEvent().description()).append('\n');
        }
        List<String> log = state.getLog();
        if (!log.isEmpty()) {
            sb.append("Recent game events:\n");
            for (String line : log.subList(Math.max(0, log.size() - RECENT_LOG_LINES), log.size())) {
                sb.append("  - ").append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
