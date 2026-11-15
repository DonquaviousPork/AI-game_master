package gamemaster.game;

import java.util.Locale;

/** What the engine needs to see before an act of the story counts as finished. */
public enum MilestoneKind {
    /** A hero succeeds at an ability or saving-throw roll (an investigation, a risky climb, a trap). */
    CHALLENGE,
    /** The party defeats an enemy (a skirmish with a minor foe, not the final enemy). */
    COMBAT;

    public static MilestoneKind parse(String text) {
        if (text != null && text.trim().toUpperCase(Locale.ROOT).startsWith("COMBAT")) {
            return COMBAT;
        }
        return CHALLENGE;
    }
}
