package gamemaster.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * A playable character. Note: this class shadows java.lang.Character inside
 * the gamemaster.model package; use the wrapper type's full name if you ever need it here.
 */
public class Character implements Combatant {

    private final String name;
    private final CharacterClass characterClass;
    private final Map<Stat, Integer> stats;
    private final int maxHealth;
    private final int armorClass;
    private int health;

    public Character(String name, CharacterClass characterClass, Map<Stat, Integer> stats) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Character name must not be blank");
        }
        this.name = name.trim();
        this.characterClass = Objects.requireNonNull(characterClass, "characterClass");
        this.stats = new EnumMap<>(Stat.class);
        for (Stat s : Stat.values()) {
            Integer score = stats.get(s);
            if (score == null) {
                throw new IllegalArgumentException("Missing score for " + s);
            }
            this.stats.put(s, score);
        }
        this.maxHealth = 10 + this.stats.get(Stat.CONSTITUTION);
        this.health = maxHealth;
        this.armorClass = 10 + modifier(this.stats.get(Stat.DEXTERITY));
    }

    /** Standard ability modifier: (score - 10) / 2, rounded down. */
    public static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }

    @Override public int getStat(Stat stat) { return stats.get(stat); }

    @Override public int getModifier(Stat stat) { return modifier(stats.get(stat)); }

    /** Reduces health by amount, never below 0. */
    @Override
    public void applyDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage must not be negative");
        }
        health = Math.max(0, health - amount);
    }

    /** Restores health by amount, never above the maximum. */
    @Override
    public void heal(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Healing must not be negative");
        }
        health = Math.min(maxHealth, health + amount);
    }

    @Override public boolean isAlive() { return health > 0; }

    @Override public String getName() { return name; }
    public CharacterClass getCharacterClass() { return characterClass; }
    @Override public int getHealth() { return health; }
    @Override public int getMaxHealth() { return maxHealth; }
    @Override public int getArmorClass() { return armorClass; }

    @Override public String toString() {
        return name + " the " + characterClass.name() + " (HP " + health + "/" + maxHealth + ", AC " + armorClass + ")";
    }
}
