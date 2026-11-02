package gamemaster.rules;

import java.util.Random;

public class RandomDice implements Dice {

    private final Random random;

    public RandomDice() {
        this(new Random());
    }

    /** Lets a caller supply a seeded Random for repeatable sequences. */
    public RandomDice(Random random) {
        this.random = random;
    }

    @Override
    public int roll(int sides) {
        if (sides < 1) {
            throw new IllegalArgumentException("A die needs at least 1 side");
        }
        return random.nextInt(sides) + 1;
    }
}
