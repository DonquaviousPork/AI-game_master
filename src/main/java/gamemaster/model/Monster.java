package gamemaster.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * An enemy. Unlike a Character it has no class and no point-buy: the scenario
 * supplies its health, armor class, weapon and any stats that differ from the
 * default score of 10 (a modifier of 0).
 */
public class Monster implements Combatant {

    private static final int DEFAULT_SCORE = 10;

    private final String name;
    private final Map<Stat, Integer> stats;
    private final int maxHealth;
    private final int armorClass;
    private final Weapon weapon;
    private int health;

    public Monster(String name, int maxHealth, int armorClass, Weapon weapon, Map<Stat, Integer> stats) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Monster name must not be blank");
        }
        if (maxHealth < 1) {
            throw new IllegalArgumentException("Monster health must be at least 1");
        }
        if (armorClass < 1) {
            throw new IllegalArgumentException("Monster armor class must be at least 1");
        }
        this.name = name.trim();
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.armorClass = armorClass;
        this.weapon = Objects.requireNonNull(weapon, "weapon");
        this.stats = new EnumMap<>(Stat.class);
        for (Stat s : Stat.values()) {
            Integer score = stats == null ? null : stats.get(s);
            this.stats.put(s, score == null ? DEFAULT_SCORE : score);
        }
    }

    public Monster(String name, int maxHealth, int armorClass, Weapon weapon) {
        this(name, maxHealth, armorClass, weapon, null);
    }

    public Weapon getWeapon() { return weapon; }

    @Override public String getName() { return name; }
    @Override public int getStat(Stat stat) { return stats.get(stat); }
    @Override public int getModifier(Stat stat) { return Character.modifier(stats.get(stat)); }
    @Override public int getHealth() { return health; }
    @Override public int getMaxHealth() { return maxHealth; }
    @Override public int getArmorClass() { return armorClass; }
    @Override public boolean isAlive() { return health > 0; }

    @Override
    public void applyDamage(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Damage must not be negative");
        }
        health = Math.max(0, health - amount);
    }

    @Override
    public void heal(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Healing must not be negative");
        }
        health = Math.min(maxHealth, health + amount);
    }

    @Override public String toString() {
        return name + " (HP " + health + "/" + maxHealth + ", AC " + armorClass + ")";
    }
}
