package gamemaster.model;

import java.util.Objects;

/**
 * A weapon or attack move: a damage die, the ability score that drives both its attack and damage
 * rolls, and a bonus (or penalty) added to the attack roll. Quick, light attacks hit more often but
 * hurt less; heavy attacks hit less often but hurt more.
 */
public record Weapon(String name, int damageSides, Stat stat, int attackBonus) {

    public static final Weapon LONGSWORD = new Weapon("Longsword", 8, Stat.STRENGTH);
    public static final Weapon QUARTERSTAFF = new Weapon("Quarterstaff", 6, Stat.STRENGTH);
    public static final Weapon DAGGER = new Weapon("Dagger", 4, Stat.DEXTERITY);

    public Weapon(String name, int damageSides, Stat stat) {
        this(name, damageSides, stat, 0);
    }

    public Weapon {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Weapon name must not be blank");
        }
        if (damageSides < 1) {
            throw new IllegalArgumentException("A damage die needs at least 1 side");
        }
        Objects.requireNonNull(stat, "stat");
    }

    /** Finds one of the built-in weapons by name, ignoring case. */
    public static Weapon byName(String name) {
        for (Weapon w : new Weapon[] {LONGSWORD, QUARTERSTAFF, DAGGER}) {
            if (w.name().equalsIgnoreCase(name == null ? "" : name.trim())) {
                return w;
            }
        }
        throw new IllegalArgumentException("Unknown weapon: " + name);
    }
}
