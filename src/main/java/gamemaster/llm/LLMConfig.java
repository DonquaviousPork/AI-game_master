package gamemaster.llm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Connection settings, read from a local .env file and the environment. The key
 * never appears in source code, and toString() hides it so it cannot leak into logs.
 */
public record LLMConfig(String apiKey, String model, String baseUrl) {

    public static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";

    public LLMConfig {
        apiKey = require(apiKey, "LLM_API_KEY");
        model = require(model, "LLM_MODEL");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }
        baseUrl = baseUrl.trim().replaceAll("/+$", "");
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank() || value.trim().startsWith("YOUR_")) {
            throw new LLMException(name + " is not set. Copy .env.example to .env and fill it in.");
        }
        return value.trim();
    }

    /**
     * Reads .env from the working directory, or else from the folder the game was started from
     * (next to the .jar), so a double-clicked game finds it. Real environment variables take priority.
     */
    public static LLMConfig load() {
        Path env = Path.of(".env");
        if (!Files.isRegularFile(env)) {
            Path beside = besideProgram();
            if (beside != null && Files.isRegularFile(beside)) {
                env = beside;
            }
        }
        return load(env, System.getenv());
    }

    private static Path besideProgram() {
        try {
            Path location = Path.of(LLMConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path folder = Files.isDirectory(location) ? location : location.getParent();
            // an installed app keeps its jar in an "app" folder with .env one level up
            for (Path p = folder; p != null && p.getNameCount() > 0; p = p.getParent()) {
                if (Files.isRegularFile(p.resolve(".env"))) {
                    return p.resolve(".env");
                }
                if (p.equals(folder.getParent() == null ? folder : folder.getParent().getParent())) {
                    break;
                }
            }
        } catch (Exception e) {
            // fall through: no .env beside the program
        }
        return null;
    }

    public static LLMConfig load(Path envFile, Map<String, String> environment) {
        Map<String, String> values = new HashMap<>();
        if (Files.isRegularFile(envFile)) {
            try {
                values.putAll(parseDotEnv(Files.readAllLines(envFile)));
            } catch (IOException e) {
                throw new LLMException("Could not read " + envFile, e);
            }
        }
        for (String key : List.of("LLM_API_KEY", "LLM_MODEL", "LLM_BASE_URL")) {
            String fromEnv = environment.get(key);
            if (fromEnv != null && !fromEnv.isBlank()) {
                values.put(key, fromEnv);
            }
        }
        return new LLMConfig(values.get("LLM_API_KEY"), values.get("LLM_MODEL"), values.get("LLM_BASE_URL"));
    }

    static Map<String, String> parseDotEnv(List<String> lines) {
        Map<String, String> out = new HashMap<>();
        for (String raw : lines) {
            String line = raw.trim();
            int eq = line.indexOf('=');
            if (line.isEmpty() || line.startsWith("#") || eq < 1) {
                continue;
            }
            String value = line.substring(eq + 1).trim();
            if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                    || value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            out.put(line.substring(0, eq).trim(), value);
        }
        return out;
    }

    @Override public String toString() {
        return "LLMConfig[model=" + model + ", baseUrl=" + baseUrl + ", apiKey=****]";
    }
}
