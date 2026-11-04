package gamemaster.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/** A rounded health bar that turns amber and then red as health drops. */
final class HealthBar extends JComponent {

    private final int health;
    private final int max;

    HealthBar(int health, int max) {
        this.health = Math.max(0, health);
        this.max = Math.max(1, max);
        setOpaque(false);
        setPreferredSize(new Dimension(200, 18));
    }

    /** True when the bar is in its red zone (a quarter of maximum health or less). */
    static boolean isLow(int health, int max) {
        return max > 0 && (double) health / max <= 0.25;
    }

    static Color colourFor(int health, int max) {
        double ratio = max <= 0 ? 0 : (double) health / max;
        if (ratio > 0.5) {
            return GuiTheme.HEALTH_OK;
        }
        return ratio > 0.25 ? GuiTheme.HEALTH_MID : GuiTheme.HEALTH_LOW;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(GuiTheme.BG);
        g2.fillRoundRect(0, 0, w, h, h, h);
        int fillWidth = (int) Math.round((w - 2.0) * Math.min(health, max) / max);
        if (fillWidth > 0) {
            g2.setColor(colourFor(health, max));
            g2.fillRoundRect(1, 1, Math.max(fillWidth, h - 2), h - 2, h - 2, h - 2);
        }
        g2.setFont(GuiTheme.sans(java.awt.Font.BOLD, 11f));
        String label = health + "/" + max;
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(health * 2 > max ? GuiTheme.BG : GuiTheme.TEXT);
        g2.drawString(label, (w - fm.stringWidth(label)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
