package gamemaster.gui;

import java.util.List;

/** Turns the choices made in the battle menu into the instruction sent to the game master. */
final class BattlePlan {

    /** One hero's choice: which attack on which enemy. */
    record Pick(String hero, String attack, String target) {}

    /** The wizard's party heal; it has no target. */
    static final String HEAL = "Healing Light";

    private BattlePlan() {}

    /** Short form shown in the story pane as the player's turn. */
    static String display(List<Pick> picks) {
        StringBuilder sb = new StringBuilder();
        for (Pick p : picks) {
            sb.append(sb.length() == 0 ? "" : "; ").append(p.hero()).append(": ").append(p.attack())
                    .append(HEAL.equals(p.attack()) ? " on the party" : " on " + p.target());
        }
        return sb.toString();
    }

    /** The full instruction for the model; every attack and name is spelled out exactly. */
    static String instruction(List<Pick> picks) {
        StringBuilder sb = new StringBuilder("Battle round. Resolve exactly these attacks, in this order, with one "
                + "attack tool call each, using these exact hero names, attack names and target names: ");
        for (Pick p : picks) {
            if (HEAL.equals(p.attack())) {
                sb.append(p.hero()).append(" casts Healing Light on the whole party (use the heal_party tool, not an attack). ");
            } else {
                sb.append(p.hero()).append(" uses ").append(p.attack()).append(" on ").append(p.target()).append(". ");
            }
        }
        sb.append("Heroes not listed do nothing this round. Then let each living enemy act.");
        return sb.toString();
    }
}
