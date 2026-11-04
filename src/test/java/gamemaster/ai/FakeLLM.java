package gamemaster.ai;

import gamemaster.llm.ChatMessage;
import gamemaster.llm.LLMClient;
import gamemaster.llm.LLMResponse;
import gamemaster.llm.ToolCall;
import gamemaster.llm.ToolSpec;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Scripted model for tests: returns canned replies and remembers what it was asked. */
public class FakeLLM implements LLMClient {

    private final Deque<LLMResponse> replies = new ArrayDeque<>();
    public final List<List<ChatMessage>> calls = new ArrayList<>();
    public final List<List<ToolSpec>> toolsOffered = new ArrayList<>();

    /** Each string becomes a plain text reply. */
    public FakeLLM(String... texts) {
        for (String t : texts) {
            replies.addLast(LLMResponse.text(t));
        }
    }

    public FakeLLM then(LLMResponse response) {
        replies.addLast(response);
        return this;
    }

    public FakeLLM thenText(String text) {
        return then(LLMResponse.text(text));
    }

    public FakeLLM thenTool(String name, String argsJson) {
        return then(new LLMResponse("", List.of(new ToolCall("call_" + (calls.size() + replies.size()), name, argsJson))));
    }

    @Override
    public LLMResponse chat(List<ChatMessage> messages, List<ToolSpec> tools) {
        calls.add(List.copyOf(messages));
        toolsOffered.add(List.copyOf(tools));
        if (replies.isEmpty()) {
            throw new IllegalStateException("The test asked for more replies than it scripted.");
        }
        return replies.removeFirst();
    }
}
