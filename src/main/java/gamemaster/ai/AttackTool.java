package gamemaster.ai;

import com.google.gson.JsonObject;
import gamemaster.game.AttackAction;
import gamemaster.game.GameEngine;
import gamemaster.llm.ToolSpec;
import gamemaster.model.Combatant;
import java.util.Objects;

/** Tool "attack": one combatant attacks an opponent. The engine rolls to hit and for damage. */
public class AttackTool implements Tool {

    private final GameEngine engine;

    public AttackTool(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("attack",
                "Make one combatant attack an opponent during combat. The game rolls to hit and for damage. "
                        + "Heroes may only attack monsters and monsters may only attack heroes.",
                Schema.object()
                        .string("attacker", "Name of the attacking hero or monster", true)
                        .string("target", "Name of the opponent being attacked", true)
                        .build());
    }

    @Override
    public String execute(JsonObject args) {
        var state = engine.getState();
        Combatant attacker = Lookup.find(state, JsonFields.text(args, "attacker"));
        Combatant target = Lookup.find(state, JsonFields.text(args, "target"));
        if (Lookup.isPlayerSide(state, attacker) == Lookup.isPlayerSide(state, target)) {
            throw new IllegalArgumentException("Heroes and monsters fight each other; "
                    + attacker.getName() + " cannot attack " + target.getName() + ".");
        }
        String result = engine.resolveAction(new AttackAction(attacker, Lookup.weaponOf(attacker), target)).summary();
        return result + " " + target.getName() + " now has " + target.getHealth() + "/"
                + target.getMaxHealth() + " HP. Phase: " + state.getPhase().name() + ".";
    }
}
