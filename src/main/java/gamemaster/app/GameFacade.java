package gamemaster.app;

import gamemaster.ai.AgentOrchestrator;
import gamemaster.ai.GeneratedScenario;
import gamemaster.ai.MemoryManager;
import gamemaster.ai.ScenarioGenerator;
import gamemaster.ai.ToolRegistry;
import gamemaster.ai.TurnResult;
import gamemaster.game.GameEngine;
import gamemaster.game.GameState;
import gamemaster.game.GameView;
import gamemaster.llm.LLMClient;
import gamemaster.model.Character;
import gamemaster.model.CharacterClass;
import gamemaster.model.CharacterFactory;
import gamemaster.model.Party;
import gamemaster.model.Stat;
import gamemaster.rules.RuleEngine;
import gamemaster.save.SaveService;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Facade: the one entry point the CLI and the GUI use. It builds and owns the engine, rules,
 * AI agent, memory and saving, and exposes a handful of plain methods, so a user interface
 * never has to know how those parts fit together.
 */
public class GameFacade {

    private final LLMClient llm;
    private final GameState state;
    private final GameEngine engine;
    private final MemoryManager memory;
    private final AgentOrchestrator agent;
    private final SaveService saves;
    private final CharacterFactory factory = new CharacterFactory();

    public GameFacade(LLMClient llm, SaveService saves, RuleEngine rules) {
        this.llm = Objects.requireNonNull(llm, "llm");
        this.saves = Objects.requireNonNull(saves, "saves");
        this.state = new GameState(new Party());
        this.engine = new GameEngine(state, Objects.requireNonNull(rules, "rules"));
        this.memory = new MemoryManager(llm);
        this.agent = new AgentOrchestrator(llm, engine, ToolRegistry.defaultFor(engine), memory);
    }

    public GameFacade(LLMClient llm, SaveService saves) {
        this(llm, saves, new RuleEngine());
    }

    // ---- party setup (only before the adventure starts) ----

    public Character addCharacter(String name, String className) {
        requireSetup();
        requireNewName(name);
        Character c = factory.createFromClass(name, CharacterClass.byName(className));
        state.getParty().addCharacter(c);
        return c;
    }

    public Character addCustomCharacter(String name, String className, Map<Stat, Integer> allocations) {
        requireSetup();
        requireNewName(name);
        Character c = factory.createCustom(name, CharacterClass.byName(className), allocations);
        state.getParty().addCharacter(c);
        return c;
    }

    public void removeCharacter(String name) {
        requireSetup();
        Character c = state.getParty().getCharacters().stream()
                .filter(x -> x.getName().equalsIgnoreCase(name == null ? "" : name.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("There is no character named \"" + name + "\"."));
        state.getParty().removeCharacter(c);
    }

    public List<Character> getParty() {
        return state.getParty().getCharacters();
    }

    // ---- playing ----

    /** Asks the AI for a scenario (optionally themed) and starts the adventure. */
    public GeneratedScenario startAdventure(String theme) {
        requireSetup();
        if (!state.getParty().isValidSize()) {
            throw new IllegalStateException("Add 1 to " + Party.MAX_SIZE + " characters before starting.");
        }
        GeneratedScenario scenario = new ScenarioGenerator(llm).generate(theme);
        scenario.applyTo(engine);
        state.addLog("The adventure begins: " + scenario.scenario().title());
        return scenario;
    }

    public TurnResult submitAction(String text) {
        requireStarted();
        return agent.takeTurn(text);
    }

    /** Undoes the last rules action (damage, fleeing). Returns false when there is nothing to undo. */
    public boolean undo() {
        requireStarted();
        return engine.undoLast();
    }

    // ---- saving ----

    public void save(String slot) {
        if (!state.getParty().isValidSize()) {
            throw new IllegalStateException("There is nothing to save yet; add your characters first.");
        }
        saves.save(slot, engine, memory);
    }

    public void load(String slot) {
        saves.load(slot, engine, memory);
    }

    public List<String> listSaves() {
        return saves.listSaves();
    }

    // ---- state for the interfaces ----

    public GameState getState() { return state; }

    public void attach(GameView view) { state.attach(view); }

    public boolean isStarted() { return state.getScenario() != null; }

    public boolean isOver() { return engine.checkEndCondition(); }

    private void requireSetup() {
        if (isStarted()) {
            throw new IllegalStateException("The party can only be changed before the adventure starts.");
        }
    }

    private void requireStarted() {
        if (!isStarted()) {
            throw new IllegalStateException("Start the adventure first.");
        }
    }

    private void requireNewName(String name) {
        if (name != null && state.getParty().getCharacters().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name.trim()))) {
            throw new IllegalArgumentException("There is already a character named \"" + name.trim() + "\".");
        }
    }
}
