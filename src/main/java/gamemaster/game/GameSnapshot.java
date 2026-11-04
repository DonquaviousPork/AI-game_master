package gamemaster.game;

import gamemaster.model.Character;
import gamemaster.model.CharacterClass;
import gamemaster.model.Monster;
import gamemaster.model.Stat;
import gamemaster.model.Weapon;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Memento: a frozen copy of everything needed to resume a game. It holds plain values only
 * (no live objects), so it can be written to a file and read back. Only GameState creates
 * one from a live game and only GameState can restore from it. The undo history is not saved.
 */
public record GameSnapshot(
        int version,
        String savedAt,
        String scenarioTitle,
        String scenarioDescription,
        String currentEvent,
        boolean concluded,
        List<CharacterData> party,
        List<MonsterData> monsters,
        List<String> log,
        String memorySummary,
        List<String> memoryRecent) {

    public static final int CURRENT_VERSION = 1;

    public GameSnapshot {
        party = party == null ? List.of() : List.copyOf(party);
        monsters = monsters == null ? List.of() : List.copyOf(monsters);
        log = log == null ? List.of() : List.copyOf(log);
        memoryRecent = memoryRecent == null ? List.of() : List.copyOf(memoryRecent);
    }

    /** One hero, stored as values. */
    public record CharacterData(String name, String characterClass, Map<String, Integer> stats, int health) {

        static CharacterData of(Character c) {
            return new CharacterData(c.getName(), c.getCharacterClass().name(), statsOf(c), c.getHealth());
        }

        Character toCharacter() {
            Character c = new Character(name, CharacterClass.byName(characterClass), toStatMap(stats));
            if (health < 0 || health > c.getMaxHealth()) {
                throw new IllegalArgumentException(name + " has impossible health " + health + ".");
            }
            c.applyDamage(c.getMaxHealth() - health);
            return c;
        }
    }

    /** One enemy, stored as values. */
    public record MonsterData(String name, int maxHealth, int health, int armorClass, String weapon,
                              Map<String, Integer> stats) {

        static MonsterData of(Monster m) {
            return new MonsterData(m.getName(), m.getMaxHealth(), m.getHealth(), m.getArmorClass(),
                    m.getWeapon().name(), statsOf(m));
        }

        Monster toMonster() {
            Monster m = new Monster(name, maxHealth, armorClass, Weapon.byName(weapon), toStatMap(stats));
            if (health < 0 || health > maxHealth) {
                throw new IllegalArgumentException(name + " has impossible health " + health + ".");
            }
            m.applyDamage(maxHealth - health);
            return m;
        }
    }

    private static Map<String, Integer> statsOf(gamemaster.model.Combatant c) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Stat s : Stat.values()) {
            out.put(s.name(), c.getStat(s));
        }
        return out;
    }

    private static Map<Stat, Integer> toStatMap(Map<String, Integer> stats) {
        if (stats == null) {
            throw new IllegalArgumentException("Stats are missing.");
        }
        Map<Stat, Integer> out = new EnumMap<>(Stat.class);
        stats.forEach((k, v) -> out.put(Stat.valueOf(k), v));
        return out;
    }
}
