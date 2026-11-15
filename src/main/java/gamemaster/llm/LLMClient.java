package gamemaster.llm;

import java.util.List;

/**
 * Adapter target: the rest of the game talks to a model only through this
 * interface, so the provider can change without touching game or agent code.
 */
public interface LLMClient {

    /** Sends the conversation, offering the given tools; the reply may ask to run some of them. */
    LLMResponse chat(List<ChatMessage> messages, List<ToolSpec> tools);

    /** Told how many seconds remain while a rate-limited request waits to retry; 0 means it is going again. */
    interface WaitListener {
        void waiting(int secondsLeft);
    }

    /** Registers who hears about rate-limit waits. Clients that never wait may ignore it. */
    default void setWaitListener(WaitListener listener) {}

    /** Stops a rate-limit wait in progress, so the request fails instead of retrying. */
    default void cancelWait() {}

    /** Total tokens the provider reported for this session's calls (0 when unknown). */
    default long totalTokens() {
        return 0;
    }

    /** How many model calls succeeded this session. */
    default int callCount() {
        return 0;
    }

    /** Plain text in, plain text out, with no tools offered. */
    default String complete(List<ChatMessage> messages) {
        return chat(messages, List.of()).content();
    }
}
