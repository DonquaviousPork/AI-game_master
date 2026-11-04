package gamemaster.ai;

import com.google.gson.JsonObject;
import gamemaster.llm.ToolSpec;

/**
 * Something the AI may ask the game to do. A tool checks its own arguments and
 * changes the game only through GameEngine. Bad input throws IllegalArgumentException
 * or IllegalStateException; the registry turns that into a message for the model.
 */
public interface Tool {

    ToolSpec spec();

    /** Runs the tool and returns a short result the model can narrate from. */
    String execute(JsonObject arguments);
}
