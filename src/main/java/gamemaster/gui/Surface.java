package gamemaster.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** A rounded panel with a soft border, used for the cards on every screen. */
final class Surface extends JPanel {

    Surface(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(GuiTheme.padding(16, 18, 16, 18));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(GuiTheme.SURFACE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
        g2.setColor(GuiTheme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
        g2.dispose();
    }
}
