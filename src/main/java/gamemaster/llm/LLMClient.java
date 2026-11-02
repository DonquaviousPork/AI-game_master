package gamemaster.llm;

import java.util.List;

/**
 * Adapter target: the rest of the game talks to a model only through this
 * interface, so the provider can change without touching game or agent code.
 */
public interface LLMClient {

    /** Sends the conversation and returns the model's reply text. */
    String complete(List<ChatMessage> messages);
}
