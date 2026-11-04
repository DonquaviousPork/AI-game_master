package gamemaster.llm;

import java.util.List;

/**
 * Adapter target: the rest of the game talks to a model only through this
 * interface, so the provider can change without touching game or agent code.
 */
public interface LLMClient {

    /** Sends the conversation, offering the given tools; the reply may ask to run some of them. */
    LLMResponse chat(List<ChatMessage> messages, List<ToolSpec> tools);

    /** Plain text in, plain text out, with no tools offered. */
    default String complete(List<ChatMessage> messages) {
        return chat(messages, List.of()).content();
    }
}
