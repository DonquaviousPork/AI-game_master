package gamemaster.game;

import gamemaster.model.Combatant;
import gamemaster.model.Weapon;
import gamemaster.rules.AttackResult;
import gamemaster.rules.RuleEngine;
import java.util.Objects;

/** F08 + F09: roll to hit; on a hit roll damage and apply it to the target. */
public class AttackAction implements Action {

    private final Combatant attacker;
    private final Weapon weapon;
    private final Combatant target;
    private final StatusKind effect;
    private int damageDealt;
    private int effectBefore = -1;     // turns the effect had before this attack put it on; -1 when this attack did not

    public AttackAction(Combatant attacker, Weapon weapon, Combatant target) {
        this(attacker, weapon, target, null);
    }

    /** An attack that also puts {@code effect} on the target when it hits (the target must survive the blow). */
    public AttackAction(Combatant attacker, Weapon weapon, Combatant target, StatusKind effect) {
        this.effect = effect;
        this.attacker = Objects.requireNonNull(attacker, "attacker");
        this.weapon = Objects.requireNonNull(weapon, "weapon");
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override public ActionKind kind() { return ActionKind.ATTACK; }

    @Override
    public ActionResult execute(GameState state, RuleEngine rules) {
        int chilled = state.statusTurns(attacker.getName(), StatusKind.CHILL) > 0 ? -StatusKind.CHILL_PENALTY : 0;
        int weakened = state.statusTurns(target.getName(), StatusKind.WEAKEN) > 0 ? -StatusKind.WEAKEN_AC : 0;
        AttackResult hit = rules.rollAttack(attacker, weapon, target, chilled, weakened);
        effectBefore = -1;
        if (!hit.hit()) {
            damageDealt = 0;
            String what = hit.fumble() ? " FUMBLE! (natural 1)" : "";
            return new ActionResult(attacker.getName() + " attacks " + target.getName() + " with " + weapon.name()
                    + " (rolled " + hit.total() + " vs AC " + hit.targetArmorClass() + ") and misses." + what);
        }
        int before = target.getHealth();
        target.applyDamage(rules.rollDamage(attacker, weapon, hit.critical()));
        damageDealt = before - target.getHealth();
        String text = attacker.getName() + " hits " + target.getName() + " with " + weapon.name() + " (rolled "
                + hit.total() + " vs AC " + hit.targetArmorClass() + ") for " + damageDealt + " damage."
                + (hit.critical() ? " CRITICAL HIT! (natural 20, double damage dice)" : "");
        if (!target.isAlive()) {
            text += " " + target.getName() + " is defeated.";
        } else if (effect != null) {
            effectBefore = state.applyStatus(target.getName(), effect);
            text += " " + target.getName() + " is " + effect.adjective() + " (" + effect.effectText() + ", "
                    + StatusKind.DURATION + " turns).";
        }
        return new ActionResult(text);
    }

    @Override
    public void undo(GameState state) {
        if (effectBefore >= 0) {
            state.setStatusTurns(target.getName(), effect, effectBefore);
            effectBefore = -1;
        }
        if (damageDealt > 0) {
            target.heal(damageDealt);
            damageDealt = 0;
        }
    }
}
