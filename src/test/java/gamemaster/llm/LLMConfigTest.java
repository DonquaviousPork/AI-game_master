package gamemaster.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LLMConfigTest {

    @Test
    void parsesKeyValueLinesIgnoringCommentsAndBlanks() {
        Map<String, String> m = LLMConfig.parseDotEnv(List.of(
                "# comment", "", "LLM_API_KEY = abc123 ", "LLM_MODEL=\"some/model\"", "junk line"));
        assertEquals("abc123", m.get("LLM_API_KEY"));
        assertEquals("some/model", m.get("LLM_MODEL"));
        assertEquals(2, m.size());
    }

    @Test
    void loadsFromAFileAndUsesTheDefaultBaseUrl() throws Exception {
        Path f = Files.createTempFile("env", ".test");
        Files.writeString(f, "LLM_API_KEY=k1\nLLM_MODEL=m1\n");
        LLMConfig c = LLMConfig.load(f, Map.of());
        assertEquals("k1", c.apiKey());
        assertEquals("m1", c.model());
        assertEquals(LLMConfig.DEFAULT_BASE_URL, c.baseUrl());
        Files.delete(f);
    }

    @Test
    void environmentVariablesOverrideTheFile() throws Exception {
        Path f = Files.createTempFile("env", ".test");
        Files.writeString(f, "LLM_API_KEY=fromfile\nLLM_MODEL=m1\n");
        LLMConfig c = LLMConfig.load(f, Map.of("LLM_API_KEY", "fromenv"));
        assertEquals("fromenv", c.apiKey());
        Files.delete(f);
    }

    @Test
    void missingFileIsFineWhenTheEnvironmentHasEverything() {
        LLMConfig c = LLMConfig.load(Path.of("does-not-exist.env"),
                Map.of("LLM_API_KEY", "k", "LLM_MODEL", "m", "LLM_BASE_URL", "http://localhost:1234/v1/"));
        assertEquals("http://localhost:1234/v1", c.baseUrl());
    }

    @Test
    void missingOrPlaceholderValuesAreRejectedWithAHelpfulMessage() {
        LLMException e = assertThrows(LLMException.class,
                () -> LLMConfig.load(Path.of("does-not-exist.env"), Map.of()));
        assertTrue(e.getMessage().contains(".env"));
        assertThrows(LLMException.class, () -> new LLMConfig("YOUR_LOCAL_API_KEY_HERE", "m", null));
        assertThrows(LLMException.class, () -> new LLMConfig("k", "YOUR_MODEL_NAME_HERE", null));
    }

    @Test
    void toStringNeverShowsTheKey() {
        String text = new LLMConfig("supersecretvalue", "m", null).toString();
        assertFalse(text.contains("supersecretvalue"));
    }
}
