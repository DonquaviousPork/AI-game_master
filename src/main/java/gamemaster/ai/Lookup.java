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

    /**
     * Case-insensitive. The model sometimes adds a role or a word to the name ("David the Fighter",
     * "David (Fighter)", "the Emberwolf"), so when there is no exact match a combatant is also accepted when its
     * whole name appears in the text, or when the text is a whole-word part of its name ("Brute"), provided
     * that points to exactly one name. When several share a name (two wolves) the first living one wins.
     */
    static Combatant find(GameState state, String name) {
        Combatant exact = pick(everyone(state), c -> c.getName().equalsIgnoreCase(name.trim()));
        if (exact != null) {
            return exact;
        }
        List<String> words = words(name);
        List<Combatant> containedName = new ArrayList<>();
        for (Combatant c : everyone(state)) {
            if (containsRun(words, words(c.getName()))) {
                containedName.add(c);
            }
        }
        Combatant best = uniqueByName(containedName, true);
        if (best == null) {
            List<Combatant> partOfName = new ArrayList<>();
            for (Combatant c : everyone(state)) {
                if (!words.isEmpty() && containsRun(words(c.getName()), words)) {
                    partOfName.add(c);
                }
            }
            best = uniqueByName(partOfName, false);
        }
        if (best == null) {
            throw new IllegalArgumentException("No one named \"" + name + "\" is in this game. Use one of these exact names: "
                    + names(state));
        }
        return best;
    }

    private static Combatant pick(List<Combatant> all, java.util.function.Predicate<Combatant> test) {
        Combatant firstMatch = null;
        for (Combatant c : all) {
            if (test.test(c)) {
                if (c.isAlive()) {
                    return c;
                }
                if (firstMatch == null) {
                    firstMatch = c;
                }
            }
        }
        return firstMatch;
    }

    /** Returns a combatant when the candidates are all one name (or, if preferLongest, one name is the longest). */
    private static Combatant uniqueByName(List<Combatant> candidates, boolean preferLongest) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (preferLongest) {
            int longest = candidates.stream().mapToInt(c -> words(c.getName()).size()).max().orElse(0);
            candidates = candidates.stream().filter(c -> words(c.getName()).size() == longest).toList();
        }
        String first = candidates.get(0).getName();
        if (candidates.stream().anyMatch(c -> !c.getName().equalsIgnoreCase(first))) {
            return null;
        }
        return pick(candidates, c -> true);
    }

    private static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        for (String w : text.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
            if (!w.isEmpty()) {
                out.add(w);
            }
        }
        return out;
    }

    /** True when {@code inner} appears as consecutive words inside {@code outer}. */
    private static boolean containsRun(List<String> outer, List<String> inner) {
        if (inner.isEmpty() || inner.size() > outer.size()) {
            return false;
        }
        for (int i = 0; i + inner.size() <= outer.size(); i++) {
            if (outer.subList(i, i + inner.size()).equals(inner)) {
                return true;
            }
        }
        return false;
    }

    static boolean isPlayerSide(GameState state, Combatant c) {
        return c instanceof Character && state.getParty().getCharacters().contains(c);
    }

    /**
     * The attack a combatant uses. A monster always uses its own weapon. A hero must use one of the four attacks
     * of its class; with no attack named the class's first attack is used, and an unknown name is an error that
     * lists what the hero can do.
     */
    static Weapon weaponOf(Combatant c, String attackName) {
        if (c instanceof Monster m) {
            return m.getWeapon();
        }
        var cls = ((Character) c).getCharacterClass();
        if (attackName == null || attackName.isBlank()) {
            return cls.attacks().get(0);
        }
        Weapon chosen = cls.attackNamed(attackName);
        if (chosen == null) {
            throw new IllegalArgumentException(c.getName() + " cannot use \"" + attackName + "\". "
                    + c.getName() + " can only use: " + attackList(cls.attacks()) + ".");
        }
        return chosen;
    }

    static String attackList(java.util.List<Weapon> attacks) {
        return attacks.stream().map(w -> w.name() + " (d" + w.damageSides() + ", "
                + (w.attackBonus() >= 0 ? "+" : "") + w.attackBonus() + " to hit)")
                .reduce((a, b) -> a + ", " + b).orElse("nothing");
    }

    private static String names(GameState state) {
        return everyone(state).stream().map(c -> c.getName() + (c.isAlive() ? "" : " (defeated)"))
                .reduce((a, b) -> a + ", " + b).orElse("nobody");
    }
}
