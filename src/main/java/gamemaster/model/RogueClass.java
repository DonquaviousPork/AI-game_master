package gamemaster.model;

import java.util.Map;

public class RogueClass implements CharacterClass {
    @Override public String name() { return "Rogue"; }

    @Override public Map<Stat, Integer> defaultStats() {
        return CharacterClass.stats(10, 15, 12, 12, 9, 10);
    }
}
