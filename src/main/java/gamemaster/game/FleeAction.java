package gamemaster.game;

import gamemaster.model.Combatant;
import gamemaster.model.Monster;
import gamemaster.model.Stat;
import gamemaster.rules.CheckResult;
import gamemaster.rules.RuleEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** The party tries to escape with a DEXTERITY check; on success the encounter ends. */
public class FleeAction implements Action {

    private final Combatant runner;
    private List<Monster> removed = List.of();

    public FleeAction(Combatant runner) {
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    @Override public ActionKind kind() { return ActionKind.FLEE; }

    @Override
    public ActionResult execute(GameState state, RuleEngine rules) {
        CheckResult r = rules.rollAbilityCheck(runner, Stat.DEXTERITY);
        if (!r.success()) {
            removed = List.of();
            return new ActionResult(runner.getName() + " tries to flee (rolled " + r.total()
                    + " vs " + r.difficulty() + ") but cannot get away.");
        }
        removed = new ArrayList<>(state.getMonsters());
        state.setMonsters(List.of());
        return new ActionResult(runner.getName() + " leads the party to safety (rolled " + r.total()
                + " vs " + r.difficulty() + ").");
    }

    @Override
    public void undo(GameState state) {
        if (!removed.isEmpty()) {
            state.setMonsters(removed);
            removed = List.of();
        }
    }
}
