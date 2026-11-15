package gamemaster.ai;

import com.google.gson.JsonObject;
import gamemaster.game.ActionKind;
import gamemaster.game.GameEngine;
import gamemaster.llm.ToolSpec;
import java.util.Objects;

/** Tool "end_story": the AI declares the story concluded; the engine moves the game to its Ended phase. */
public class EndStoryTool implements Tool {

    private final GameEngine engine;

    public EndStoryTool(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("end_story",
                "End the adventure. Call it as soon as the heroes achieve the objective shown in the game state, "
                        + "or on the final turn. It is refused before the final turn if the objective is not met.",
                Schema.object()
                        .string("ending", "One or two sentences describing how the story ends", true)
                        .bool("objective_achieved", "true only if the heroes actually achieved the objective", true)
                        .build());
    }

    @Override
    public String execute(JsonObject args) {
        String ending = JsonFields.text(args, "ending");
        if (!engine.getState().getPhase().allows(ActionKind.ABILITY_CHECK)) {
            throw new IllegalStateException("The story cannot end during the "
                    + engine.getState().getPhase().name() + " phase.");
        }
        boolean achieved = args.has("objective_achieved") && !args.get("objective_achieved").isJsonNull()
                && args.get("objective_achieved").getAsBoolean();
        var scenario = engine.getState().getScenario();
        if (achieved && scenario != null && scenario.type().engineDecides() && !engine.objectiveMet()) {
            throw new IllegalStateException("The game engine decides whether this objective is met, and it is not met yet. "
                    + "Keep playing toward it; the game ends the story by itself when it is.");
        }
        if (!achieved && scenario != null && scenario.type().engineDecides()
                && engine.getState().getTurnsTaken() < engine.getState().getTurnLimit()) {
            throw new IllegalStateException("The story cannot end in failure yet: the heroes are still alive and turns remain. "
                    + "Keep playing toward the objective; the game ends the story itself if every hero falls or the turns run out.");
        }
        engine.getState().addLog("The story ends: " + ending);
        engine.concludeStory(achieved);
        return (achieved ? "Objective achieved. " : "Objective not achieved. ") + "The story has concluded. Phase: " + engine.getState().getPhase().name() + ".";
    }
}
