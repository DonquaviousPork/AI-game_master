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

    static final int MAX_LIVING_MONSTERS = 4;

    private final GameEngine engine;

    public SpawnMonsterTool(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("spawn_monster",
                "Introduce a new enemy into the story. This starts combat. At most "
                        + MAX_LIVING_MONSTERS + " living enemies at once.",
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
        long living = engine.getState().getMonsters().stream().filter(Monster::isAlive).count();
        if (living >= MAX_LIVING_MONSTERS) {
            throw new IllegalStateException("There are already " + living + " living monsters.");
        }
        Monster monster = JsonFields.monster(args);
        engine.addMonster(monster);
        return monster.getName() + " appears (HP " + monster.getMaxHealth() + ", AC " + monster.getArmorClass()
                + "). Phase: " + engine.getState().getPhase().name() + ".";
    }
}
