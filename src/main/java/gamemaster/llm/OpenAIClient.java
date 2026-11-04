package gamemaster.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;

/**
 * Adapter for any OpenAI-compatible chat-completions endpoint (Groq, OpenRouter,
 * Gemini's compatibility layer, OpenAI itself). It translates our ChatMessage
 * list to that JSON format and the reply back to plain text.
 */
public class OpenAIClient implements LLMClient {

    private static final double TEMPERATURE = 0.8;

    /** How many times a rate-limited (HTTP 429) request is retried before giving up. */
    static final int MAX_RATE_LIMIT_RETRIES = 2;
    static final long DEFAULT_WAIT_MS = 5_000;
    static final long MAX_WAIT_MS = 20_000;

    private final LLMConfig config;
    private final HttpClient http;
    private final LongConsumer sleeper;

    public OpenAIClient(LLMConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    OpenAIClient(LLMConfig config, HttpClient http) {
        this(config, http, OpenAIClient::sleepMillis);
    }

    OpenAIClient(LLMConfig config, HttpClient http, LongConsumer sleeper) {
        this.config = Objects.requireNonNull(config, "config");
        this.http = Objects.requireNonNull(http, "http");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper");
    }

    private static void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMException("The request was interrupted.", e);
        }
    }

    /** Milliseconds to wait after a 429: the server's Retry-After seconds, capped, or a default when absent. */
    static long waitMillis(String retryAfterSeconds) {
        if (retryAfterSeconds != null) {
            try {
                double seconds = Double.parseDouble(retryAfterSeconds.trim());
                if (seconds >= 0) {
                    return Math.min(MAX_WAIT_MS, (long) Math.ceil(seconds * 1000));
                }
            } catch (NumberFormatException ignored) {
                // fall through to the default
            }
        }
        return DEFAULT_WAIT_MS;
    }

    @Override
    public LLMResponse chat(List<ChatMessage> messages, List<ToolSpec> tools) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(messages, tools)))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            for (int retry = 0; response.statusCode() == 429 && retry < MAX_RATE_LIMIT_RETRIES; retry++) {
                sleeper.accept(waitMillis(response.headers().firstValue("Retry-After").orElse(null)));
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
            }
            return parseResponse(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new LLMException("Could not reach the model service: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMException("The request was interrupted.", e);
        }
    }

    String buildRequestBody(List<ChatMessage> messages, List<ToolSpec> tools) {
        JsonArray array = new JsonArray();
        for (ChatMessage m : messages) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m.role());
            if (m.content() == null || m.content().isEmpty() && !m.toolCalls().isEmpty()) {
                o.add("content", com.google.gson.JsonNull.INSTANCE);
            } else {
                o.addProperty("content", m.content());
            }
            if (m.toolCallId() != null) {
                o.addProperty("tool_call_id", m.toolCallId());
            }
            if (!m.toolCalls().isEmpty()) {
                JsonArray calls = new JsonArray();
                for (ToolCall c : m.toolCalls()) {
                    JsonObject fn = new JsonObject();
                    fn.addProperty("name", c.name());
                    fn.addProperty("arguments", c.argumentsJson());
                    JsonObject call = new JsonObject();
                    call.addProperty("id", c.id());
                    call.addProperty("type", "function");
                    call.add("function", fn);
                    calls.add(call);
                }
                o.add("tool_calls", calls);
            }
            array.add(o);
        }
        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.add("messages", array);
        body.addProperty("temperature", TEMPERATURE);
        if (tools != null && !tools.isEmpty()) {
            JsonArray toolArray = new JsonArray();
            for (ToolSpec t : tools) {
                JsonObject fn = new JsonObject();
                fn.addProperty("name", t.name());
                fn.addProperty("description", t.description());
                fn.add("parameters", t.parameters());
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty("type", "function");
                wrapper.add("function", fn);
                toolArray.add(wrapper);
            }
            body.add("tools", toolArray);
        }
        return body.toString();
    }

    static LLMResponse parseResponse(int status, String body) {
        if (status == 401 || status == 403) {
            throw new LLMException("The model service rejected the API key (HTTP " + status + ").");
        }
        if (status == 429) {
            throw new LLMException("Rate limit reached (HTTP 429). Wait a minute and try again.");
        }
        if (status < 200 || status >= 300) {
            throw new LLMException("The model service returned HTTP " + status + ": " + abbreviate(body));
        }
        try {
            JsonArray choices = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LLMException("The model returned no choices.");
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            JsonElement content = message.get("content");
            String text = content == null || content.isJsonNull() ? "" : content.getAsString();

            List<ToolCall> calls = new ArrayList<>();
            JsonElement rawCalls = message.get("tool_calls");
            if (rawCalls != null && rawCalls.isJsonArray()) {
                for (JsonElement e : rawCalls.getAsJsonArray()) {
                    JsonObject call = e.getAsJsonObject();
                    JsonObject fn = call.getAsJsonObject("function");
                    JsonElement args = fn.get("arguments");
                    String id = call.has("id") ? call.get("id").getAsString() : "call_" + calls.size();
                    calls.add(new ToolCall(id, fn.get("name").getAsString(),
                            args == null || args.isJsonNull() ? "{}" : args.getAsString()));
                }
            }
            if (text.isBlank() && calls.isEmpty()) {
                throw new LLMException("The model returned an empty reply.");
            }
            return new LLMResponse(text, calls);
        } catch (JsonParseException | IllegalStateException | ClassCastException | NullPointerException e) {
            throw new LLMException("The model service sent a reply in an unexpected format.", e);
        }
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }
}
