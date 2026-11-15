package gamemaster.game;

import gamemaster.model.Character;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private int turnsTaken;
    private boolean objectiveAchieved;
    private int milestoneIndex, beatSuccesses, beatKills, beatTurns, beatFailures;
    private int checkFailStreak, spawnsThisTurn, healsUsed;
    /** Active effects by combatant name; the number is the turns left. */
    private final Map<String, Map<StatusKind, Integer>> statuses = new LinkedHashMap<>();
    /** Enemies that have already used an effect this turn (each may use one per turn). */
    private final java.util.Set<String> effectUsers = new java.util.HashSet<>();

    /** Healing Light can be cast this many times in one adventure. */
    public static final int HEALS_PER_ADVENTURE = 2;
    /** After this many failed checks in a row, the next one is easier. */
    public static final int FAILS_BEFORE_EASIER = 2;
    /** A challenge act ends after this many failed checks (the party pushes through). */
    public static final int FAILS_TO_PUSH_THROUGH = 3;

    /** An adventure lasts this many player turns; the engine ends it when they are used up. */
    public static final int TURN_LIMIT = 20;
    private int turnLimit = TURN_LIMIT;
    private boolean easyWin;

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
    public int getTurnsTaken() { return turnsTaken; }
    public int turnsLeft() { return Math.max(0, turnLimit - turnsTaken); }
    public int getTurnLimit() { return turnLimit; }

    /** True in demo mode: defeating the single weak enemy achieves the objective. Not saved with the game. */
    public boolean isEasyWin() { return easyWin; }
    public void setEasyWin(boolean on) { this.easyWin = on; }

    /** Sets how many turns the adventure lasts (20 normally; a shorter limit is used for demos). */
    public void setTurnLimit(int turns) {
        if (turns < 1 || turns > TURN_LIMIT) {
            throw new IllegalArgumentException("The turn limit must be between 1 and " + TURN_LIMIT + ".");
        }
        this.turnLimit = turns;
    }
    public boolean isObjectiveAchieved() { return objectiveAchieved; }

    // ---- Story acts ----

    private List<Milestone> acts() { return scenario == null ? List.of() : scenario.milestones(); }

    /** The act the party is in, or null when there are no acts or all are done (the final showdown). */
    public Milestone currentMilestone() {
        return milestoneIndex < acts().size() ? acts().get(milestoneIndex) : null;
    }

    public int getMilestoneIndex() { return milestoneIndex; }

    /** True when the story has no acts, or every act is finished: the final enemy may appear and be fought. */
    public boolean inFinale() { return milestoneIndex >= acts().size(); }

    public int getBeatTurns() { return beatTurns; }

    /** A short line such as "Act 2 of 3: The Cold Flame" for the screen and the CLI; empty when there are no acts. */
    public String progressText() {
        if (acts().isEmpty()) {
            return "";
        }
        int total = acts().size() + 1;
        return inFinale() ? "Act " + total + " of " + total + ": the showdown"
                : "Act " + (milestoneIndex + 1) + " of " + total + ": " + currentMilestone().title();
    }

    /** Called by the engine after a roll succeeded. */
    public void noteSuccess() { beatSuccesses++; }

    /** Called by the engine after every ability check: tracks the run of failures that makes the next check easier. */
    public void noteCheck(boolean success) {
        if (success) {
            checkFailStreak = 0;
        } else {
            checkFailStreak++;
            beatFailures++;
        }
    }

    public int getCheckFailStreak() { return checkFailStreak; }

    public int getBeatFailures() { return beatFailures; }

    public int getSpawnsThisTurn() { return spawnsThisTurn; }

    public void noteSpawn() { spawnsThisTurn++; }

    public int healsLeft() { return Math.max(0, HEALS_PER_ADVENTURE - healsUsed); }

    public int getHealsUsed() { return healsUsed; }

    public void noteHeal() { healsUsed++; }

    /** Gives a Healing Light use back (used when the cast is undone). */
    public void undoHeal() { healsUsed = Math.max(0, healsUsed - 1); }

    /** Called by the engine after enemies were defeated. */
    public void noteKills(int count) { beatKills += count; }

    /** Moves on to the next act and starts its counters from zero. */
    public void advanceMilestone() {
        milestoneIndex++;
        beatSuccesses = 0;
        beatKills = 0;
        beatTurns = 0;
        beatFailures = 0;
        statuses.clear();
        effectUsers.clear();
        changed();
    }

    /** True when the current act's condition has been seen by the engine. */
    public boolean currentMilestoneDone() {
        Milestone m = currentMilestone();
        if (m == null) {
            return false;
        }
        return m.kind() == MilestoneKind.CHALLENGE ? beatSuccesses >= 1 || beatFailures >= FAILS_TO_PUSH_THROUGH
                : beatKills >= 1;
    }

    public List<Monster> getMonsters() { return Collections.unmodifiableList(monsters); }
    public List<String> getLog() { return Collections.unmodifiableList(log); }

    public boolean hasLivingMonsters() {
        return monsters.stream().anyMatch(Monster::isAlive);
    }

    // ---- Mutators (each notifies the views) ----

    public void setScenario(Scenario scenario) {
        this.scenario = Objects.requireNonNull(scenario, "scenario");
        this.turnsTaken = 0;
        this.objectiveAchieved = false;
        this.milestoneIndex = 0;
        this.beatSuccesses = 0;
        this.beatKills = 0;
        this.beatTurns = 0;
        this.beatFailures = 0;
        this.checkFailStreak = 0;
        this.spawnsThisTurn = 0;
        this.healsUsed = 0;
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

    /** How the story ended (victory, defeat or time ran out); empty while it is still going. */
    public String endingText() {
        if (!concluded && !party.isDefeated()) {
            return "";
        }
        if (party.isDefeated()) {
            return "DEFEAT: the party has fallen.";
        }
        return objectiveAchieved ? "VICTORY: the objective was achieved."
                : "The adventure ended without the objective being achieved.";
    }

    /** Counts one player turn. Called by the AI layer at the start of each turn. */
    public void startTurn() {
        turnsTaken++;
        beatTurns++;
        spawnsThisTurn = 0;
        effectUsers.clear();
        changed();
    }

    // ---- status effects ----

    /** Turns left of an effect on this combatant, 0 when it is not affected. */
    public int statusTurns(String name, StatusKind kind) {
        Map<StatusKind, Integer> m = statuses.get(name);
        return m == null ? 0 : m.getOrDefault(kind, 0);
    }

    /** Puts the effect on a combatant for {@link StatusKind#DURATION} turns; returns the turns it had before (0 if none). */
    public int applyStatus(String name, StatusKind kind) {
        int before = statusTurns(name, kind);
        setStatusTurns(name, kind, StatusKind.DURATION);
        return before;
    }

    /** Sets the turns left (0 removes the effect). Used by undo and by the end-of-turn count. */
    public void setStatusTurns(String name, StatusKind kind, int turns) {
        if (turns <= 0) {
            Map<StatusKind, Integer> m = statuses.get(name);
            if (m != null) {
                m.remove(kind);
                if (m.isEmpty()) {
                    statuses.remove(name);
                }
            }
        } else {
            statuses.computeIfAbsent(name, k -> new java.util.EnumMap<>(StatusKind.class)).put(kind, turns);
        }
        changed();
    }

    /** A copy of the effects on one combatant, in a fixed order. */
    public Map<StatusKind, Integer> statusesOf(String name) {
        Map<StatusKind, Integer> m = statuses.get(name);
        return m == null ? Map.of() : new java.util.EnumMap<>(m);
    }

    public List<String> statusNames() { return List.copyOf(statuses.keySet()); }

    public void clearStatuses() {
        if (!statuses.isEmpty()) {
            statuses.clear();
            changed();
        }
    }

    /** True the first time an enemy asks to use an effect in a turn; false afterwards. */
    public boolean claimEffect(String name) { return effectUsers.add(name); }

    public void releaseEffect(String name) { effectUsers.remove(name); }

    public void conclude() {
        conclude(false);
    }

    /** Ends the story; {@code achieved} records whether the objective was met (a win) or not. */
    public void conclude(boolean achieved) {
        concluded = true;
        objectiveAchieved = achieved;
        changed();
    }

    public void addLog(String message) {
        log.add(Objects.requireNonNull(message, "message"));
        changed();
    }

    /** Back to an empty Setup state: no heroes, enemies, scenario or log. Used by "return to the main menu". */
    public void reset() {
        party.clear();
        monsters.clear();
        scenario = null;
        currentEvent = null;
        concluded = false;
        turnsTaken = 0;
        objectiveAchieved = false;
        milestoneIndex = 0;
        beatSuccesses = 0;
        beatKills = 0;
        beatTurns = 0;
        beatFailures = 0;
        checkFailStreak = 0;
        spawnsThisTurn = 0;
        healsUsed = 0;
        log.clear();
        phase = new SetupPhase();
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
                memoryRecent == null ? List.of() : memoryRecent,
                scenario == null ? "" : scenario.objective(),
                turnsTaken,
                objectiveAchieved,
                scenario == null ? "NARRATIVE" : scenario.type().name(),
                scenario == null ? "" : scenario.target(),
                acts().stream().map(GameSnapshot.MilestoneData::of).toList(),
                milestoneIndex, beatSuccesses, beatKills, beatTurns, beatFailures, healsUsed, statusLines());
    }

    private List<String> statusLines() {
        List<String> out = new java.util.ArrayList<>();
        statuses.forEach((name, m) -> m.forEach((k, t) -> out.add(name + "|" + k.name() + "|" + t)));
        return out;
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
                : new Scenario(snapshot.scenarioTitle(), snapshot.scenarioDescription(), snapshot.objective(),
                        ObjectiveType.parse(snapshot.objectiveType()), snapshot.objectiveTarget(),
                        snapshot.milestones().stream().map(GameSnapshot.MilestoneData::toMilestone).toList());
        StoryEvent newEvent = snapshot.currentEvent() == null || snapshot.currentEvent().isBlank() ? null
                : new StoryEvent(snapshot.currentEvent());

        party.replaceMembers(heroes);   // validates the size before changing anything
        monsters.clear();
        monsters.addAll(enemies);
        scenario = newScenario;
        currentEvent = newEvent;
        concluded = snapshot.concluded();
        turnsTaken = snapshot.turnsTaken();
        objectiveAchieved = snapshot.objectiveAchieved();
        milestoneIndex = Math.min(snapshot.milestoneIndex(), newScenario == null ? 0 : newScenario.milestones().size());
        beatSuccesses = snapshot.beatSuccesses();
        beatKills = snapshot.beatKills();
        beatTurns = snapshot.beatTurns();
        beatFailures = snapshot.beatFailures();
        healsUsed = Math.min(snapshot.healsUsed(), HEALS_PER_ADVENTURE);
        checkFailStreak = 0;
        spawnsThisTurn = 0;
        effectUsers.clear();
        statuses.clear();
        for (String line : snapshot.statuses()) {
            String[] parts = line.split("\\|");
            if (parts.length == 3) {
                try {
                    StatusKind.parse(parts[1]).ifPresent(k -> statuses
                            .computeIfAbsent(parts[0], n -> new java.util.EnumMap<>(StatusKind.class))
                            .put(k, Math.min(StatusKind.DURATION, Integer.parseInt(parts[2].trim()))));
                } catch (NumberFormatException ignored) {
                    // a damaged effect line is skipped; the rest of the save still loads
                }
            }
        }
        log.clear();
        log.addAll(snapshot.log());
        phase = new SetupPhase();
        changed();
    }
}
