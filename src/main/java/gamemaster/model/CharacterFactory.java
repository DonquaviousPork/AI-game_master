package gamemaster.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory Method for characters: one method per way of creating a character,
 * so callers never build stats or run validation themselves.
 */
public class CharacterFactory {

    public static final int BASE_STAT = 8;
    public static final int MAX_STAT = 15;
    public static final int POINT_POOL = 20;

    /** F02: a character with the class's default stat spread. */
    public Character createFromClass(String name, CharacterClass classType) {
        return new Character(name, classType, classType.defaultStats());
    }

    /**
     * F03: a character built from player-allocated points. Each allocation is the
     * number of points added on top of BASE_STAT; they must total POINT_POOL and
     * no resulting score may exceed MAX_STAT.
     */
    public Character createCustom(String name, CharacterClass classType, Map<Stat, Integer> allocations) {
        Map<Stat, Integer> scores = new EnumMap<>(Stat.class);
        int total = 0;
        for (Stat stat : Stat.values()) {
            int points = allocations.getOrDefault(stat, 0);
            if (points < 0) {
                throw new IllegalArgumentException("Points for " + stat + " cannot be negative.");
            }
            int score = BASE_STAT + points;
            if (score > MAX_STAT) {
                throw new IllegalArgumentException(
                        stat + " would be " + score + ", above the maximum of " + MAX_STAT + ".");
            }
            scores.put(stat, score);
            total += points;
        }
        if (total != POINT_POOL) {
            throw new IllegalArgumentException(
                    "You must spend exactly " + POINT_POOL + " points (you spent " + total + ").");
        }
        return new Character(name, classType, scores);
    }
}
