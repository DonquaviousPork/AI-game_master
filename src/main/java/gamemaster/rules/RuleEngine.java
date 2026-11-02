package gamemaster.rules;

import gamemaster.model.Combatant;
import gamemaster.model.Stat;
import gamemaster.model.Weapon;
import java.util.Objects;

/**
 * All dice mechanics (F07 to F10). This class only rolls and compares; it never
 * changes a combatant. The caller applies the results, which keeps the AI out
 * of the arithmetic entirely.
 */
public class RuleEngine {

    /** Used when an effect arrives without a difficulty value. */
    public static final int DEFAULT_DIFFICULTY = 12;

    private static final int D20 = 20;

    private final Dice dice;

    public RuleEngine(Dice dice) {
        this.dice = Objects.requireNonNull(dice, "dice");
    }

    public RuleEngine() {
        this(new RandomDice());
    }

    /** F07: d20 + stat modifier, success when the total meets the difficulty. */
    public CheckResult rollAbilityCheck(Combatant character, Stat stat, int difficulty) {
        requireAlive(character, "make an ability check");
        requirePositive(difficulty);
        int roll = dice.roll(D20);
        int modifier = character.getModifier(stat);
        int total = roll + modifier;
        return new CheckResult(roll, modifier, total, difficulty, total >= difficulty);
    }

    public CheckResult rollAbilityCheck(Combatant character, Stat stat) {
        return rollAbilityCheck(character, stat, DEFAULT_DIFFICULTY);
    }

    /** F08: d20 + the weapon stat's modifier against the target's armor class. */
    public AttackResult rollAttack(Combatant attacker, Weapon weapon, Combatant target) {
        requireAlive(attacker, "attack");
        Objects.requireNonNull(weapon, "weapon");
        Objects.requireNonNull(target, "target");
        if (!target.isAlive()) {
            throw new IllegalStateException(target.getName() + " is already defeated.");
        }
        int roll = dice.roll(D20);
        int modifier = attacker.getModifier(weapon.stat());
        int total = roll + modifier;
        int armorClass = target.getArmorClass();
        return new AttackResult(roll, modifier, total, armorClass, total >= armorClass);
    }

    /**
     * F09: the weapon's die plus the weapon stat's modifier, never below 1.
     * Returns the amount only; the caller passes it to Combatant.applyDamage().
     */
    public int rollDamage(Combatant attacker, Weapon weapon) {
        requireAlive(attacker, "deal damage");
        Objects.requireNonNull(weapon, "weapon");
        int roll = dice.roll(weapon.damageSides());
        return Math.max(1, roll + attacker.getModifier(weapon.stat()));
    }

    /** F10: d20 + stat modifier, success when the total meets the difficulty. */
    public SaveResult rollSavingThrow(Combatant target, Stat stat, int difficulty) {
        requireAlive(target, "make a saving throw");
        requirePositive(difficulty);
        int roll = dice.roll(D20);
        int modifier = target.getModifier(stat);
        int total = roll + modifier;
        return new SaveResult(roll, modifier, total, difficulty, total >= difficulty);
    }

    public SaveResult rollSavingThrow(Combatant target, Stat stat) {
        return rollSavingThrow(target, stat, DEFAULT_DIFFICULTY);
    }

    private static void requireAlive(Combatant c, String action) {
        Objects.requireNonNull(c, "character");
        if (!c.isAlive()) {
            throw new IllegalStateException(c.getName() + " is defeated and cannot " + action + ".");
        }
    }

    private static void requirePositive(int difficulty) {
        if (difficulty < 1) {
            throw new IllegalArgumentException("Difficulty must be at least 1.");
        }
    }
}
