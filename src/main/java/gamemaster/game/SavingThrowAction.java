package gamemaster.game;

import gamemaster.model.Combatant;
import gamemaster.model.Stat;
import gamemaster.rules.RuleEngine;
import gamemaster.rules.SaveResult;
import java.util.Objects;

/**
 * F10: a saving throw. The target rolls d20 plus a stat modifier to resist a hazard such as a trap, a spell
 * or poison. If the save fails, the hazard's damage is applied; undo restores exactly what was lost.
 */
public class SavingThrowAction implements Action {

    private final Combatant target;
    private final Stat stat;
    private final int difficulty;
    private final int damageOnFail;
    private final String hazard;
    private int damageTaken;

    public SavingThrowAction(Combatant target, Stat stat, int difficulty, int damageOnFail, String hazard) {
        this.target = Objects.requireNonNull(target, "target");
        this.stat = Objects.requireNonNull(stat, "stat");
        if (damageOnFail < 0) {
            throw new IllegalArgumentException("Damage cannot be negative.");
        }
        this.difficulty = difficulty;
        this.damageOnFail = damageOnFail;
        this.hazard = hazard == null || hazard.isBlank() ? "the danger" : hazard.trim();
    }

    @Override public ActionKind kind() { return ActionKind.SAVING_THROW; }

    @Override
    public ActionResult execute(GameState state, RuleEngine rules) {
        SaveResult r = rules.rollSavingThrow(target, stat, difficulty);
        String head = target.getName() + " makes a " + stat + " saving throw against " + hazard
                + " (rolled " + r.total() + " vs " + r.difficulty() + "): ";
        if (r.success()) {
            damageTaken = 0;
            return new ActionResult(head + "saved.");
        }
        int before = target.getHealth();
        target.applyDamage(damageOnFail);
        damageTaken = before - target.getHealth();
        String text = head + "failed, takes " + damageTaken + " damage.";
        if (!target.isAlive()) {
            text += " " + target.getName() + " is defeated.";
        }
        return new ActionResult(text);
    }

    @Override
    public void undo(GameState state) {
        if (damageTaken > 0) {
            target.heal(damageTaken);
            damageTaken = 0;
        }
    }
}
