package gamemaster.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Small dark-themed replacements for the two Swing widgets that ignore colour settings. */
final class DarkUi {

    private DarkUi() {}

    static void styleScrollPane(JScrollPane pane) {
        pane.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        pane.getVerticalScrollBar().setOpaque(false);
    }

    static void styleCombo(JComboBox<?> box) {
        box.setUI(new DarkComboBoxUI());
        box.setBorder(GuiTheme.fieldBorderTight());
        box.setBackground(GuiTheme.SURFACE_ALT);
        box.setForeground(GuiTheme.TEXT);
    }

    private static final class DarkScrollBarUI extends BasicScrollBarUI {
        @Override protected JButton createDecreaseButton(int orientation) { return empty(); }
        @Override protected JButton createIncreaseButton(int orientation) { return empty(); }

        private static JButton empty() {
            JButton b = new JButton();
            Dimension none = new Dimension(0, 0);
            b.setPreferredSize(none);
            b.setMinimumSize(none);
            b.setMaximumSize(none);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // transparent track
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? GuiTheme.PURPLE : GuiTheme.BORDER);
            g2.fillRoundRect(r.x + 2, r.y, r.width - 4, r.height, 8, 8);
            g2.dispose();
        }
    }

    private static final class DarkComboBoxUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            BasicArrowButton b = new BasicArrowButton(BasicArrowButton.SOUTH, GuiTheme.SURFACE_ALT,
                    GuiTheme.SURFACE_ALT, GuiTheme.PURPLE_SOFT, GuiTheme.SURFACE_ALT);
            b.setBorder(javax.swing.BorderFactory.createEmptyBorder());
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(GuiTheme.SURFACE_ALT);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    static Color dim(Color c) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 120);
    }
}
