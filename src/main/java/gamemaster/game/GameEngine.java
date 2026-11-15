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
        long deadBefore = state.getMonsters().stream().filter(m -> !m.isAlive()).count();
        ActionResult result = action.execute(state, rules);
        history.record(action);
        state.addLog(result.summary());
        advancePhase();
        long defeated = state.getMonsters().stream().filter(m -> !m.isAlive()).count() - deadBefore;
        if (defeated > 0) {
            state.noteKills((int) defeated);
        }
        if (result.success()) {
            state.noteSuccess();
        }
        if (action.kind() == ActionKind.ABILITY_CHECK) {
            state.noteCheck(result.success());
        }
        checkMilestone();
        checkObjective();
        return result;
    }

    /**
     * True when the engine's own state shows the objective is met. Only the typed objectives are decided here;
     * a NARRATIVE objective is never met by the engine.
     */
    public boolean objectiveMet() {
        Scenario scenario = state.getScenario();
        if (scenario == null || state.getParty().isDefeated()) {
            return false;
        }
        if (!state.inFinale()) {
            return false;   // the showdown only counts once every act before it is finished
        }
        var monsters = state.getMonsters();
        return switch (scenario.type()) {
            case DEFEAT_TARGET -> monsters.stream()
                    .anyMatch(m -> m.getName().equalsIgnoreCase(scenario.target())) && monsters.stream()
                    .filter(m -> m.getName().equalsIgnoreCase(scenario.target())).noneMatch(m -> m.isAlive());
            case DEFEAT_ALL -> !monsters.isEmpty() && monsters.stream().noneMatch(m -> m.isAlive());
            case SURVIVE, NARRATIVE -> false;
        };
    }

    /** Moves the story on to the next act when the engine has seen what the current act needs. */
    private void checkMilestone() {
        if (!state.isConcluded() && state.currentMilestoneDone()) {
            var done = state.currentMilestone();
            boolean pushed = state.getBeatFailures() >= GameState.FAILS_TO_PUSH_THROUGH
                    && done.kind() == MilestoneKind.CHALLENGE;
            state.advanceMilestone();
            state.addLog("Act complete: " + done.title() + (pushed ? " (the party pushes through despite setbacks)" : "")
                    + (state.inFinale() ? " - the showdown begins." : "."));
        }
    }

    /** Ends the story as a victory the moment a typed objective is met. */
    private void checkObjective() {
        if (!state.isConcluded() && objectiveMet()) {
            state.addLog("Objective achieved: " + state.getScenario().objective());
            concludeStory(true);
        }
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
        concludeStory(false);
    }

    /** Ends the story and records whether the objective was achieved. */
    public void concludeStory(boolean objectiveAchieved) {
        state.conclude(objectiveAchieved);
        advancePhase();
    }

    /** Loads a saved game into the current state, drops the undo history and moves to the right phase. */
    public void load(GameSnapshot snapshot) {
        state.restore(snapshot);
        history.clear();
        advancePhase();
    }

    /** Abandons the current game and returns to an empty Setup state; the undo history is dropped as well. */
    public void reset() {
        state.reset();
        history.clear();
    }

    public boolean checkEndCondition() {
        return state.isConcluded() || state.getParty().isDefeated();
    }

    /**
     * Start of a player turn: effects do their work and count down. Burn deals its damage; every effect loses a turn
     * and ends at zero. Returns one line per thing that happened, for the log and the turn's tool activity.
     */
    public java.util.List<String> tickStatuses() {
        java.util.List<String> out = new java.util.ArrayList<>();
        long deadBefore = state.getMonsters().stream().filter(m -> !m.isAlive()).count();
        for (String name : state.statusNames()) {
            var who = java.util.stream.Stream.concat(
                    state.getParty().getCharacters().stream().map(c -> (gamemaster.model.Combatant) c),
                    state.getMonsters().stream().map(m -> (gamemaster.model.Combatant) m))
                    .filter(c -> c.getName().equals(name)).findFirst().orElse(null);
            if (who == null || !who.isAlive()) {
                state.setStatusTurns(name, StatusKind.BURN, 0);
                state.setStatusTurns(name, StatusKind.CHILL, 0);
                state.setStatusTurns(name, StatusKind.WEAKEN, 0);
                continue;
            }
            for (var entry : state.statusesOf(name).entrySet()) {
                StatusKind kind = entry.getKey();
                if (kind == StatusKind.BURN && who.isAlive()) {
                    int before = who.getHealth();
                    who.applyDamage(StatusKind.BURN_DAMAGE);
                    String line = name + " takes " + (before - who.getHealth()) + " burn damage ("
                            + who.getHealth() + "/" + who.getMaxHealth() + " HP)."
                            + (who.isAlive() ? "" : " " + name + " is defeated.");
                    out.add(line);
                }
                int left = entry.getValue() - 1;
                state.setStatusTurns(name, kind, left);
                if (left <= 0) {
                    out.add(name + " is no longer " + kind.adjective() + ".");
                }
            }
        }
        out.forEach(state::addLog);
        advancePhase();
        long defeated = state.getMonsters().stream().filter(m -> !m.isAlive()).count() - deadBefore;
        if (defeated > 0) {
            state.noteKills((int) defeated);
        }
        checkMilestone();
        checkObjective();
        return out;
    }

    /** Moves through phases until the state no longer asks for a change. */
    public void advancePhase() {
        GamePhase next = state.getPhase().next(state);
        while (!next.name().equals(state.getPhase().name())) {
            state.setPhase(next);
            next = state.getPhase().next(state);
        }
        if (!state.hasLivingMonsters()) {
            state.clearStatuses();      // effects only last while a fight does
        }
    }
}
