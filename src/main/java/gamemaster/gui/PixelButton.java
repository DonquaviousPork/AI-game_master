package gamemaster.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;

/**
 * A button drawn like the title menu: pixel lettering, a gold highlight bar when hovered or focused, a gold
 * outline when it is the main action. Labels are shown in capitals (the pixel font has no lower case).
 */
final class PixelButton extends JButton {

    private final boolean primary;
    private boolean chosen;

    PixelButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** For a row of choices: the chosen one is drawn lit. */
    void setChosen(boolean chosen) {
        this.chosen = chosen;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PixelFont.width(getText(), 2) + 40, 40);
    }

    @Override
    public Dimension getMinimumSize() { return getPreferredSize(); }

    @Override
    public Dimension getMaximumSize() { return getPreferredSize(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth(), h = getHeight();
        boolean on = isEnabled() && (getModel().isRollover() || getModel().isArmed() || isFocusOwner());
        boolean lit = isEnabled() && (on || chosen);
        g2.setColor(PixelTheme.field());
        g2.fillRect(0, 0, w, h);
        if (lit) {
            g2.setComposite(AlphaComposite.SrcOver.derive(0.30f));
            g2.setColor(PixelTheme.hiliteFill());
            g2.fillRect(0, 0, w, h);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(PixelTheme.hiliteFill());
            g2.fillRect(0, h - 3, w, 3);
        }
        Color edge = !isEnabled() ? PixelTheme.edge().darker() : primary || chosen ? PixelTheme.hilite() : PixelTheme.edge();
        g2.setColor(edge);
        g2.drawRect(0, 0, w - 1, h - 1);
        if (primary && isEnabled()) {
            g2.drawRect(1, 1, w - 3, h - 3);
        }
        Color colour = !isEnabled() ? PixelTheme.muted().darker() : lit || primary ? PixelTheme.hilite() : PixelTheme.text();
        int tw = PixelFont.width(getText(), 2);
        int tx = (w - tw) / 2, ty = (h - PixelFont.height(2)) / 2 - 1;
        PixelFont.paint(g2, getText(), tx + 2, ty + 2, 2, PixelTheme.shadow());
        PixelFont.paint(g2, getText(), tx, ty, 2, colour);
        g2.dispose();
    }
}
