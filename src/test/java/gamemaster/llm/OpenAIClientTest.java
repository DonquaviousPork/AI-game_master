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
        String body = client().buildRequestBody(List.of(ChatMessage.system("be brief"), ChatMessage.user("hi")), List.of());
        JsonObject o = JsonParser.parseString(body).getAsJsonObject();
        assertEquals("test/model", o.get("model").getAsString());
        assertEquals(2, o.getAsJsonArray("messages").size());
        JsonObject first = o.getAsJsonArray("messages").get(0).getAsJsonObject();
        assertEquals("system", first.get("role").getAsString());
        assertEquals("be brief", first.get("content").getAsString());
    }

    @Test
    void requestBodyNeverContainsTheApiKey() {
        assertFalse(client().buildRequestBody(List.of(ChatMessage.user("hi")), List.of()).contains("\"k\""));
    }

    @Test
    void parsesTheFirstChoicesContent() {
        String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello there\"}}]}";
        assertEquals("Hello there", OpenAIClient.parseResponse(200, json).content());
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

    @Test
    void toolsAreSentInTheRequestWhenOffered() {
        JsonObject params = new JsonObject();
        params.addProperty("type", "object");
        String body = client().buildRequestBody(List.of(ChatMessage.user("hi")),
                List.of(new ToolSpec("roll_check", "Roll a check", params)));
        JsonObject tool = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("tools")
                .get(0).getAsJsonObject();
        assertEquals("function", tool.get("type").getAsString());
        assertEquals("roll_check", tool.getAsJsonObject("function").get("name").getAsString());
    }

    @Test
    void noToolsKeyWhenNoneOffered() {
        String body = client().buildRequestBody(List.of(ChatMessage.user("hi")), List.of());
        assertFalse(JsonParser.parseString(body).getAsJsonObject().has("tools"));
    }

    @Test
    void toolCallsAndToolResultsRoundTripInTheRequestFormat() {
        ToolCall call = new ToolCall("c1", "attack", "{\"attacker\":\"Brenna\"}");
        String body = client().buildRequestBody(List.of(
                ChatMessage.assistantToolCalls("", List.of(call)),
                ChatMessage.toolResult("c1", "Brenna hits.")), List.of());
        var messages = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("messages");
        JsonObject assistant = messages.get(0).getAsJsonObject();
        assertTrue(assistant.get("content").isJsonNull());
        JsonObject sent = assistant.getAsJsonArray("tool_calls").get(0).getAsJsonObject();
        assertEquals("c1", sent.get("id").getAsString());
        assertEquals("attack", sent.getAsJsonObject("function").get("name").getAsString());
        JsonObject tool = messages.get(1).getAsJsonObject();
        assertEquals("tool", tool.get("role").getAsString());
        assertEquals("c1", tool.get("tool_call_id").getAsString());
    }

    @Test
    void parsesToolCallsWhenContentIsNull() {
        String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":["
                + "{\"id\":\"abc\",\"type\":\"function\",\"function\":{\"name\":\"roll_check\","
                + "\"arguments\":\"{\\\"character\\\":\\\"Mira\\\"}\"}}]}}]}";
        LLMResponse r = OpenAIClient.parseResponse(200, json);
        assertTrue(r.hasToolCalls());
        assertEquals("abc", r.toolCalls().get(0).id());
        assertEquals("roll_check", r.toolCalls().get(0).name());
        assertEquals("{\"character\":\"Mira\"}", r.toolCalls().get(0).argumentsJson());
    }

    @Test
    void retryAfterIsHonouredCappedAndDefaulted() {
        assertEquals(3000L, OpenAIClient.waitMillis("3"));
        assertEquals(1500L, OpenAIClient.waitMillis("1.5"));
        assertEquals(OpenAIClient.MAX_WAIT_MS, OpenAIClient.waitMillis("600"));
        assertEquals(OpenAIClient.DEFAULT_WAIT_MS, OpenAIClient.waitMillis(null));
        assertEquals(OpenAIClient.DEFAULT_WAIT_MS, OpenAIClient.waitMillis("soon"));
        assertEquals(OpenAIClient.DEFAULT_WAIT_MS, OpenAIClient.waitMillis("-4"));
    }
}
