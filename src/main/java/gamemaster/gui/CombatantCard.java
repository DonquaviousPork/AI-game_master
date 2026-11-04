package gamemaster.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A rounded card for one hero or enemy: name, class or weapon, and a health bar. */
final class CombatantCard extends JPanel {

    private final Color accent;
    private final boolean alive;

    CombatantCard(GameViewModel.Card card, Color accent) {
        super(new BorderLayout(0, 6));
        this.accent = accent;
        this.alive = card.alive();
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
        setBorder(GuiTheme.padding(10, 14, 12, 14));

        JLabel name = new JLabel(card.alive() ? card.name() : card.name() + "  (defeated)");
        name.setFont(GuiTheme.serif(java.awt.Font.BOLD, 16f));
        name.setForeground(card.alive() ? GuiTheme.TEXT : GuiTheme.MUTED);
        JLabel detail = new JLabel(card.detail());
        detail.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 12f));
        detail.setForeground(GuiTheme.MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.NORTH);
        top.add(detail, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(new HealthBar(card.health(), card.maxHealth()), BorderLayout.SOUTH);
        setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder()));
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(alive ? GuiTheme.SURFACE_ALT : GuiTheme.SURFACE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g2.setColor(alive ? accent : GuiTheme.BORDER);
        g2.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);
        g2.setColor(GuiTheme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g2.dispose();
    }
}
