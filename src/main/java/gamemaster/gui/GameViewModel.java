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
    record Card(String name, String detail, int health, int maxHealth, boolean alive) {}

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
                c.getHealth(), c.getMaxHealth(), c.isAlive());
    }

    private static Card enemy(Monster m) {
        return new Card(m.getName(), m.getWeapon().name() + "  -  AC " + m.getArmorClass(),
                m.getHealth(), m.getMaxHealth(), m.isAlive());
    }
}
