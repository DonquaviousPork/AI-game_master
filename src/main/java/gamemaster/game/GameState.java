package gamemaster.game;

import gamemaster.model.Character;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Instant;
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

    // ---- Memento ----

    /** Freezes the game into plain values. The memory text belongs to the AI layer, so the caller passes it in. */
    public GameSnapshot createSnapshot(String memorySummary, List<String> memoryRecent) {
        return new GameSnapshot(
                GameSnapshot.CURRENT_VERSION,
                Instant.now().toString(),
                scenario == null ? null : scenario.title(),
                scenario == null ? null : scenario.description(),
                currentEvent == null ? null : currentEvent.description(),
                concluded,
                party.getCharacters().stream().map(GameSnapshot.CharacterData::of).toList(),
                monsters.stream().map(GameSnapshot.MonsterData::of).toList(),
                List.copyOf(log),
                memorySummary == null ? "" : memorySummary,
                memoryRecent == null ? List.of() : memoryRecent);
    }

    /**
     * Rebuilds the game from a snapshot. Everything is built and checked first, and only then swapped
     * in, so a damaged snapshot throws IllegalArgumentException and leaves the current game untouched.
     * The phase is reset to Setup; the engine moves it to the right phase afterwards.
     */
    public void restore(GameSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        List<Character> heroes = snapshot.party().stream().map(GameSnapshot.CharacterData::toCharacter).toList();
        List<Monster> enemies = snapshot.monsters().stream().map(GameSnapshot.MonsterData::toMonster).toList();
        Scenario newScenario = snapshot.scenarioTitle() == null ? null
                : new Scenario(snapshot.scenarioTitle(), snapshot.scenarioDescription());
        StoryEvent newEvent = snapshot.currentEvent() == null || snapshot.currentEvent().isBlank() ? null
                : new StoryEvent(snapshot.currentEvent());

        party.replaceMembers(heroes);   // validates the size before changing anything
        monsters.clear();
        monsters.addAll(enemies);
        scenario = newScenario;
        currentEvent = newEvent;
        concluded = snapshot.concluded();
        log.clear();
        log.addAll(snapshot.log());
        phase = new SetupPhase();
        changed();
    }
}
