package gamemaster.gui;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * What a hero or enemy card does when something happens to it: shake and flash on a hit, a bigger gold pop on a
 * critical hit, a sidestep on a miss, and a short cheeky line in a speech bubble. It is worked out from the change in
 * the card's health and from the new rule lines, so it needs nothing from the engine.
 */
record CardReaction(Kind kind, int amount, String bubble) {

    enum Kind { HIT, CRIT, MISS, HEAL, DEFEAT, CHEER }

    static final long DURATION_MS = 1600;

    private static final String[] HIT = {"Ouch!", "Hey, rude!", "That stings!", "Oof.", "I felt that."};
    private static final String[] CRIT = {"CRITICAL!!", "Brutal!", "Ow, my ancestors!", "That was unfair!"};
    private static final String[] MISS = {"Too slow!", "Missed me!", "Nice try.", "Whoosh!"};
    private static final String[] HEAL = {"Ahh, better.", "Much better.", "Thanks!"};
    private static final String[] DEFEAT = {"Tell my story...", "Not like this.", "I regret nothing.", "Gah!"};
    private static final String[] DEFEAT_ENEMY = {"Impossible...", "Curse you!", "Back to the dark...", "Argh!"};
    private static final String[] CHEER = {"Victory!", "Nailed it!", "Easy.", "We did it!"};

    /**
     * Works out the reaction of one card, or null when nothing happened to it.
     *
     * @param prevHealth the card's health at the last update, or null when it was not there (no reaction then)
     * @param wasAlive   whether it was alive at the last update
     * @param newLines   the rule lines added since the last update
     * @param cheer      true once, when the party has just won
     */
    static CardReaction detect(GameViewModel.Card now, Integer prevHealth, boolean wasAlive, List<String> newLines,
                               boolean cheer, boolean hero) {
        if (cheer && hero && now.alive()) {
            return make(Kind.CHEER, 0, now.name(), true);
        }
        if (prevHealth == null) {
            return null;
        }
        if (!wasAlive) {
            return null;        // a fallen card says nothing more, whatever the log mentions
        }
        boolean crit = false;
        boolean miss = false;
        for (String line : newLines) {
            if (line.contains(" hits " + now.name() + " ") && line.contains("CRITICAL HIT")) {
                crit = true;
            }
            if (line.contains(" attacks " + now.name() + " ") && line.contains("misses")) {
                miss = true;
            }
        }
        int lost = prevHealth - now.health();
        if (lost > 0) {
            if (wasAlive && !now.alive()) {
                return make(Kind.DEFEAT, lost, now.name(), hero);
            }
            return make(crit ? Kind.CRIT : Kind.HIT, lost, now.name(), hero);
        }
        if (lost < 0) {
            return make(Kind.HEAL, -lost, now.name(), hero);
        }
        return miss && now.alive() ? make(Kind.MISS, 0, now.name(), hero) : null;
    }

    private static CardReaction make(Kind kind, int amount, String name, boolean hero) {
        String[] pool = switch (kind) {
            case HIT -> HIT;
            case CRIT -> CRIT;
            case MISS -> MISS;
            case HEAL -> HEAL;
            case DEFEAT -> hero ? DEFEAT : DEFEAT_ENEMY;
            case CHEER -> CHEER;
        };
        Random rnd = new Random(Objects.hash(name, amount, kind));
        return new CardReaction(kind, amount, pool[rnd.nextInt(pool.length)]);
    }

    /** The floating text above the card: "-6", "-12!", "miss", "+3", or nothing. */
    String floating() {
        return switch (kind) {
            case HIT, DEFEAT -> "-" + amount;
            case CRIT -> "-" + amount + "!";
            case MISS -> "miss";
            case HEAL -> "+" + amount;
            case CHEER -> "";
        };
    }
}
