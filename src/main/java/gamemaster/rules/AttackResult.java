package gamemaster.rules;

/**
 * Outcome of an attack roll (F08). A natural 20 is a critical hit (it always hits and doubles the damage dice);
 * a natural 1 is a fumble (it always misses).
 */
public record AttackResult(int roll, int modifier, int total, int targetArmorClass, boolean hit,
                           boolean critical, boolean fumble) {

    public AttackResult(int roll, int modifier, int total, int targetArmorClass, boolean hit) {
        this(roll, modifier, total, targetArmorClass, hit, false, false);
    }
}
