package gamemaster.ai;

import com.google.gson.JsonObject;
import gamemaster.game.ActionKind;
import gamemaster.game.GameEngine;
import gamemaster.llm.ToolSpec;
import gamemaster.model.Monster;
import java.util.List;
import java.util.Objects;

/** Tool "spawn_monster": the AI proposes an enemy; the same limits as scenario generation decide if it enters. */
public class SpawnMonsterTool implements Tool {

    private final GameEngine engine;

    public SpawnMonsterTool(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("spawn_monster",
                "Introduce a new enemy into the story. This starts combat. The game limits how many enemies "
                        + "and how much total enemy health the party can face (see the game state), and at most "
                        + EncounterBudget.MAX_SPAWNS_PER_TURN + " can appear per turn.",
                Schema.object()
                        .string("name", "Name of the monster", true)
                        .integer("health", "Health, " + JsonFields.MIN_HEALTH + " to " + JsonFields.MAX_HEALTH, true)
                        .integer("armor_class", "Armor class, " + JsonFields.MIN_AC + " to " + JsonFields.MAX_AC, true)
                        .choice("weapon", "Weapon the monster uses", List.of("longsword", "quarterstaff", "dagger"), true)
                        .build());
    }

    @Override
    public String execute(JsonObject args) {
        // Story and Combat allow ability checks; Setup and Ended do not, so this means "the game is running".
        if (!engine.getState().getPhase().allows(ActionKind.ABILITY_CHECK)) {
            throw new IllegalStateException("Monsters cannot appear during the "
                    + engine.getState().getPhase().name() + " phase.");
        }
        var state = engine.getState();
        if (state.getSpawnsThisTurn() >= EncounterBudget.MAX_SPAWNS_PER_TURN) {
            throw new IllegalStateException("At most " + EncounterBudget.MAX_SPAWNS_PER_TURN
                    + " enemies can appear in one turn. Narrate what has happened and bring more in later.");
        }
        String clamped = "";
        if (args.has("health") && args.get("health").isJsonPrimitive() && args.get("health").getAsJsonPrimitive().isNumber()) {
            int asked = args.get("health").getAsInt();
            if (asked > JsonFields.MAX_HEALTH) {
                args.addProperty("health", JsonFields.MAX_HEALTH);     // a boss asked for 45 becomes 30; the limits below still apply
                clamped = " (the requested health " + asked + " was lowered to the maximum of " + JsonFields.MAX_HEALTH + ")";
            }
        }
        Monster monster = JsonFields.monster(args);
        var scenario = state.getScenario();
        if (scenario != null && scenario.type() == gamemaster.game.ObjectiveType.DEFEAT_TARGET
                && monster.getName().equalsIgnoreCase(scenario.target()) && !state.inFinale()) {
            throw new IllegalStateException(monster.getName() + " is the final enemy and cannot appear yet. The story is in "
                    + state.progressText() + ". Finish this act first (see the game state).");
        }
        String note = "";
        if (EncounterBudget.isBoss(state, monster.getName())) {
            int cap = EncounterBudget.bossCap(state);
            if (monster.getMaxHealth() > cap) {
                monster = new Monster(monster.getName(), cap, monster.getArmorClass(), monster.getWeapon());
                note = " (its health was lowered to " + cap + ", the most this party can face)";
            }
        } else {
            if (EncounterBudget.slotsLeft(state) < 1) {
                throw new IllegalStateException("Too many enemies for this party: at most "
                        + EncounterBudget.maxAtOnce(state) + " at once (not counting the final enemy), and "
                        + EncounterBudget.livingOrdinary(state) + " are alive. Let the fight play out first.");
            }
            if (monster.getMaxHealth() > EncounterBudget.hpLeft(state)) {
                throw new IllegalStateException("That enemy is too strong for this party right now: the enemy health "
                        + "budget is " + EncounterBudget.hpBudget(state) + " in total and " + EncounterBudget.hpLeft(state)
                        + " is left. Spawn an enemy with health " + EncounterBudget.hpLeft(state) + " or less.");
            }
        }
        state.noteSpawn();
        engine.addMonster(monster);
        return monster.getName() + " appears (HP " + monster.getMaxHealth() + ", AC " + monster.getArmorClass()
                + ")" + note + clamped + ". Phase: " + engine.getState().getPhase().name() + ".";
    }
}
