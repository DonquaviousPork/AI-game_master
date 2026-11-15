package gamemaster.game;

import java.util.Locale;

/**
 * How an adventure's objective is decided. The three typed kinds are checked by the game engine from its own
 * state, so the AI cannot award a win. NARRATIVE (the default, and what older saves load as) keeps the older
 * behaviour in which the AI reports whether the objective was achieved.
 */
public enum ObjectiveType {
    NARRATIVE, DEFEAT_TARGET, DEFEAT_ALL, SURVIVE;

    /** Reads a model's or a save file's text; anything unknown is NARRATIVE, never an error. */
    public static ObjectiveType parse(String text) {
        if (text == null) {
            return NARRATIVE;
        }
        String t = text.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (ObjectiveType type : values()) {
            if (type.name().equals(t)) {
                return type;
            }
        }
        return NARRATIVE;
    }

    /** True when the engine decides the result. */
    public boolean engineDecides() {
        return this != NARRATIVE;
    }
}
