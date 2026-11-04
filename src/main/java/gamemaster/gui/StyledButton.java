package gamemaster.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;

/** A flat rounded button: purple for the main action, quiet green for the rest. */
final class StyledButton extends JButton {

    private final boolean primary;

    StyledButton(String text, boolean primary) {
        super(text);
        this.primary = primary;
        setFont(GuiTheme.sans(java.awt.Font.BOLD, 13f));
        setForeground(GuiTheme.TEXT);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 36, 38);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color fill = primary ? GuiTheme.PURPLE_DARK : GuiTheme.SURFACE_ALT;
        if (!isEnabled()) {
            fill = GuiTheme.SURFACE;
        } else if (getModel().isPressed()) {
            fill = fill.darker();
        } else if (getModel().isRollover()) {
            fill = primary ? GuiTheme.PURPLE : GuiTheme.BORDER;
        }
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
        g2.setColor(primary && isEnabled() ? GuiTheme.PURPLE : GuiTheme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
        g2.setFont(getFont());
        g2.setColor(isEnabled() ? GuiTheme.TEXT : GuiTheme.MUTED);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
