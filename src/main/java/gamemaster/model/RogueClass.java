package gamemaster.model;

import java.util.List;
import java.util.Map;

public class RogueClass implements CharacterClass {
    @Override public String name() { return "Rogue"; }

    @Override public Map<Stat, Integer> defaultStats() {
        return CharacterClass.stats(10, 15, 12, 12, 9, 10);
    }

    @Override public List<Weapon> attacks() {
        return List.of(
                new Weapon("Quick Stab", 4, Stat.DEXTERITY, 2),
                new Weapon("Sneak Attack", 8, Stat.DEXTERITY, -2),
                new Weapon("Dagger Throw", 6, Stat.DEXTERITY, 0),
                new Weapon("Pommel Strike", 4, Stat.STRENGTH, 1));
    }
}
