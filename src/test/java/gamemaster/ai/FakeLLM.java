package gamemaster.ai;

import gamemaster.llm.ChatMessage;
import gamemaster.llm.LLMClient;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Scripted model for tests: returns canned replies and remembers what it was asked. */
public class FakeLLM implements LLMClient {

    private final Deque<String> replies = new ArrayDeque<>();
    public final List<List<ChatMessage>> calls = new ArrayList<>();

    public FakeLLM(String... replies) {
        for (String r : replies) {
            this.replies.addLast(r);
        }
    }

    @Override
    public String complete(List<ChatMessage> messages) {
        calls.add(List.copyOf(messages));
        if (replies.isEmpty()) {
            throw new IllegalStateException("The test asked for more replies than it scripted.");
        }
        return replies.removeFirst();
    }
}
