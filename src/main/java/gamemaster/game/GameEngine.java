package gamemaster.game;

import gamemaster.model.Monster;
import gamemaster.rules.RuleEngine;
import java.util.Objects;

/**
 * Owns the rules loop: validates a requested action against the current phase,
 * runs it, records it, then moves the game to whatever phase the new state demands.
 * Anything the AI proposes reaches the game only through this class.
 */
public class GameEngine {

    private final GameState state;
    private final RuleEngine rules;
    private final ActionHistory history = new ActionHistory();

    public GameEngine(GameState state, RuleEngine rules) {
        this.state = Objects.requireNonNull(state, "state");
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    public GameState getState() { return state; }
    public ActionHistory getHistory() { return history; }

    public void setScenario(Scenario scenario) {
        state.setScenario(scenario);
        advancePhase();
    }

    public void setCurrentEvent(StoryEvent event) {
        state.setCurrentEvent(event);
    }

    /** Adds an enemy; if the game is running this starts combat. */
    public void addMonster(Monster monster) {
        state.addMonster(monster);
        advancePhase();
    }

    /** Throws IllegalStateException if the current phase does not allow this kind of action. */
    public void validate(Action action) {
        Objects.requireNonNull(action, "action");
        GamePhase phase = state.getPhase();
        if (!phase.allows(action.kind())) {
            throw new IllegalStateException(action.kind() + " is not allowed during the "
                    + phase.name() + " phase.");
        }
    }

    public ActionResult resolveAction(Action action) {
        validate(action);
        ActionResult result = action.execute(state, rules);
        history.record(action);
        state.addLog(result.summary());
        advancePhase();
        return result;
    }

    /** Undoes the last action's state change. Returns false when there was nothing to undo. */
    public boolean undoLast() {
        if (history.undoLast(state).isEmpty()) {
            return false;
        }
        state.addLog("Last action undone.");
        advancePhase();
        return true;
    }

    /** Ends the story by choice (the AI or the player decides it is finished). */
    public void concludeStory() {
        state.conclude();
        advancePhase();
    }

    public boolean checkEndCondition() {
        return state.isConcluded() || state.getParty().isDefeated();
    }

    /** Moves through phases until the state no longer asks for a change. */
    public void advancePhase() {
        GamePhase next = state.getPhase().next(state);
        while (!next.name().equals(state.getPhase().name())) {
            state.setPhase(next);
            next = state.getPhase().next(state);
        }
    }
}
