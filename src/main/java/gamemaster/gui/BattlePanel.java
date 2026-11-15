package gamemaster.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The optional battle menu: one row per living hero with a menu of that hero's four attacks and a menu of the
 * living enemies. "Fight!" sends every choice at once, so a whole round is one click instead of typing an
 * instruction for each hero.
 */
final class BattlePanel extends JPanel {

    private static final String PASS = "(do nothing)";

    private record Row(String hero, JComboBox<String> attack, JComboBox<String> target) {}

    private final Consumer<List<BattlePlan.Pick>> onFight;
    private final StyledButton fight = new StyledButton("Fight!", true);
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, String> lastAttack = new HashMap<>();
    private final Map<String, String> lastTarget = new HashMap<>();
    private boolean enabled = true;
    private boolean hasEnemies;

    BattlePanel(Consumer<List<BattlePlan.Pick>> onFight) {
        super(new GridBagLayout());
        this.onFight = onFight;
        setOpaque(false);
        fight.addActionListener(e -> fire());
    }

    /** Rebuilds the rows from the current heroes and enemies, keeping each hero's earlier choices where possible. */
    void refresh(GameViewModel m) {
        remember();
        removeAll();
        rows.clear();
        List<String> enemies = m.enemies().stream().filter(GameViewModel.Card::alive)
                .map(GameViewModel.Card::name).toList();
        hasEnemies = !enemies.isEmpty();
        int y = 0;
        for (GameViewModel.Card hero : m.heroes()) {
            if (!hero.alive()) {
                continue;
            }
            JLabel name = new JLabel(hero.name() + " (" + hero.detail().split("  -  ")[0] + ")");
            name.setFont(GuiTheme.sans(java.awt.Font.BOLD, 13f));
            name.setForeground(GuiTheme.TEXT);
            JComboBox<String> attack = new JComboBox<>();
            attack.addItem(PASS);
            hero.attacks().forEach(attack::addItem);
            JComboBox<String> target = new JComboBox<>();
            enemies.forEach(target::addItem);
            String a = lastAttack.getOrDefault(hero.name(), hero.attacks().isEmpty() ? PASS : hero.attacks().get(0));
            attack.setSelectedItem(a);
            String t = lastTarget.get(hero.name());
            if (t != null) {
                target.setSelectedItem(t);
            }
            JLabel uses = label("uses");
            JLabel on = label("on");
            add(name, c(0, y, 0));
            add(uses, c(1, y, 0));
            add(attack, c(2, y, 1));
            add(on, c(3, y, 0));
            add(target, c(4, y, 1));
            rows.add(new Row(hero.name(), attack, target));
            y++;
        }
        GridBagConstraints f = c(5, 0, 0);
        f.gridheight = Math.max(1, y);
        f.fill = GridBagConstraints.VERTICAL;
        f.insets = new Insets(2, 14, 2, 0);
        add(fight, f);
        applyEnabled();
        revalidate();
        repaint();
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 12f));
        l.setForeground(GuiTheme.MUTED);
        return l;
    }

    private static GridBagConstraints c(int x, int y, double weight) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.weightx = weight;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(2, 6, 2, 6);
        return g;
    }

    private void remember() {
        for (Row r : rows) {
            if (r.attack().getSelectedItem() != null) {
                lastAttack.put(r.hero(), r.attack().getSelectedItem().toString());
            }
            if (r.target().getSelectedItem() != null) {
                lastTarget.put(r.hero(), r.target().getSelectedItem().toString());
            }
        }
    }

    /** The choices as they stand now; heroes set to "do nothing" are left out. */
    List<BattlePlan.Pick> picks() {
        List<BattlePlan.Pick> out = new ArrayList<>();
        for (Row r : rows) {
            Object a = r.attack().getSelectedItem();
            Object t = r.target().getSelectedItem();
            if (a != null && t != null && !PASS.equals(a)) {
                out.add(new BattlePlan.Pick(r.hero(), a.toString(), t.toString()));
            }
        }
        return out;
    }

    private void fire() {
        List<BattlePlan.Pick> picks = picks();
        if (enabled && !picks.isEmpty()) {
            remember();
            onFight.accept(picks);
        }
    }

    @Override
    public void setEnabled(boolean on) {
        enabled = on;
        super.setEnabled(on);
        applyEnabled();
    }

    private void applyEnabled() {
        for (Row r : rows) {
            r.attack().setEnabled(enabled);
            r.target().setEnabled(enabled);
        }
        fight.setEnabled(enabled && hasEnemies && !rows.isEmpty());
    }
}
