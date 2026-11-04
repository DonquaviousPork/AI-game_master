package gamemaster.llm;

import java.util.List;

/**
 * One message in a chat. role is "system", "user", "assistant" or "tool".
 * An assistant message may carry tool requests; a tool message carries the
 * result for one request, identified by toolCallId.
 */
public record ChatMessage(String role, String content, List<ToolCall> toolCalls, String toolCallId) {

    public ChatMessage {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public ChatMessage(String role, String content) {
        this(role, content, List.of(), null);
    }

    public static ChatMessage system(String content) { return new ChatMessage("system", content); }
    public static ChatMessage user(String content) { return new ChatMessage("user", content); }
    public static ChatMessage assistant(String content) { return new ChatMessage("assistant", content); }

    public static ChatMessage assistantToolCalls(String content, List<ToolCall> calls) {
        return new ChatMessage("assistant", content, calls, null);
    }

    public static ChatMessage toolResult(String toolCallId, String content) {
        return new ChatMessage("tool", content, List.of(), toolCallId);
    }
}
