package gamemaster.model;

import java.util.EnumMap;
import java.util.Map;

/** A character class (Fighter, Wizard, ...) and its default stat spread. */
public interface CharacterClass {

    String name();

    /** Returns a fresh map of default ability scores for this class. */
    Map<Stat, Integer> defaultStats();

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
