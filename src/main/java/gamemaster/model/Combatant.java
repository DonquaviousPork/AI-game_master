package gamemaster.model;

/**
 * Anything that can take part in combat: a player Character or a Monster.
 * The RuleEngine depends on this interface only, so it never needs to know
 * which kind of combatant it is rolling for.
 */
public interface Combatant {

    String getName();

    int getStat(Stat stat);

    int getModifier(Stat stat);

    int getHealth();

    int getMaxHealth();

    int getArmorClass();

    boolean isAlive();

    /** Reduces health by amount, never below 0. Negative amounts are rejected. */
    void applyDamage(int amount);

    /** Restores health by amount, never above the maximum. Negative amounts are rejected. */
    void heal(int amount);
}
