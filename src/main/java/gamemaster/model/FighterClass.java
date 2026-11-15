package gamemaster.model;

import java.util.List;
import java.util.Map;

public class FighterClass implements CharacterClass {
    @Override public String name() { return "Fighter"; }

    @Override public Map<Stat, Integer> defaultStats() {
        return CharacterClass.stats(15, 12, 14, 8, 9, 10);
    }

    @Override public List<Weapon> attacks() {
        return List.of(
                new Weapon("Slash", 8, Stat.STRENGTH, 0),
                new Weapon("Shield Bash", 4, Stat.STRENGTH, 2),
                new Weapon("Power Strike", 12, Stat.STRENGTH, -3),
                new Weapon("Throwing Axe", 6, Stat.DEXTERITY, -2));
    }
}
