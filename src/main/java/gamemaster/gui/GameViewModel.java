package gamemaster.gui;

import gamemaster.game.GameState;
import gamemaster.model.Character;
import gamemaster.model.Monster;
import java.util.List;

/**
 * An immutable copy of what the window shows. It is built on the thread that changed the game
 * and then handed to the screen thread, so painting never reads a game that is mid-change.
 */
record GameViewModel(String title, String phase, String scene, List<Card> heroes, List<Card> enemies, boolean over) {

    /** One hero or enemy as shown on a card. */
    record Card(String name, String detail, int health, int maxHealth, boolean alive, String look, String tooltip,
                 List<String> attacks) {}

    static GameViewModel from(GameState s) {
        List<Card> heroes = s.getParty().getCharacters().stream().map(GameViewModel::hero).toList();
        List<Card> enemies = s.getMonsters().stream().map(GameViewModel::enemy).toList();
        boolean over = s.isConcluded() || s.getParty().isDefeated();
        return new GameViewModel(
                s.getScenario() == null ? "" : s.getScenario().title(),
                s.getPhase().name(),
                s.getCurrentEvent() == null ? "" : s.getCurrentEvent().description(),
                heroes, enemies, over);
    }

    private static Card hero(Character c) {
        return new Card(c.getName(), c.getCharacterClass().name() + "  -  AC " + c.getArmorClass(),
                c.getHealth(), c.getMaxHealth(), c.isAlive(),
                Sprites.lookForHero(c.getCharacterClass().name()), attacksText(c),
                c.getCharacterClass().attacks().stream().map(gamemaster.model.Weapon::name).toList());
    }

    private static Card enemy(Monster m) {
        return new Card(m.getName(), m.getWeapon().name() + "  -  AC " + m.getArmorClass(),
                m.getHealth(), m.getMaxHealth(), m.isAlive(),
                Sprites.lookForEnemy(m.getName(), m.getWeapon().name()), "", List.of());
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
