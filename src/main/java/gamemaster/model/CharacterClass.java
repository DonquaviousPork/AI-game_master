package gamemaster.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A character class (Fighter, Wizard, ...) and its default stat spread. */
public interface CharacterClass {

    String name();

    /** Returns a fresh map of default ability scores for this class. */
    Map<Stat, Integer> defaultStats();

    /** The four attacks a hero of this class can use. Nothing outside this list can be used in combat. */
    List<Weapon> attacks();

    /** Finds one of this class's attacks by name, ignoring case; null when there is no such attack. */
    default Weapon attackNamed(String attackName) {
        if (attackName == null) {
            return null;
        }
        String wanted = attackName.trim().toLowerCase();
        for (Weapon w : attacks()) {
            if (w.name().toLowerCase().equals(wanted)) {
                return w;
            }
        }
        for (Weapon w : attacks()) {
            String n = w.name().toLowerCase();
            if (!wanted.isEmpty() && (n.contains(wanted) || wanted.contains(n))) {
                return w;
            }
        }
        return null;
    }

    /** Finds one of the built-in classes by name, ignoring case. */
    static CharacterClass byName(String name) {
        if (name != null) {
            switch (name.trim().toLowerCase()) {
                case "fighter": return new FighterClass();
                case "wizard": return new WizardClass();
                case "rogue": return new RogueClass();
                default: break;
            }
        }
        throw new IllegalArgumentException("Unknown character class: " + name);
    }

    /** Convenience for building a stat map in Stat declaration order. */
    static Map<Stat, Integer> stats(int str, int dex, int con, int intel, int wis, int cha) {
        Map<Stat, Integer> m = new EnumMap<>(Stat.class);
        m.put(Stat.STRENGTH, str);
        m.put(Stat.DEXTERITY, dex);
        m.put(Stat.CONSTITUTION, con);
        m.put(Stat.INTELLIGENCE, intel);
        m.put(Stat.WISDOM, wis);
        m.put(Stat.CHARISMA, cha);
        return m;
    }
}
