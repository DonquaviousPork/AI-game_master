package gamemaster.gui;

import gamemaster.game.GameState;
import gamemaster.model.Character;
import gamemaster.model.Monster;
import java.util.List;

/**
 * An immutable copy of what the window shows. It is built on the thread that changed the game
 * and then handed to the screen thread, so painting never reads a game that is mid-change.
 */
record GameViewModel(String title, String phase, String scene, List<Card> heroes, List<Card> enemies, boolean over,
                     String objective, int turn, int turnLimit, String ending, boolean victory,
                     List<String> log, int healsLeft) {

    /** One hero or enemy as shown on a card. */
    record Card(String name, String detail, int health, int maxHealth, boolean alive, String look, String tooltip,
                 List<AttackInfo> attacks, String details, int armorClass, List<Effect> effects) {

        /** The old shape, for a card with no effects on it. */
        Card(String name, String detail, int health, int maxHealth, boolean alive, String look, String tooltip,
             List<AttackInfo> attacks, String details, int armorClass) {
            this(name, detail, health, maxHealth, alive, look, tooltip, attacks, details, armorClass, List.of());
        }
    }

    /** One status effect on a card and the turns it has left. */
    record Effect(gamemaster.game.StatusKind kind, int turns) {}

    /** What the battle menu needs to work out the odds of one attack. */
    record AttackInfo(String name, int damageSides, int statModifier, int attackBonus) {}

    static GameViewModel from(GameState s) {
        List<Card> heroes = s.getParty().getCharacters().stream().map(c -> hero(s, c)).toList();
        List<Card> enemies = s.getMonsters().stream().map(m -> enemy(s, m))
                .sorted(java.util.Comparator.comparing((Card c) -> !c.alive())).toList();   // the fallen go to the bottom
        boolean over = s.isConcluded() || s.getParty().isDefeated();
        return new GameViewModel(
                s.getScenario() == null ? "" : s.getScenario().title(),
                s.getPhase().name(),
                s.getCurrentEvent() == null ? "" : s.getCurrentEvent().description(),
                heroes, enemies, over,
                s.getScenario() == null ? "" : s.getScenario().objective(),
                s.getTurnsTaken(), s.getTurnLimit(), endingText(s, over), over && s.isObjectiveAchieved(),
                List.copyOf(s.getLog()), s.healsLeft());
    }

    /** The effects on one combatant, in a fixed order. */
    static List<Effect> effects(GameState s, String name) {
        List<Effect> out = new java.util.ArrayList<>();
        s.statusesOf(name).forEach((k, t) -> out.add(new Effect(k, t)));
        return out;
    }

    /** How the story ended, for the story pane; empty while it is still going. */
    static String endingText(GameState s, boolean over) {
        return over ? s.endingText() : "";
    }

    private static Card hero(GameState s, Character c) {
        return new Card(c.getName(), c.getCharacterClass().name() + "  -  AC " + c.getArmorClass(),
                c.getHealth(), c.getMaxHealth(), c.isAlive(),
                Sprites.lookForHero(c.getCharacterClass().name()), attacksText(c),
                c.getCharacterClass().attacks().stream()
                        .map(w -> new AttackInfo(w.name(), w.damageSides(), c.getModifier(w.stat()), w.attackBonus()))
                        .toList(),
                detailsText(c, c.getCharacterClass().name(), c.getCharacterClass().attacks()), c.getArmorClass(), effects(s, c.getName()));
    }

    private static Card enemy(GameState s, Monster m) {
        return new Card(m.getName(), m.getWeapon().name() + "  -  AC " + m.getArmorClass(),
                m.getHealth(), m.getMaxHealth(), m.isAlive(),
                Sprites.lookForEnemy(m.getName(), m.getWeapon().name()), "", List.of(),
                detailsText(m, "Enemy", List.of(m.getWeapon())), m.getArmorClass(), effects(s, m.getName()));
    }

    /** Everything shown when a card is clicked: abilities with modifiers, health, armour class and attacks. */
    static String detailsText(gamemaster.model.Combatant c, String kind, List<gamemaster.model.Weapon> weapons) {
        StringBuilder sb = new StringBuilder("<html><body style='width:260px'><b style='font-size:14px'>")
                .append(c.getName()).append("</b> (").append(kind).append(")<br>")
                .append("HP ").append(c.getHealth()).append(" / ").append(c.getMaxHealth())
                .append(" &nbsp; AC ").append(c.getArmorClass()).append("<br><br>");
        for (gamemaster.model.Stat s : gamemaster.model.Stat.values()) {
            int mod = c.getModifier(s);
            sb.append(s.name().charAt(0)).append(s.name().substring(1).toLowerCase()).append(": ")
                    .append(c.getStat(s)).append(" (").append(mod >= 0 ? "+" : "").append(mod).append(")<br>");
        }
        sb.append("<br><b>Attacks</b>");
        for (var w : weapons) {
            sb.append("<br>").append(w.name()).append(" - d").append(w.damageSides()).append(", ")
                    .append(w.attackBonus() >= 0 ? "+" : "").append(w.attackBonus()).append(" to hit");
        }
        return sb.append("</body></html>").toString();
    }

    /** One line per attack the hero can use, for the card's tooltip. */
    static String attacksText(Character c) {
        StringBuilder sb = new StringBuilder("<html><b>" + c.getName() + " can use:</b>");
        for (var w : c.getCharacterClass().attacks()) {
            sb.append("<br>").append(w.name()).append(" - d").append(w.damageSides())
                    .append(", ").append(w.attackBonus() >= 0 ? "+" : "").append(w.attackBonus()).append(" to hit");
        }
        return sb.append("</html>").toString();
    }

    /** The same list as plain text, for the story pane. */
    static String attacksLine(Character c) {
        StringBuilder sb = new StringBuilder(c.getName() + " (" + c.getCharacterClass().name() + ") attacks: ");
        boolean first = true;
        for (var w : c.getCharacterClass().attacks()) {
            sb.append(first ? "" : "; ").append(w.name()).append(" d").append(w.damageSides()).append(' ')
                    .append(w.attackBonus() >= 0 ? "+" : "").append(w.attackBonus());
            first = false;
        }
        return sb.toString();
    }
}
