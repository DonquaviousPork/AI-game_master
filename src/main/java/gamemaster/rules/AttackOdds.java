package gamemaster.rules;

import gamemaster.model.Combatant;
import gamemaster.model.Weapon;

/**
 * How good an attack is against a given armor class: the chance to hit, the damage range and an overall strength
 * rating. It uses the same rules as {@link RuleEngine} (d20 + modifiers against armor class, a natural 20 always
 * hits, a natural 1 always misses, damage is the die plus the modifier and never below 1) but rolls nothing.
 * The rating is based on the damage expected per attack and does not count the extra damage of critical hits.
 */
public record AttackOdds(int hitPercent, int minDamage, int maxDamage, double averageDamage,
                         double expectedDamage, Rating rating) {

    /** A simple label for how strong an attack is, from weakest to strongest. */
    public enum Rating {
        WEAK("Weak"), FAIR("Fair"), STRONG("Strong"), VERY_STRONG("Very strong");

        private final String label;

        Rating(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Expected damage per attack below which an attack is Weak, Fair and Strong; at or above the last it is Very strong. */
    static final double FAIR_FROM = 2.0, STRONG_FROM = 3.5, VERY_STRONG_FROM = 5.0;

    private static final int D20 = 20;

    public static AttackOdds of(Combatant attacker, Weapon weapon, int targetArmorClass) {
        return of(weapon.damageSides(), attacker.getModifier(weapon.stat()), weapon.attackBonus(), targetArmorClass);
    }

    /**
     * @param damageSides   sides of the damage die
     * @param statModifier  the weapon stat's modifier (added to the hit roll and to the damage)
     * @param attackBonus   the attack's own bonus to hit
     * @param armorClass    the target's armor class
     */
    public static AttackOdds of(int damageSides, int statModifier, int attackBonus, int armorClass) {
        int hits = 0;
        for (int roll = 1; roll <= D20; roll++) {
            boolean hit = roll == D20 || (roll != 1 && roll + statModifier + attackBonus >= armorClass);
            if (hit) {
                hits++;
            }
        }
        int min = Math.max(1, 1 + statModifier);
        int max = Math.max(1, damageSides + statModifier);
        double total = 0;
        for (int die = 1; die <= damageSides; die++) {
            total += Math.max(1, die + statModifier);
        }
        double average = total / damageSides;
        double chance = hits / (double) D20;
        double expected = chance * average;
        Rating rating = expected >= VERY_STRONG_FROM ? Rating.VERY_STRONG
                : expected >= STRONG_FROM ? Rating.STRONG
                : expected >= FAIR_FROM ? Rating.FAIR : Rating.WEAK;
        return new AttackOdds(hits * 100 / D20, min, max, average, expected, rating);
    }

    /** "55%" style text. */
    public String hitText() {
        return hitPercent + "%";
    }

    /** "3-10" style text (a single number when the range is one value). */
    public String damageText() {
        return minDamage == maxDamage ? String.valueOf(minDamage) : minDamage + "-" + maxDamage;
    }
}
