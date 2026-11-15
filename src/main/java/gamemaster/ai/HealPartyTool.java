package gamemaster.ai;

import com.google.gson.JsonObject;
import gamemaster.game.GameEngine;
import gamemaster.game.HealAction;
import gamemaster.llm.ToolSpec;
import gamemaster.model.Combatant;
import java.util.Objects;

/** Tool "heal_party": a wizard casts Healing Light. The engine rolls and applies the healing. */
public class HealPartyTool implements Tool {

    private final GameEngine engine;

    public HealPartyTool(GameEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("heal_party",
                "A wizard casts Healing Light: every living hero recovers a d6 plus the wizard's WISDOM modifier. "
                        + "Only wizards can, at most " + gamemaster.game.GameState.HEALS_PER_ADVENTURE
                        + " times per adventure. Use it when the player asks a wizard to heal the party.",
                Schema.object()
                        .string("caster", "Exact name of the wizard casting the spell", true)
                        .build());
    }

    @Override
    public String execute(JsonObject args) {
        Combatant caster = Lookup.find(engine.getState(), JsonFields.text(args, "caster"));
        String result = engine.resolveAction(new HealAction(caster)).summary();
        return result + " Phase: " + engine.getState().getPhase().name() + ".";
    }
}
