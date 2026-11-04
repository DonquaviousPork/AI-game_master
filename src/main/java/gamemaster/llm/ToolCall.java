package gamemaster.llm;

/** A request from the model to run a tool. The arguments arrive as a JSON string. */
public record ToolCall(String id, String name, String argumentsJson) {}
