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
import java.util.List;
import java.util.Objects;

/**
 * Adapter for any OpenAI-compatible chat-completions endpoint (Groq, OpenRouter,
 * Gemini's compatibility layer, OpenAI itself). It translates our ChatMessage
 * list to that JSON format and the reply back to plain text.
 */
public class OpenAIClient implements LLMClient {

    private static final double TEMPERATURE = 0.8;

    private final LLMConfig config;
    private final HttpClient http;

    public OpenAIClient(LLMConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    OpenAIClient(LLMConfig config, HttpClient http) {
        this.config = Objects.requireNonNull(config, "config");
        this.http = Objects.requireNonNull(http, "http");
    }

    @Override
    public String complete(List<ChatMessage> messages) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(messages)))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            return parseResponse(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new LLMException("Could not reach the model service: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMException("The request was interrupted.", e);
        }
    }

    String buildRequestBody(List<ChatMessage> messages) {
        JsonArray array = new JsonArray();
        for (ChatMessage m : messages) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m.role());
            o.addProperty("content", m.content());
            array.add(o);
        }
        JsonObject body = new JsonObject();
        body.addProperty("model", config.model());
        body.add("messages", array);
        body.addProperty("temperature", TEMPERATURE);
        return body.toString();
    }

    static String parseResponse(int status, String body) {
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
            JsonElement content = choices.get(0).getAsJsonObject().getAsJsonObject("message").get("content");
            if (content == null || content.isJsonNull() || content.getAsString().isBlank()) {
                throw new LLMException("The model returned an empty reply.");
            }
            return content.getAsString();
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
