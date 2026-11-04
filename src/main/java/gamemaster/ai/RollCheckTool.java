package gamemaster.ai;

import com.google.gson.JsonObject;
import gamemaster.game.AbilityCheckAction;
import gamemaster.game.GameEngine;
import gamemaster.llm.ToolSpec;
import gamemaster.model.Combatant;
import gamemaster.model.Stat;
import java.util.Arrays;
import java.util.Objects;

/** Tool "roll_check": the engine rolls a d20 ability check for a character. The AI never sees a die. */
public class RollCheckTool implements Tool {

    static final int MIN_DIFFICULTY = 5, MAX_DIFFICULTY = 20;

    private final GameEngine engine;

    public RollCheckTool(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("roll_check",
                "Make a character attempt something uncertain. The game rolls a d20 plus the stat modifier "
                        + "and reports success or failure. Use it instead of deciding the outcome yourself.",
                Schema.object()
                        .string("character", "Name of the character making the attempt", true)
                        .choice("stat", "Ability used for the attempt",
                                Arrays.stream(Stat.values()).map(Enum::name).toList(), true)
                        .integer("difficulty", "Target number, " + MIN_DIFFICULTY + " (easy) to "
                                + MAX_DIFFICULTY + " (very hard). Default 12.", false)
                        .build());
    }

    @Override
    public String execute(JsonObject args) {
        Combatant who = Lookup.find(engine.getState(), JsonFields.text(args, "character"));
        Stat stat;
        try {
            stat = Stat.valueOf(JsonFields.text(args, "stat").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("\"stat\" must be one of " + Arrays.toString(Stat.values()) + ".");
        }
        int difficulty = args.has("difficulty")
                ? JsonFields.number(args, "difficulty", MIN_DIFFICULTY, MAX_DIFFICULTY)
                : 12;
        return engine.resolveAction(new AbilityCheckAction(who, stat, difficulty)).summary();
    }
}
