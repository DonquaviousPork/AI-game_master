package gamemaster.game;

import gamemaster.model.Combatant;
import gamemaster.model.Stat;
import gamemaster.rules.CheckResult;
import gamemaster.rules.RuleEngine;
import java.util.Objects;

/** F07: a d20 ability check. It changes no state, so undo has nothing to reverse. */
public class AbilityCheckAction implements Action {

    private final Combatant actor;
    private final Stat stat;
    private final int difficulty;

    public AbilityCheckAction(Combatant actor, Stat stat, int difficulty) {
        this.actor = Objects.requireNonNull(actor, "actor");
        this.stat = Objects.requireNonNull(stat, "stat");
        this.difficulty = difficulty;
    }

    public AbilityCheckAction(Combatant actor, Stat stat) {
        this(actor, stat, RuleEngine.DEFAULT_DIFFICULTY);
    }

    @Override public ActionKind kind() { return ActionKind.ABILITY_CHECK; }

    @Override
    public ActionResult execute(GameState state, RuleEngine rules) {
        CheckResult r = rules.rollAbilityCheck(actor, stat, difficulty);
        return new ActionResult(actor.getName() + " makes a " + stat + " check (rolled " + r.total()
                + " vs " + r.difficulty() + "): " + (r.success() ? "success." : "failure."));
    }

    @Override public void undo(GameState state) { /* nothing to reverse */ }
}
