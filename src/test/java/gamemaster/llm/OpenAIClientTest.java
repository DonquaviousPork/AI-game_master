package gamemaster.llm;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.http.HttpClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenAIClientTest {

    private OpenAIClient client() {
        return new OpenAIClient(new LLMConfig("k", "test/model", null), HttpClient.newHttpClient());
    }

    @Test
    void requestBodyHasModelAndMessagesInOrder() {
        String body = client().buildRequestBody(List.of(ChatMessage.system("be brief"), ChatMessage.user("hi")));
        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        assertEquals("test/model", o.get("model").getAsString());
        assertEquals(2, o.getAsJsonArray("messages").size());
        JsonObject first = o.getAsJsonArray("messages").get(0).getAsJsonObject();
        assertEquals("system", first.get("role").getAsString());
        assertEquals("be brief", first.get("content").getAsString());
    }

    @Test
    void requestBodyNeverContainsTheApiKey() {
        assertFalse(client().buildRequestBody(List.of(ChatMessage.user("hi"))).contains("\"k\""));
    }

    @Test
    void parsesTheFirstChoicesContent() {
        String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello there\"}}]}";
        assertEquals("Hello there", OpenAIClient.parseResponse(200, json));
    }

    @Test
    void rejectedKeysRateLimitsAndServerErrorsGiveDistinctMessages() {
        assertTrue(assertThrows(LLMException.class, () -> OpenAIClient.parseResponse(401, "{}"))
                .getMessage().contains("API key"));
        assertTrue(assertThrows(LLMException.class, () -> OpenAIClient.parseResponse(429, "{}"))
                .getMessage().contains("Rate limit"));
        assertTrue(assertThrows(LLMException.class, () -> OpenAIClient.parseResponse(500, "boom"))
                .getMessage().contains("500"));
    }

    @Test
    void emptyMissingOrMalformedRepliesAreReportedNotCrashed() {
        assertThrows(LLMException.class, () -> OpenAIClient.parseResponse(200, "{\"choices\":[]}"));
        assertThrows(LLMException.class,
                () -> OpenAIClient.parseResponse(200, "{\"choices\":[{\"message\":{\"content\":null}}]}"));
        assertThrows(LLMException.class, () -> OpenAIClient.parseResponse(200, "not json"));
        assertThrows(LLMException.class, () -> OpenAIClient.parseResponse(200, "{\"error\":1}"));
    }
}
