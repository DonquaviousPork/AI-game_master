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
        List<String> memoryRecent,
        String objective,
        int turnsTaken,
        boolean objectiveAchieved,
        String objectiveType,
        String objectiveTarget,
        List<MilestoneData> milestones,
        int milestoneIndex,
        int beatSuccesses,
        int beatKills,
        int beatTurns,
        int beatFailures,
        int healsUsed,
        List<String> statuses) {

    public GameSnapshot(int version, String savedAt, String scenarioTitle, String scenarioDescription,
                        String currentEvent, boolean concluded, List<CharacterData> party,
                        List<MonsterData> monsters, List<String> log, String memorySummary,
                        List<String> memoryRecent, String objective, int turnsTaken, boolean objectiveAchieved,
                        String objectiveType, String objectiveTarget, List<MilestoneData> milestones,
                        int milestoneIndex, int beatSuccesses, int beatKills, int beatTurns, int beatFailures,
                        int healsUsed) {
        this(version, savedAt, scenarioTitle, scenarioDescription, currentEvent, concluded, party, monsters, log,
                memorySummary, memoryRecent, objective, turnsTaken, objectiveAchieved, objectiveType, objectiveTarget,
                milestones, milestoneIndex, beatSuccesses, beatKills, beatTurns, beatFailures, healsUsed, List.of());
    }

    /** Saves written before objectives and turn counting have no such fields; they load with these as defaults. */
    public static final int CURRENT_VERSION = 1;

    public GameSnapshot(int version, String savedAt, String scenarioTitle, String scenarioDescription,
                        String currentEvent, boolean concluded, List<CharacterData> party,
                        List<MonsterData> monsters, List<String> log, String memorySummary,
                        List<String> memoryRecent) {
        this(version, savedAt, scenarioTitle, scenarioDescription, currentEvent, concluded, party, monsters,
                log, memorySummary, memoryRecent, "", 0, false, "NARRATIVE", "");
    }

    public GameSnapshot(int version, String savedAt, String scenarioTitle, String scenarioDescription,
                        String currentEvent, boolean concluded, List<CharacterData> party,
                        List<MonsterData> monsters, List<String> log, String memorySummary,
                        List<String> memoryRecent, String objective, int turnsTaken, boolean objectiveAchieved) {
        this(version, savedAt, scenarioTitle, scenarioDescription, currentEvent, concluded, party, monsters,
                log, memorySummary, memoryRecent, objective, turnsTaken, objectiveAchieved, "NARRATIVE", "");
    }

    public GameSnapshot(int version, String savedAt, String scenarioTitle, String scenarioDescription,
                        String currentEvent, boolean concluded, List<CharacterData> party,
                        List<MonsterData> monsters, List<String> log, String memorySummary,
                        List<String> memoryRecent, String objective, int turnsTaken, boolean objectiveAchieved,
                        String objectiveType, String objectiveTarget) {
        this(version, savedAt, scenarioTitle, scenarioDescription, currentEvent, concluded, party, monsters,
                log, memorySummary, memoryRecent, objective, turnsTaken, objectiveAchieved, objectiveType,
                objectiveTarget, List.of(), 0, 0, 0, 0, 0, 0);
    }

    public GameSnapshot {
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
        milestoneIndex = Math.max(0, milestoneIndex);
        beatSuccesses = Math.max(0, beatSuccesses);
        beatKills = Math.max(0, beatKills);
        beatTurns = Math.max(0, beatTurns);
        beatFailures = Math.max(0, beatFailures);
        healsUsed = Math.max(0, healsUsed);
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
        party = party == null ? List.of() : List.copyOf(party);
        monsters = monsters == null ? List.of() : List.copyOf(monsters);
        log = log == null ? List.of() : List.copyOf(log);
        memoryRecent = memoryRecent == null ? List.of() : List.copyOf(memoryRecent);
        objective = objective == null ? "" : objective;
        turnsTaken = Math.max(0, turnsTaken);
        objectiveType = objectiveType == null || objectiveType.isBlank() ? "NARRATIVE" : objectiveType;
        objectiveTarget = objectiveTarget == null ? "" : objectiveTarget;
    }

    /** One act of the story, stored as values. */
    public record MilestoneData(String title, String description, String kind) {

        static MilestoneData of(Milestone m) {
            return new MilestoneData(m.title(), m.description(), m.kind().name());
        }

        Milestone toMilestone() {
            return new Milestone(title, description, MilestoneKind.parse(kind));
        }
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
