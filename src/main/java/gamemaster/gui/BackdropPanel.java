package gamemaster.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** The window background: a soft green with a faint purple aura at the top right. */
class BackdropPanel extends JPanel {

    BackdropPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(GuiTheme.BG);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        glow(g2, getWidth(), getHeight(), getWidth() * 0.85f, -getHeight() * 0.05f,
                Math.max(getWidth(), getHeight()) * 0.65f, GuiTheme.GLOW_PURPLE);
        glow(g2, getWidth(), getHeight(), getWidth() * 0.05f, getHeight() * 1.05f,
                Math.max(getWidth(), getHeight()) * 0.55f, GuiTheme.GLOW_GREEN);
        g2.dispose();
    }

    private static void glow(Graphics2D g2, int width, int height, float cx, float cy, float radius, Color colour) {
        Color centre = colour;
        Color edge = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 0);
        g2.setPaint(new RadialGradientPaint(cx, cy, radius, new float[] {0f, 1f}, new Color[] {centre, edge}));
        g2.fillRect(0, 0, width, height);
    }
}
