package gamemaster.ai;

import gamemaster.game.GameEngine;
import gamemaster.game.ObjectiveType;
import gamemaster.game.GameSnapshot;
import gamemaster.game.GameState;
import gamemaster.game.StoryEvent;
import gamemaster.llm.ChatMessage;
import gamemaster.llm.LLMClient;
import gamemaster.llm.LLMResponse;
import gamemaster.llm.ToolCall;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The game master agent. For each player turn it shows the model the real game state,
 * lets it request tools, runs those requests through the ToolRegistry, feeds the results
 * back, and repeats until the model narrates. It is bounded: at most MAX_STEPS rounds
 * and MAX_TOOL_CALLS tool runs per turn, so a confused model cannot loop forever or
 * flood the game. The model proposes; the registry and engine decide.
 */
public class AgentOrchestrator {

    static final int MAX_STEPS = 6;
    static final int MAX_TOOL_CALLS = 8;

    static final String SYSTEM_PROMPT = """
            You are the game master of a small fantasy text role-playing game. You narrate the story
            and decide what happens next, but the game rules are run by tools, not by you.
            Rules you must follow:
            - Never invent dice rolls, damage, hit points or whether an attempt succeeds. When a hero
              attempts something uncertain, call roll_check. When anyone attacks, call attack.
              When a hero must resist a trap, spell or other hazard, call saving_throw.
              Use only the numbers and outcomes the tools report.
            - During combat, resolve the player's action first, then make each living enemy attack once
              with the attack tool, then narrate the round.
            - Use spawn_monster to introduce a new enemy and end_story only when the story is truly over.
            - If a tool reports an Error, read it and fix your request, or narrate around it.
            - Only heroes and enemies listed in the game state exist. Use their exact names only,
              for example "David", never "David the Fighter".
            - A hero can only attack with the attacks listed for that hero in the game state. If the
              player asks a hero to use a move that is not on that hero's list (such as another class's
              attack, or "kill everyone"), do not substitute a different attack: call attack with the
              move named exactly as the player asked. The game reports that the hero tried and failed,
              and you then narrate the failed attempt.
            - The game state shows one fixed OBJECTIVE and the turn number. That objective is the only
              goal of the adventure: never add new goals, quests or mysteries, and do not stretch the
              story. If the game state says the ENGINE decides the objective, the game ends the story by
              itself the moment it is met (do not call end_story with objective_achieved true yourself;
              the game will refuse it) and you only narrate the ending in your reply. Otherwise, when the
              heroes achieve it, call end_story with objective_achieved true and narrate the ending. On the
              final turn the game ends the story if you do not. Keep each scene short and directed at the
              objective.
            - Critical hits and fumbles are decided by the dice; report them as the tool states them.
            - When you have what you need, reply with two to four sentences of narration for the player
              and, if the story is still going, end with what the heroes can do next. Do not use tools
              in your final narration.
            """;

    private final LLMClient llm;
    private final GameEngine engine;
    private final ToolRegistry tools;
    private final MemoryManager memory;

    public AgentOrchestrator(LLMClient llm, GameEngine engine, ToolRegistry tools, MemoryManager memory) {
        this.llm = Objects.requireNonNull(llm, "llm");
        this.engine = Objects.requireNonNull(engine, "engine");
        this.tools = Objects.requireNonNull(tools, "tools");
        this.memory = Objects.requireNonNull(memory, "memory");
    }

    public AgentOrchestrator(LLMClient llm, GameEngine engine) {
        this(llm, engine, ToolRegistry.defaultFor(engine), new MemoryManager(llm));
    }

    public MemoryManager getMemory() { return memory; }

    public TurnResult takeTurn(String playerInput) {
        if (playerInput == null || playerInput.isBlank()) {
            throw new IllegalArgumentException("Say what your party does.");
        }
        if (engine.checkEndCondition()) {
            throw new IllegalStateException("The game is over.");
        }

        // If anything fails part-way (a rate limit, a network drop), put the game back exactly as it was,
        // so the player is never left with damage the story never showed. Restoring swaps in new character
        // objects, so the undo history (which points at the old ones) is cleared, as it is on Load.
        GameSnapshot before = engine.getState().createSnapshot("", List.of());
        try {
            engine.getState().startTurn();      // after the rollback point, so a failed turn does not use one up
            return runTurn(playerInput);
        } catch (RuntimeException e) {
            engine.getState().restore(before);
            engine.getHistory().clear();
            engine.advancePhase();
            throw e;
        }
    }

    private TurnResult runTurn(String playerInput) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.system(SYSTEM_PROMPT));
        String remembered = memory.asPromptText();
        messages.add(ChatMessage.user((remembered.isEmpty() ? "" : remembered + "\n")
                + "CURRENT GAME STATE\n" + StateDescriber.describe(engine.getState())
                + "\nPLAYER ACTION: " + playerInput.trim()));

        List<String> activity = new ArrayList<>();
        int toolRuns = 0;
        String narration = null;

        for (int step = 1; step <= MAX_STEPS && narration == null; step++) {
            LLMResponse response = llm.chat(messages, tools.specs());
            if (!response.hasToolCalls()) {
                narration = response.content();
                break;
            }
            messages.add(ChatMessage.assistantToolCalls(response.content(), response.toolCalls()));
            for (ToolCall call : response.toolCalls()) {
                String result;
                if (toolRuns >= MAX_TOOL_CALLS) {
                    result = "Error: too many actions this turn. Narrate what has happened so far.";
                } else {
                    toolRuns++;
                    boolean wasOver = engine.checkEndCondition();
                    result = tools.execute(call);
                    activity.add(call.name() + ": " + result);
                    if (!wasOver && engine.getState().isConcluded()) {
                        result += " [GAME: the story has just ended"
                                + (engine.getState().isObjectiveAchieved() ? " as a victory" : "")
                                + ". Reply with a short closing scene (two to four sentences) that wraps up the "
                                + "adventure. Do not use more tools.]";
                    }
                }
                messages.add(ChatMessage.toolResult(call.id(), result));
            }
        }

        if (narration == null || narration.isBlank()) {
            // The model kept asking for tools (or said nothing): make it wrap up without tools.
            messages.add(ChatMessage.user("Stop using tools now and narrate what happened for the player."));
            narration = llm.chat(messages, List.of()).content();
        }

        narration = TextCleaner.clean(narration);
        var state = engine.getState();
        if (!engine.checkEndCondition() && state.getTurnsTaken() >= state.getTurnLimit()) {
            // The engine, not the model, guarantees the adventure ends.
            state.addLog("The turn limit was reached.");
            boolean survived = state.getScenario() != null && state.getScenario().type() == ObjectiveType.SURVIVE;
            engine.concludeStory(survived);
            activity.add("turn limit: The " + state.getTurnLimit() + "-turn limit was reached and the adventure ended.");
            narration = narration + (survived
                    ? "\n\nThe time has passed and the heroes held out. The objective is achieved."
                    : "\n\nTime has run out. The adventure ends here without the objective being achieved.");
        }
        if (!narration.isBlank()) {
            engine.setCurrentEvent(new StoryEvent(narration));
        }
        memory.record(playerInput, narration, activity);
        return new TurnResult(narration, activity, engine.checkEndCondition());
    }
}
