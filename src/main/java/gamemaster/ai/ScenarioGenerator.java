package gamemaster.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import gamemaster.game.ObjectiveType;
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
    /** Used only when the model leaves the objective out, so every adventure still has a goal. */
    static final String DEFAULT_OBJECTIVE = "Resolve the situation you find yourselves in before time runs out.";
    static final int MAX_MONSTERS = 3;
    static final int MIN_HEALTH = JsonFields.MIN_HEALTH, MAX_HEALTH = JsonFields.MAX_HEALTH;
    static final int MIN_AC = JsonFields.MIN_AC, MAX_AC = JsonFields.MAX_AC;

    private static final String SYSTEM_PROMPT = """
            You write opening scenarios for a small fantasy text role-playing game.
            Reply with ONLY one JSON object and no other text, in exactly this shape:
            {"title": "...", "description": "...", "objective": "...", "objective_type": "defeat_target",
             "objective_target": "...", "opening_event": "...",
             "monsters": [{"name": "...", "health": 8, "armor_class": 12, "weapon": "dagger"}]}
            Rules:
            - title: a short name for the adventure.
            - description: two or three sentences setting the scene for a party of heroes.
            - objective: ONE concrete goal the heroes can achieve in about 15 short turns, written as one
              sentence the player can read (for example: Rescue the missing children and drive the goblins out
              of the tree house). It must be a single goal, not a list, and must not need more than a few scenes.
            - objective_type: "defeat_target" (the objective is to defeat one named enemy), "survive" (hold
              out until time runs out) or "narrative" (anything else, such as finding or rescuing something).
              Prefer defeat_target when the goal ends in a showdown, and write the objective to match.
            - objective_target: for defeat_target only, the name of that enemy (a boss); otherwise an empty
              string. This enemy does NOT appear in the opening: it comes later in the story.
            - opening_event: one or two sentences describing the first scene. It is a quiet or mysterious
              scene, not a fight: the heroes arrive, explore, talk or investigate before any combat.
            - monsters: leave this as an empty array []. Enemies are introduced later by the game master, so
              the story has a beginning and a middle before any fight. (Only if the player's request asks
              for a fight right away, list 1 to 3 enemies.) health must be %d to %d, armor_class must be %d
              to %d, weapon must be one of: longsword, quarterstaff, dagger.
            - Do not decide any outcomes, rolls or damage. The game rules handle those.
            - Inside text values use single quotes, never double quotes, and put no line breaks inside a value.
            """.formatted(MIN_HEALTH, MAX_HEALTH, MIN_AC, MAX_AC);

    private final LLMClient llm;

    public ScenarioGenerator(LLMClient llm) {
        this.llm = Objects.requireNonNull(llm, "llm");
    }

    /** @param theme optional hint from the player, may be null or blank */
    public GeneratedScenario generate(String theme) {
        return generate(theme, false);
    }

    /** Largest enemy allowed in an easy (demo) adventure: it falls in one or two hits. */
    static final int EASY_MAX_HEALTH = 5, EASY_MAX_AC = 8;

    /**
     * @param theme optional hint from the player, may be null or blank
     * @param easy  true for the demo: one weak enemy and the objective "Defeat the ..."
     */
    public GeneratedScenario generate(String theme, boolean easy) {
        List<ChatMessage> conversation = new ArrayList<>();
        conversation.add(ChatMessage.system(SYSTEM_PROMPT));
        conversation.add(ChatMessage.user(theme == null || theme.isBlank()
                ? "Create a new adventure."
                : "Create a new adventure with this theme: " + theme.trim()));
        if (easy) {
            conversation.add(ChatMessage.user("This is a quick demo (this overrides the rule about an empty monsters "
                    + "list): list exactly ONE enemy with very low health and armor class, and make the objective "
                    + "simply to defeat that enemy."));
        }

        String lastProblem = "";
        String lastReply = "";
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String reply = llm.complete(conversation);
            lastReply = reply == null ? "" : reply;
            try {
                GeneratedScenario parsed = parse(reply);
                return easy ? makeEasy(parsed) : parsed;
            } catch (IllegalArgumentException e) {
                lastProblem = e.getMessage();
                conversation.add(ChatMessage.assistant(reply));
                conversation.add(ChatMessage.user("That reply was rejected: " + lastProblem
                        + " Reply again with only the corrected JSON object."));
            }
        }
        throw new LLMException("The AI could not produce a valid scenario: " + lastProblem
                + " (the last reply began: \"" + preview(lastReply) + "\")");
    }

    /**
     * The engine, not the model, makes a demo adventure easy: one enemy, capped health and armor class,
     * and the objective is to defeat it.
     */
    static GeneratedScenario makeEasy(GeneratedScenario s) {
        Monster first;
        if (s.monsters().isEmpty()) {
            String name = s.scenario().target();
            if (name.isBlank()) {
                return s;
            }
            // The demo starts at the showdown: the named boss is put on the field, made weak by the engine.
            first = new Monster(name, EASY_MAX_HEALTH, EASY_MAX_AC, gamemaster.model.Weapon.DAGGER);
        } else {
            first = s.monsters().get(0);
        }
        Monster weak = new Monster(first.getName(), Math.min(first.getMaxHealth(), EASY_MAX_HEALTH),
                Math.min(first.getArmorClass(), EASY_MAX_AC), first.getWeapon());
        Scenario scenario = new Scenario(s.scenario().title(), s.scenario().description(),
                "Defeat the " + first.getName() + ".", ObjectiveType.DEFEAT_TARGET, first.getName());
        return new GeneratedScenario(scenario, s.openingEvent(), List.of(weak));
    }

    /** Package-visible so tests can check validation without a model. */
    static GeneratedScenario parse(String reply) {
        JsonObject root = extractObject(reply);
        String objective = root.has("objective") && !root.get("objective").isJsonNull()
                ? root.get("objective").getAsString().trim() : "";
        if (objective.length() > 240) {
            throw new IllegalArgumentException("\"objective\" must be one short sentence (under 240 characters).");
        }
        if (objective.isEmpty()) {
            objective = DEFAULT_OBJECTIVE;
        }
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
        ObjectiveType type = ObjectiveType.parse(optional(root, "objective_type"));
        String target = optional(root, "objective_target");
        // The engine only trusts a typed objective it can actually check; anything else stays narrative.
        if (type == ObjectiveType.DEFEAT_TARGET) {
            String wanted = target;
            Monster match = monsters.stream().filter(m -> m.getName().equalsIgnoreCase(wanted)).findFirst().orElse(null);
            if (match != null) {
                target = match.getName();
            } else if (target.isBlank() || target.length() > 60) {
                type = ObjectiveType.NARRATIVE;   // nothing the engine could check
                target = "";
            }
        } else if (type == ObjectiveType.DEFEAT_ALL && monsters.isEmpty()) {
            type = ObjectiveType.NARRATIVE;
        }
        if (type != ObjectiveType.DEFEAT_TARGET) {
            target = "";
        }
        Scenario scenario = new Scenario(text(root, "title"), text(root, "description"), objective, type, target);
        return new GeneratedScenario(scenario, event, monsters);
    }

    private static String optional(JsonObject root, String key) {
        return root.has(key) && !root.get(key).isJsonNull() && root.get(key).isJsonPrimitive()
                ? root.get(key).getAsString().trim() : "";
    }

    private static JsonObject extractObject(String reply) {
        if (reply == null) {
            throw new IllegalArgumentException("The reply was empty.");
        }
        int start = reply.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("The reply did not contain a JSON object.");
        }
        int end = matchingBrace(reply, start);
        if (end < 0) {
            throw new IllegalArgumentException("The JSON object was never closed (the reply looks cut off).");
        }
        try {
            return JsonParser.parseString(reply.substring(start, end + 1)).getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            String detail = e.getMessage() == null ? "" : e.getMessage().split("\\R")[0];
            throw new IllegalArgumentException("The reply was not valid JSON (" + cut(detail, 160) + ").");
        }
    }

    /** Index of the brace that closes the one at start, ignoring braces inside strings; -1 if unclosed. */
    static int matchingBrace(String text, int start) {
        int depth = 0;
        boolean inString = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String preview(String reply) {
        return cut(reply.replaceAll("\\s+", " ").trim(), 300);
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private static String text(JsonObject o, String key) {
        return JsonFields.text(o, key);
    }
}
