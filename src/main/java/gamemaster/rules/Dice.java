package gamemaster.rules;

/**
 * Source of die rolls. The game uses RandomDice; tests supply fixed values so
 * every roll is predictable.
 */
public interface Dice {

    /** Rolls one die with the given number of sides and returns 1..sides. */
    int roll(int sides);
}
