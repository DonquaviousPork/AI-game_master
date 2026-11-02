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
    private int damageDealt;

    public AttackAction(Combatant attacker, Weapon weapon, Combatant target) {
        this.attacker = Objects.requireNonNull(attacker, "attacker");
        this.weapon = Objects.requireNonNull(weapon, "weapon");
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override public ActionKind kind() { return ActionKind.ATTACK; }

    @Override
    public ActionResult execute(GameState state, RuleEngine rules) {
        AttackResult hit = rules.rollAttack(attacker, weapon, target);
        if (!hit.hit()) {
            damageDealt = 0;
            return new ActionResult(attacker.getName() + " attacks " + target.getName()
                    + " (rolled " + hit.total() + " vs AC " + hit.targetArmorClass() + ") and misses.");
        }
        int before = target.getHealth();
        target.applyDamage(rules.rollDamage(attacker, weapon));
        damageDealt = before - target.getHealth();
        String text = attacker.getName() + " hits " + target.getName() + " (rolled " + hit.total()
                + " vs AC " + hit.targetArmorClass() + ") for " + damageDealt + " damage.";
        if (!target.isAlive()) {
            text += " " + target.getName() + " is defeated.";
        }
        return new ActionResult(text);
    }

    @Override
    public void undo(GameState state) {
        if (damageDealt > 0) {
            target.heal(damageDealt);
            damageDealt = 0;
        }
    }
}
