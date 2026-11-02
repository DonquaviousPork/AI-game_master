package gamemaster.rules;

import java.util.ArrayDeque;
import java.util.Deque;

/** Test dice that return a scripted sequence and refuse impossible values. */
class FixedDice implements Dice {

    private final Deque<Integer> values = new ArrayDeque<>();

    FixedDice(int... rolls) {
        for (int r : rolls) {
            values.addLast(r);
        }
    }

    @Override
    public int roll(int sides) {
        if (values.isEmpty()) {
            throw new IllegalStateException("The test asked for more rolls than it scripted.");
        }
        int value = values.removeFirst();
        if (value < 1 || value > sides) {
            throw new IllegalStateException("Scripted roll " + value + " is impossible on a d" + sides);
        }
        return value;
    }

    int remaining() {
        return values.size();
    }
}
