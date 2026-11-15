package gamemaster.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** The window background: a soft green with two slowly drifting auras and a gentle wave along the bottom. */
class BackdropPanel extends JPanel {

    private final long start = System.nanoTime();
    private final javax.swing.Timer ticker = new javax.swing.Timer(40, e -> repaint());

    BackdropPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(GuiTheme.BG);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        ticker.start();
    }

    @Override
    public void removeNotify() {
        ticker.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        double t = (System.nanoTime() - start) / 1e9;
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        glow(g2, w, h, w * (0.85f + 0.05f * (float) Math.sin(t / 5)), -h * 0.05f + h * 0.04f * (float) Math.cos(t / 7),
                Math.max(w, h) * 0.65f, GuiTheme.GLOW_PURPLE);
        glow(g2, w, h, w * (0.05f + 0.05f * (float) Math.cos(t / 6)), h * (1.05f + 0.03f * (float) Math.sin(t / 4)),
                Math.max(w, h) * 0.55f, GuiTheme.GLOW_GREEN);
        wave(g2, w, h, t, 0.86, 14, 0.9, 1.0, GuiTheme.GLOW_GREEN);
        wave(g2, w, h, t, 0.92, 10, -1.3, 1.7, GuiTheme.GLOW_PURPLE);
        g2.dispose();
    }

    /** A slow sine wave filled down to the bottom edge in a faint tint of {@code colour}. */
    private static void wave(Graphics2D g2, int w, int h, double t, double level, double amplitude, double speed,
                             double length, Color colour) {
        java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
        path.moveTo(0, h);
        for (int x = 0; x <= w; x += 8) {
            double y = h * level + amplitude * Math.sin(x / (110.0 * length) + t * speed);
            path.lineTo(x, y);
        }
        path.lineTo(w, h);
        path.closePath();
        g2.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.min(60, colour.getAlpha())));
        g2.fill(path);
    }

    private static void glow(Graphics2D g2, int width, int height, float cx, float cy, float radius, Color colour) {
        Color centre = colour;
        Color edge = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 0);
        g2.setPaint(new RadialGradientPaint(cx, cy, radius, new float[] {0f, 1f}, new Color[] {centre, edge}));
        g2.fillRect(0, 0, width, height);
    }
}
