package gamemaster.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import gamemaster.game.GameEngine;
import gamemaster.llm.ToolCall;
import gamemaster.llm.ToolSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The only door between the AI and the game. It lists the tools the model may use and
 * runs a requested call. Anything wrong with a request becomes an "Error: ..." message
 * for the model to correct itself; it never throws and never changes the game.
 */
public class ToolRegistry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public static ToolRegistry defaultFor(GameEngine engine) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new RollCheckTool(engine));
        registry.register(new AttackTool(engine));
        registry.register(new SpawnMonsterTool(engine));
        registry.register(new EndStoryTool(engine));
        return registry;
    }

    public void register(Tool tool) {
        Objects.requireNonNull(tool, "tool");
        tools.put(tool.spec().name(), tool);
    }

    public List<ToolSpec> specs() {
        List<ToolSpec> out = new ArrayList<>();
        tools.values().forEach(t -> out.add(t.spec()));
        return out;
    }

    public String execute(ToolCall call) {
        Tool tool = tools.get(call.name());
        if (tool == null) {
            return "Error: unknown tool \"" + call.name() + "\". Available: " + String.join(", ", tools.keySet()) + ".";
        }
        JsonObject args;
        try {
            String raw = call.argumentsJson() == null || call.argumentsJson().isBlank() ? "{}" : call.argumentsJson();
            args = JsonParser.parseString(raw).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            return "Error: the arguments were not a valid JSON object.";
        }
        try {
            return tool.execute(args);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "Error: " + e.getMessage();
        }
    }
}
