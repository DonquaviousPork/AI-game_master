package gamemaster.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gamemaster.model.Monster;
import gamemaster.model.Weapon;
import java.util.Map;

/**
 * Reads and range-checks fields of JSON the AI produced. Shared by the scenario
 * generator and the spawn tool so a monster is judged by the same limits everywhere.
 */
final class JsonFields {

    static final int MIN_HEALTH = 1, MAX_HEALTH = 30;
    static final int MIN_AC = 8, MAX_AC = 18;

    static final Map<String, Weapon> WEAPONS = Map.of(
            "longsword", Weapon.LONGSWORD,
            "quarterstaff", Weapon.QUARTERSTAFF,
            "dagger", Weapon.DAGGER);

    private JsonFields() {}

    static String text(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || e.getAsString().isBlank()) {
            throw new IllegalArgumentException("\"" + key + "\" must be a non-empty string.");
        }
        return e.getAsString().trim();
    }

    static int number(JsonObject o, String key, int min, int max) {
        JsonElement e = o.get(key);
        int value;
        try {
            if (e == null || !e.isJsonPrimitive()) {
                throw new NumberFormatException();
            }
            value = Integer.parseInt(e.getAsString().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("\"" + key + "\" must be a whole number.");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException("\"" + key + "\" must be between " + min + " and " + max + ".");
        }
        return value;
    }

    /** Builds a Monster from {name, health, armor_class, weapon}, or throws IllegalArgumentException. */
    static Monster monster(JsonObject m) {
        String weaponName = text(m, "weapon").toLowerCase();
        Weapon weapon = WEAPONS.get(weaponName);
        if (weapon == null) {
            throw new IllegalArgumentException("Unknown weapon \"" + weaponName
                    + "\". Use longsword, quarterstaff or dagger.");
        }
        return new Monster(text(m, "name"),
                number(m, "health", MIN_HEALTH, MAX_HEALTH),
                number(m, "armor_class", MIN_AC, MAX_AC),
                weapon);
    }
}
