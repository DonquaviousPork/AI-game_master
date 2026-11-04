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
                "End the adventure when the story has reached a natural conclusion. Do not use this "
                        + "while enemies remain unless the story truly ends.",
                Schema.object().string("ending", "One or two sentences describing how the story ends", true).build());
    }

    @Override
    public String execute(JsonObject args) {
        String ending = JsonFields.text(args, "ending");
        if (!engine.getState().getPhase().allows(ActionKind.ABILITY_CHECK)) {
            throw new IllegalStateException("The story cannot end during the "
                    + engine.getState().getPhase().name() + " phase.");
        }
        engine.getState().addLog("The story ends: " + ending);
        engine.concludeStory();
        return "The story has concluded. Phase: " + engine.getState().getPhase().name() + ".";
    }
}
