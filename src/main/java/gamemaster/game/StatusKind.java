package gamemaster.game;

import java.util.Locale;
import java.util.Optional;

/** A short-lived effect on a hero or enemy. The engine applies and counts these; the AI only asks for them. */
public enum StatusKind {
    BURN("Burn", "burning"),
    CHILL("Chill", "chilled"),
    WEAKEN("Weaken", "weakened");

    /** Every effect lasts this many player turns. */
    public static final int DURATION = 2;
    public static final int BURN_DAMAGE = 2;
    public static final int CHILL_PENALTY = 3;
    public static final int WEAKEN_AC = 2;

    private final String label;
    private final String adjective;

    StatusKind(String label, String adjective) {
        this.label = label;
        this.adjective = adjective;
    }

    public String label() { return label; }
    public String adjective() { return adjective; }

    /** What the effect does, in a few words (for the log and the prompt). */
    public String effectText() {
        return switch (this) {
            case BURN -> BURN_DAMAGE + " damage at the start of each turn";
            case CHILL -> "-" + CHILL_PENALTY + " to attack rolls";
            case WEAKEN -> "-" + WEAKEN_AC + " armor class";
        };
    }

    public static Optional<StatusKind> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String t = text.trim().toLowerCase(Locale.ROOT);
        for (StatusKind k : values()) {
            if (k.name().toLowerCase(Locale.ROOT).equals(t) || k.adjective.equals(t)) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }

    /** The effect a hero's attack applies on a hit: Wizards burn (Fire Bolt) and chill (Frost Ray), Rogues weaken (Sneak Attack). */
    public static Optional<StatusKind> forHeroAttack(String className, String weaponName) {
        if (className == null || weaponName == null) {
            return Optional.empty();
        }
        String c = className.trim().toLowerCase(Locale.ROOT);
        String w = weaponName.trim().toLowerCase(Locale.ROOT);
        if (c.equals("wizard") && w.equals("fire bolt")) {
            return Optional.of(BURN);
        }
        if (c.equals("wizard") && w.equals("frost ray")) {
            return Optional.of(CHILL);
        }
        if (c.equals("rogue") && w.equals("sneak attack")) {
            return Optional.of(WEAKEN);
        }
        return Optional.empty();
    }
}
