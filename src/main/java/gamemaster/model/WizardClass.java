package gamemaster.model;

import java.util.List;
import java.util.Map;

public class WizardClass implements CharacterClass {
    @Override public String name() { return "Wizard"; }

    @Override public Map<Stat, Integer> defaultStats() {
        return CharacterClass.stats(8, 12, 10, 15, 14, 9);
    }

    @Override public List<Weapon> attacks() {
        return List.of(
                new Weapon("Magic Missile", 4, Stat.INTELLIGENCE, 2),
                new Weapon("Fire Bolt", 8, Stat.INTELLIGENCE, -1),
                new Weapon("Frost Ray", 6, Stat.INTELLIGENCE, 0),
                new Weapon("Staff Strike", 6, Stat.STRENGTH, 0));
    }
}
