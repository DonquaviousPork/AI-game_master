package gamemaster.game;

import gamemaster.model.Monster;
import gamemaster.model.Party;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Everything the game knows right now. It is the Subject of the Observer pattern:
 * every mutator notifies the attached views, so the CLI and GUI never poll.
 * Only the engine and the actions it runs should call the mutators.
 */
public class GameState {

    private final Party party;
    private final List<Monster> monsters = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private final List<GameView> views = new ArrayList<>();
    private Scenario scenario;
    private StoryEvent currentEvent;
    private GamePhase phase = new SetupPhase();
    private boolean concluded;

    public GameState(Party party) {
        this.party = Objects.requireNonNull(party, "party");
    }

    // ---- Observer ----

    public void attach(GameView view) {
        views.add(Objects.requireNonNull(view, "view"));
    }

    public void detach(GameView view) {
        views.remove(view);
    }

    private void changed() {
        for (GameView v : new ArrayList<>(views)) {
            v.update(this);
        }
    }

    // ---- Queries ----

    public Party getParty() { return party; }
    public Scenario getScenario() { return scenario; }
    public StoryEvent getCurrentEvent() { return currentEvent; }
    public GamePhase getPhase() { return phase; }
    public boolean isConcluded() { return concluded; }
    public List<Monster> getMonsters() { return Collections.unmodifiableList(monsters); }
    public List<String> getLog() { return Collections.unmodifiableList(log); }

    public boolean hasLivingMonsters() {
        return monsters.stream().anyMatch(Monster::isAlive);
    }

    // ---- Mutators (each notifies the views) ----

    public void setScenario(Scenario scenario) {
        this.scenario = Objects.requireNonNull(scenario, "scenario");
        changed();
    }

    public void setCurrentEvent(StoryEvent event) {
        this.currentEvent = Objects.requireNonNull(event, "event");
        changed();
    }

    public void setPhase(GamePhase phase) {
        this.phase = Objects.requireNonNull(phase, "phase");
        changed();
    }

    public void addMonster(Monster monster) {
        monsters.add(Objects.requireNonNull(monster, "monster"));
        changed();
    }

    /** Replaces the whole monster list; used by undo to restore what a flee removed. */
    public void setMonsters(List<Monster> replacement) {
        monsters.clear();
        monsters.addAll(replacement);
        changed();
    }

    public void conclude() {
        concluded = true;
        changed();
    }

    public void addLog(String message) {
        log.add(Objects.requireNonNull(message, "message"));
        changed();
    }
}
