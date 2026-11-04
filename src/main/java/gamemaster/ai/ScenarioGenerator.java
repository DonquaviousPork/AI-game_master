package gamemaster.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import gamemaster.game.Scenario;
import gamemaster.game.StoryEvent;
import gamemaster.model.Monster;
import gamemaster.llm.ChatMessage;
import gamemaster.llm.LLMClient;
import gamemaster.llm.LLMException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * F01: asks the model for a starting scenario as JSON, then validates every field
 * against the game's limits. If the reply is unusable it tells the model exactly
 * what was wrong and asks once more. The AI proposes; this class decides what is allowed.
 */
public class ScenarioGenerator {

    static final int MAX_ATTEMPTS = 2;
    static final int MAX_MONSTERS = 3;
    static final int MIN_HEALTH = JsonFields.MIN_HEALTH, MAX_HEALTH = JsonFields.MAX_HEALTH;
    static final int MIN_AC = JsonFields.MIN_AC, MAX_AC = JsonFields.MAX_AC;

    private static final String SYSTEM_PROMPT = """
            You write opening scenarios for a small fantasy text role-playing game.
            Reply with ONLY one JSON object and no other text, in exactly this shape:
            {"title": "...", "description": "...", "opening_event": "...",
             "monsters": [{"name": "...", "health": 8, "armor_class": 12, "weapon": "dagger"}]}
            Rules:
            - title: a short name for the adventure.
            - description: two or three sentences setting the scene for a party of heroes.
            - opening_event: one or two sentences describing what happens first.
            - monsters: 0 to 3 enemies the party meets at once. health must be %d to %d,
              armor_class must be %d to %d, weapon must be one of: longsword, quarterstaff, dagger.
            - Do not decide any outcomes, rolls or damage. The game rules handle those.
            """.formatted(MIN_HEALTH, MAX_HEALTH, MIN_AC, MAX_AC);

    private final LLMClient llm;

    public ScenarioGenerator(LLMClient llm) {
        this.llm = Objects.requireNonNull(llm, "llm");
    }

    /** @param theme optional hint from the player, may be null or blank */
    public GeneratedScenario generate(String theme) {
        List<ChatMessage> conversation = new ArrayList<>();
        conversation.add(ChatMessage.system(SYSTEM_PROMPT));
        conversation.add(ChatMessage.user(theme == null || theme.isBlank()
                ? "Create a new adventure."
                : "Create a new adventure with this theme: " + theme.trim()));

        String lastProblem = "";
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String reply = llm.complete(conversation);
            try {
                return parse(reply);
            } catch (IllegalArgumentException e) {
                lastProblem = e.getMessage();
                conversation.add(ChatMessage.assistant(reply));
                conversation.add(ChatMessage.user("That reply was rejected: " + lastProblem
                        + " Reply again with only the corrected JSON object."));
            }
        }
        throw new LLMException("The AI could not produce a valid scenario: " + lastProblem);
    }

    /** Package-visible so tests can check validation without a model. */
    static GeneratedScenario parse(String reply) {
        JsonObject root = extractObject(reply);
        Scenario scenario = new Scenario(text(root, "title"), text(root, "description"));
        StoryEvent event = new StoryEvent(text(root, "opening_event"));

        JsonElement monstersElement = root.get("monsters");
        if (monstersElement == null || !monstersElement.isJsonArray()) {
            throw new IllegalArgumentException("\"monsters\" must be an array.");
        }
        JsonArray array = monstersElement.getAsJsonArray();
        if (array.size() > MAX_MONSTERS) {
            throw new IllegalArgumentException("At most " + MAX_MONSTERS + " monsters are allowed.");
        }
        List<Monster> monsters = new ArrayList<>();
        for (JsonElement e : array) {
            if (!e.isJsonObject()) {
                throw new IllegalArgumentException("Each monster must be an object.");
            }
            JsonObject m = e.getAsJsonObject();
            monsters.add(JsonFields.monster(m));
        }
        return new GeneratedScenario(scenario, event, monsters);
    }

    private static JsonObject extractObject(String reply) {
        if (reply == null) {
            throw new IllegalArgumentException("The reply was empty.");
        }
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("The reply did not contain a JSON object.");
        }
        try {
            return JsonParser.parseString(reply.substring(start, end + 1)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IllegalArgumentException("The reply was not valid JSON.");
        }
    }

    private static String text(JsonObject o, String key) {
        return JsonFields.text(o, key);
    }
}
