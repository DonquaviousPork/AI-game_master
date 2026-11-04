package gamemaster.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * Draws one pixel sprite. A defeated character is grey; a living one on low health glows red;
 * everyone else is drawn normally.
 */
final class SpriteIcon extends JComponent {

    static final int SCALE = 3;
    private static final int MARGIN = 7;

    private final String look;
    private final String seed;
    private final boolean alive;
    private final boolean low;

    SpriteIcon(String look, String seed, boolean alive, boolean low) {
        this.look = look;
        this.seed = seed;
        this.alive = alive;
        this.low = alive && low;
        setOpaque(false);
        Dimension size = new Dimension(Sprites.WIDTH * SCALE + 2 * MARGIN, Sprites.HEIGHT * SCALE + 2 * MARGIN);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    boolean isGlowing() { return low; }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        if (low) {
            float radius = Math.max(w, h) * 0.62f;
            g2.setPaint(new RadialGradientPaint(w / 2f, h / 2f, radius, new float[] {0f, 0.55f, 1f},
                    new Color[] {new Color(0xFF3B3B, false).brighter(), new Color(255, 59, 59, 120), new Color(255, 59, 59, 0)}));
            g2.fillRect(0, 0, w, h);
        }
        String[] rows = Sprites.grid(look);
        int x0 = (w - Sprites.WIDTH * SCALE) / 2;
        int y0 = (h - Sprites.HEIGHT * SCALE) / 2;
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                Color c = Sprites.colour(rows[y].charAt(x), look, seed, alive);
                if (c != null) {
                    g2.setColor(c);
                    g2.fillRect(x0 + x * SCALE, y0 + y * SCALE, SCALE, SCALE);
                }
            }
        }
        g2.dispose();
    }
}
