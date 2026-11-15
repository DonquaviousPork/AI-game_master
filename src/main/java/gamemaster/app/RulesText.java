package gamemaster.app;

import java.util.List;

/** The player-facing rules of the game, written once and shown in both the window and the command line. */
public final class RulesText {

    private RulesText() {}

    /** One titled block of the rules. */
    public record Section(String title, List<String> paragraphs) {}

    public static List<Section> sections() {
        return List.of(
                new Section("THE GOAL", List.of(
                        "Every adventure has one objective and a turn limit: 20 turns, or 5 in demo mode. You win by "
                                + "reaching the objective in time. You lose if every hero falls or the turns run out first.")),
                new Section("HOW A TURN WORKS", List.of(
                        "You type what your party does, or use the battle menu in a fight. The game master (the AI) "
                                + "describes the scene and the result.",
                        "The AI never rolls dice or decides outcomes. When something is uncertain it asks the game's "
                                + "rules engine, which rolls the dice and applies the numbers. What the log shows as [rules] "
                                + "is exactly what the engine did.")),
                new Section("THE STORY ACTS", List.of(
                        "Most adventures have three acts, shown beside the objective. Act 1 is an investigation: a hero "
                                + "must succeed at a roll (or the party pushes through after three failures). Act 2 is a "
                                + "complication: the party must defeat a minor enemy. Act 3 is the showdown with the boss.",
                        "The engine tracks the acts. The boss cannot appear before the last one, and defeating it "
                                + "wins the adventure by itself.")),
                new Section("ABILITY CHECKS", List.of(
                        "A check is a d20 plus the hero's stat modifier, against a difficulty from 5 (easy) to 20 "
                                + "(very hard), usually 12. Meet or beat it to succeed.",
                        "A stat's modifier is (score - 10) divided by 2, rounded down. The six stats are Strength, "
                                + "Dexterity, Constitution, Intelligence, Wisdom and Charisma.",
                        "A failed check still moves the story on, and after two failures in a row the next check is "
                                + "3 easier.")),
                new Section("SAVING THROWS", List.of(
                        "A saving throw is the same roll, used to resist a trap, a spell or another hazard. If a hero "
                                + "fails it, they take damage.")),
                new Section("COMBAT", List.of(
                        "A fight starts when an enemy appears. Each round you choose an attack for each hero, then every "
                                + "living enemy attacks once.",
                        "To attack, roll a d20 plus the stat modifier and the attack's bonus. If the total meets or beats "
                                + "the target's armor class, it hits. A hero's armor class is 10 plus their Dexterity modifier.",
                        "A natural 20 always hits and is a critical hit: the damage dice are rolled twice. A natural 1 "
                                + "always misses. Damage is the attack's die plus the stat modifier, never below 1.",
                        "A hero's health is 10 plus their Constitution score. At 0 health a hero is defeated. The "
                                + "battle menu shows each attack's hit chance, damage range and a Weak to Very strong rating.",
                        "The game limits how many enemies and how much total enemy health the party can face at once, "
                                + "based on the size and health of the party. The final boss is exempt.")),
                new Section("STATUS EFFECTS", List.of(
                        "Some attacks leave an effect on a hit that lasts 2 turns. Burn deals 2 damage at the start of "
                                + "each turn. Chill gives the target -3 to its attack rolls. Weaken lowers its armor class by 2.",
                        "Wizards cause two: Fire Bolt burns and Frost Ray chills. Rogues cause one: Sneak Attack weakens. "
                                + "Fighters cause none. Some enemies can burn, chill or weaken a hero the same way, at most "
                                + "once per enemy per turn. A new hit refreshes an effect; it does not stack. Effects end "
                                + "when the fight does. Cards show them, for example Burn 2.")),
                new Section("HEALING", List.of(
                        "Wizards can cast Healing Light, twice per adventure. It heals every living hero for a d6 plus "
                                + "the wizard's Wisdom modifier (at least 1), up to their maximum health. It cannot bring "
                                + "back a fallen hero. Fighters and rogues cannot heal.")),
                new Section("UNDO, SAVE AND LOAD", List.of(
                        "Undo reverses the last action's damage or healing; dice are not rolled again. You can save and "
                                + "load at any time, and the game warns you before you leave with unsaved progress.")),
                new Section("DEMO MODE", List.of(
                        "Demo mode gives you 5 turns and one easy enemy with no acts, so a victory can be shown quickly.")));
    }

    /** The rules as plain text for the command line, wrapped to {@code width} columns. */
    public static String plain(int width) {
        StringBuilder sb = new StringBuilder();
        for (Section s : sections()) {
            sb.append(s.title()).append('\n');
            for (String p : s.paragraphs()) {
                sb.append(wrap(p, width)).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private static String wrap(String text, int width) {
        StringBuilder out = new StringBuilder();
        int col = 0;
        for (String word : text.split(" ")) {
            if (col > 0 && col + 1 + word.length() > width) {
                out.append('\n');
                col = 0;
            } else if (col > 0) {
                out.append(' ');
                col++;
            }
            out.append(word);
            col += word.length();
        }
        return out.toString();
    }
}
