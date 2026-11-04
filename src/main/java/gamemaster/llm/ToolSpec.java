package gamemaster.llm;

import com.google.gson.JsonObject;

/** Describes one tool to the model: its name, what it does, and a JSON-schema for its arguments. */
public record ToolSpec(String name, String description, JsonObject parameters) {
    public ToolSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name must not be blank");
        }
    }
}
