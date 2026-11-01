package gamemaster.model;

import java.util.EnumMap;
import java.util.Map;

/** A character class (Fighter, Wizard, ...) and its default stat spread. */
public interface CharacterClass {

    String name();

    /** Returns a fresh map of default ability scores for this class. */
    Map<Stat, Integer> defaultStats();

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
