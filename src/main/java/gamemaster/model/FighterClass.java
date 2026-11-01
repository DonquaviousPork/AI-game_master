package gamemaster.model;

import java.util.Map;

public class FighterClass implements CharacterClass {
    @Override public String name() { return "Fighter"; }

    @Override public Map<Stat, Integer> defaultStats() {
        return CharacterClass.stats(15, 12, 14, 8, 9, 10);
    }
}
