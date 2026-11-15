package gamemaster.ai;

import gamemaster.game.GameState;
import gamemaster.game.ObjectiveType;
import gamemaster.model.Character;
import gamemaster.model.Monster;

/**
 * How much opposition the AI may put in front of the party, decided by the engine from the party's size and health.
 * The final enemy of a defeat-target adventure is exempt from the head-count and HP budget but is capped in health.
 */
final class EncounterBudget {

    static final int MAX_AT_ONCE = 4;
    static final int MAX_SPAWNS_PER_TURN = 2;
    static final int MIN_BUDGET = 8;
    private static final double SHARE = 0.60;

    private EncounterBudget() {}

    static int livingHeroes(GameState s) {
        return (int) s.getParty().getCharacters().stream().filter(Character::isAlive).count();
    }

    static int partyMaxHealth(GameState s) {
        return s.getParty().getCharacters().stream().mapToInt(Character::getMaxHealth).sum();
    }

    /** Most enemies allowed at once (not counting the final enemy). */
    static int maxAtOnce(GameState s) {
        return Math.min(MAX_AT_ONCE, Math.max(1, livingHeroes(s)) + 1);
    }

    /** Total health the living ordinary enemies may add up to. */
    static int hpBudget(GameState s) {
        return Math.max(MIN_BUDGET, (int) Math.round(partyMaxHealth(s) * SHARE));
    }

    static boolean isBoss(GameState s, String name) {
        var sc = s.getScenario();
        return sc != null && sc.type() == ObjectiveType.DEFEAT_TARGET && name.equalsIgnoreCase(sc.target());
    }

    private static boolean ordinary(GameState s, Monster m) {
        return m.isAlive() && !isBoss(s, m.getName());
    }

    static int livingOrdinary(GameState s) {
        return (int) s.getMonsters().stream().filter(m -> ordinary(s, m)).count();
    }

    static int hpInUse(GameState s) {
        return s.getMonsters().stream().filter(m -> ordinary(s, m)).mapToInt(Monster::getMaxHealth).sum();
    }

    static int slotsLeft(GameState s) { return Math.max(0, maxAtOnce(s) - livingOrdinary(s)); }

    static int hpLeft(GameState s) { return Math.max(0, hpBudget(s) - hpInUse(s)); }

    /** The most health the boss may have: the party's combined maximum health. */
    static int bossCap(GameState s) { return Math.max(MIN_BUDGET, partyMaxHealth(s)); }
}
