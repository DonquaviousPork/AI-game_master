package gamemaster.llm;

import java.util.List;

/** What the model sent back: text, tool requests, or both. */
public record LLMResponse(String content, List<ToolCall> toolCalls) {

    public LLMResponse {
        content = content == null ? "" : content;
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static LLMResponse text(String content) {
        return new LLMResponse(content, List.of());
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
