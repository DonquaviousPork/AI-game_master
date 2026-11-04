package gamemaster.ai;

import gamemaster.game.GameState;
import gamemaster.model.Character;
import gamemaster.model.Combatant;
import gamemaster.model.Monster;
import gamemaster.model.Weapon;
import java.util.ArrayList;
import java.util.List;

/** Finds combatants by the name the model used, and picks each one's weapon. */
final class Lookup {

    private Lookup() {}

    static List<Combatant> everyone(GameState state) {
        List<Combatant> all = new ArrayList<>(state.getParty().getCharacters());
        all.addAll(state.getMonsters());
        return all;
    }

    /** Case-insensitive; when several share a name (two wolves) the first living one wins. */
    static Combatant find(GameState state, String name) {
        Combatant firstMatch = null;
        for (Combatant c : everyone(state)) {
            if (c.getName().equalsIgnoreCase(name.trim())) {
                if (c.isAlive()) {
                    return c;
                }
                if (firstMatch == null) {
                    firstMatch = c;
                }
            }
        }
        if (firstMatch == null) {
            throw new IllegalArgumentException("No one named \"" + name + "\" is in this game. Present: "
                    + names(state));
        }
        return firstMatch;
    }

    static boolean isPlayerSide(GameState state, Combatant c) {
        return c instanceof Character && state.getParty().getCharacters().contains(c);
    }

    static Weapon weaponOf(Combatant c) {
        if (c instanceof Monster m) {
            return m.getWeapon();
        }
        String cls = ((Character) c).getCharacterClass().name().toLowerCase();
        if (cls.contains("fighter")) {
            return Weapon.LONGSWORD;
        }
        if (cls.contains("rogue")) {
            return Weapon.DAGGER;
        }
        return Weapon.QUARTERSTAFF;
    }

    private static String names(GameState state) {
        return everyone(state).stream().map(c -> c.getName() + (c.isAlive() ? "" : " (defeated)"))
                .reduce((a, b) -> a + ", " + b).orElse("nobody");
    }
}
