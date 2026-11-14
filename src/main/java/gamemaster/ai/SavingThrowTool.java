package gamemaster.ai;

import com.google.gson.JsonObject;
import gamemaster.game.GameEngine;
import gamemaster.game.SavingThrowAction;
import gamemaster.llm.ToolSpec;
import gamemaster.model.Combatant;
import gamemaster.model.Stat;
import java.util.Arrays;
import java.util.Objects;

/** Tool "saving_throw": the engine rolls a character's save against a hazard and applies the damage if it fails. */
public class SavingThrowTool implements Tool {

    static final int MIN_DIFFICULTY = 5, MAX_DIFFICULTY = 20, MAX_DAMAGE = 10;

    private final GameEngine engine;

    public SavingThrowTool(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("saving_throw",
                "Let a character try to resist a hazard such as a trap, a spell or poison. The game rolls a d20 "
                        + "plus the stat modifier. If the save fails, the damage you give is applied by the game.",
                Schema.object()
                        .string("character", "Name of the hero or monster making the save", true)
                        .choice("stat", "Ability used to resist",
                                Arrays.stream(Stat.values()).map(Enum::name).toList(), true)
                        .integer("difficulty", "Target number, " + MIN_DIFFICULTY + " (easy) to "
                                + MAX_DIFFICULTY + " (very hard). Default 12.", false)
                        .integer("damage_on_fail", "Damage taken if the save fails, 0 to " + MAX_DAMAGE
                                + ". Default 0.", false)
                        .string("hazard", "What the character is resisting, for example \"the falling rocks\"", false)
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
        int difficulty = args.has("difficulty") ? JsonFields.number(args, "difficulty", MIN_DIFFICULTY, MAX_DIFFICULTY) : 12;
        int damage = args.has("damage_on_fail") ? JsonFields.number(args, "damage_on_fail", 0, MAX_DAMAGE) : 0;
        String hazard = args.has("hazard") && !args.get("hazard").isJsonNull() ? args.get("hazard").getAsString() : null;
        String result = engine.resolveAction(new SavingThrowAction(who, stat, difficulty, damage, hazard)).summary();
        return result + " " + who.getName() + " now has " + who.getHealth() + "/" + who.getMaxHealth()
                + " HP. Phase: " + engine.getState().getPhase().name() + ".";
    }
}
