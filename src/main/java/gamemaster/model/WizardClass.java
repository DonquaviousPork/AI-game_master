package gamemaster.model;

import java.util.Map;

public class WizardClass implements CharacterClass {
    @Override public String name() { return "Wizard"; }

    @Override public Map<Stat, Integer> defaultStats() {
        return CharacterClass.stats(8, 12, 10, 15, 14, 9);
    }
}
