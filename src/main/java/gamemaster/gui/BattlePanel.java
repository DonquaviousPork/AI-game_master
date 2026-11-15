package gamemaster.gui;

import gamemaster.rules.AttackOdds;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The optional battle menu: one card per living hero with a menu of that hero's four attacks, a menu of the living
 * enemies, and a strength badge with the hit chance and damage range of the chosen attack on the chosen enemy.
 * "Fight!" sends every choice at once, so a whole round is one click instead of typing an instruction for each hero.
 */
final class BattlePanel extends JPanel {

    private static final String PASS = "(do nothing)";

    private record Row(String hero, JComboBox<String> attack, JComboBox<String> target) {}

    private final Consumer<List<BattlePlan.Pick>> onFight;
    private final StyledButton fight = new StyledButton("Fight!", true);
    private final JPanel rowHolder = new JPanel();
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, String> lastAttack = new HashMap<>();
    private final Map<String, String> lastTarget = new HashMap<>();
    private boolean enabled = true;
    private boolean hasEnemies;

    BattlePanel(Consumer<List<BattlePlan.Pick>> onFight) {
        super(new BorderLayout(12, 0));
        this.onFight = onFight;
        setOpaque(false);
        rowHolder.setOpaque(false);
        rowHolder.setLayout(new BoxLayout(rowHolder, BoxLayout.Y_AXIS));
        fight.addActionListener(e -> fire());
        fight.setPreferredSize(new Dimension(110, 0));
        add(rowHolder, BorderLayout.CENTER);
        add(fight, BorderLayout.EAST);
    }

    /** Rebuilds the rows from the current heroes and enemies, keeping each hero's earlier choices where possible. */
    void refresh(GameViewModel m) {
        remember();
        rowHolder.removeAll();
        rows.clear();
        List<GameViewModel.Card> enemyCards = m.enemies().stream().filter(GameViewModel.Card::alive).toList();
        List<String> enemies = enemyCards.stream().map(GameViewModel.Card::name).toList();
        hasEnemies = !enemies.isEmpty();
        for (GameViewModel.Card hero : m.heroes()) {
            if (!hero.alive()) {
                continue;
            }
            JComboBox<String> attack = new JComboBox<>();
            attack.addItem(PASS);
            hero.attacks().forEach(a -> attack.addItem(a.name()));
            JComboBox<String> target = new JComboBox<>();
            enemies.forEach(target::addItem);
            DarkUi.styleCombo(attack);
            DarkUi.styleCombo(target);
            String first = hero.attacks().isEmpty() ? PASS : hero.attacks().get(0).name();
            attack.setSelectedItem(lastAttack.getOrDefault(hero.name(), first));
            String t = lastTarget.get(hero.name());
            if (t != null) {
                target.setSelectedItem(t);
            }
            rows.add(new Row(hero.name(), attack, target));
            rowHolder.add(heroCard(hero, attack, target, enemyCards));
            rowHolder.add(Box.createVerticalStrut(6));
        }
        applyEnabled();
        revalidate();
        repaint();
    }

    private JComponent heroCard(GameViewModel.Card hero, JComboBox<String> attack, JComboBox<String> target,
                                List<GameViewModel.Card> enemyCards) {
        JPanel card = new RoundedRow();
        card.setLayout(new GridBagLayout());
        card.setBorder(GuiTheme.padding(6, 12, 6, 12));

        JLabel name = new JLabel(hero.name() + " (" + hero.detail().split("  -  ")[0] + ")");
        name.setFont(GuiTheme.serif(java.awt.Font.BOLD, 14f));
        name.setForeground(GuiTheme.TEXT);
        name.setPreferredSize(new Dimension(170, name.getPreferredSize().height));
        RatingBadge badge = new RatingBadge();
        JLabel odds = new JLabel(" ");
        odds.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 12f));
        odds.setForeground(GuiTheme.MUTED);

        Runnable update = () -> {
            Object a = attack.getSelectedItem();
            Object t = target.getSelectedItem();
            GameViewModel.AttackInfo info = hero.attacks().stream()
                    .filter(x -> x.name().equals(a)).findFirst().orElse(null);
            GameViewModel.Card enemy = enemyCards.stream().filter(x -> x.name().equals(t)).findFirst().orElse(null);
            if (info == null || enemy == null) {
                badge.set(null);
                odds.setText(info == null ? "no attack this round" : " ");
                return;
            }
            AttackOdds o = AttackOdds.of(info.damageSides(), info.statModifier(), info.attackBonus(), enemy.armorClass());
            badge.set(o.rating());
            odds.setText("Hit " + o.hitText() + "   Damage " + o.damageText() + "   vs AC " + enemy.armorClass());
        };
        attack.addActionListener(e -> update.run());
        target.addActionListener(e -> update.run());
        update.run();

        card.add(name, c(0, 0));
        card.add(small("uses"), c(1, 0));
        card.add(attack, wide(2, 0));
        card.add(small("on"), c(3, 0));
        card.add(target, wide(4, 0));
        card.add(badge, c(5, 0));
        card.add(odds, c(6, 0));
        return card;
    }

    private static JLabel small(String text) {
        JLabel l = new JLabel(text);
        l.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 12f));
        l.setForeground(GuiTheme.MUTED);
        return l;
    }

    private static GridBagConstraints c(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.insets = new Insets(2, 6, 2, 6);
        g.anchor = GridBagConstraints.WEST;
        return g;
    }

    private static GridBagConstraints wide(int x, int y) {
        GridBagConstraints g = c(x, y);
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
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

    /** A hero's row: a softly rounded card. */
    private static final class RoundedRow extends JPanel {
        RoundedRow() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(GuiTheme.SURFACE_ALT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.setColor(GuiTheme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /** The strength badge: a colour-coded pill saying Weak, Fair, Strong or Very strong. */
    private static final class RatingBadge extends JComponent {
        private AttackOdds.Rating rating;

        RatingBadge() {
            setPreferredSize(new Dimension(104, 24));
        }

        void set(AttackOdds.Rating r) {
            rating = r;
            repaint();
        }

        private static Color colour(AttackOdds.Rating r) {
            return switch (r) {
                case WEAK -> GuiTheme.HEALTH_LOW;
                case FAIR -> GuiTheme.HEALTH_MID;
                case STRONG -> GuiTheme.HEALTH_OK;
                case VERY_STRONG -> GuiTheme.PURPLE;
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (rating == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(colour(rating));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setFont(GuiTheme.sans(java.awt.Font.BOLD, 12f));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            String text = rating.label().toUpperCase();
            g2.setColor(GuiTheme.BG);
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }
}
