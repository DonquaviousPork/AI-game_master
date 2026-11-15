package gamemaster.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicScrollBarUI;

/** Drawing helpers and small style functions for the pixel look. */
final class PixelUi {

    private PixelUi() {}

    /** A plate with a purple (or green) edge, a faint inner line and bright corner brackets, like the title menu. */
    static void frame(Graphics2D g2, int w, int h, float alpha) {
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2.setColor(PixelTheme.plate());
        g2.fillRect(0, 0, w, h);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(PixelTheme.edge());
        g2.drawRect(0, 0, w - 1, h - 1);
        g2.setComposite(AlphaComposite.SrcOver.derive(0.35f));
        g2.drawRect(3, 3, w - 7, h - 7);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(PixelTheme.corner());
        int a = Math.min(12, Math.max(4, Math.min(w, h) / 4));
        g2.fillRect(0, 0, a, 2);
        g2.fillRect(0, 0, 2, a);
        g2.fillRect(w - a, h - 2, a, 2);
        g2.fillRect(w - 2, h - a, 2, a);
    }

    /** Pixel lettering with a one-block drop shadow. */
    static void text(Graphics2D g2, String text, int x, int y, int scale, Color colour) {
        PixelFont.paint(g2, text, x + scale, y + scale, scale, PixelTheme.shadow());
        PixelFont.paint(g2, text, x, y, scale, colour);
    }

    /** A square-cornered border with a 2 pixel edge, used for text fields and lists. */
    static Border fieldBorder() {
        return new Border() {
            @Override public void paintBorder(java.awt.Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(c.hasFocus() ? PixelTheme.hilite() : PixelTheme.edge());
                g.drawRect(x, y, w - 1, h - 1);
                g.drawRect(x + 1, y + 1, w - 3, h - 3);
            }
            @Override public Insets getBorderInsets(java.awt.Component c) { return new Insets(8, 12, 8, 12); }
            @Override public boolean isBorderOpaque() { return true; }
        };
    }

    static void styleField(JTextField f) {
        f.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
        f.setOpaque(true);
        f.setBackground(PixelTheme.field());
        f.setForeground(PixelTheme.body());
        f.setCaretColor(PixelTheme.hilite());
        f.setSelectionColor(PixelTheme.edge());
        f.setSelectedTextColor(Color.WHITE);
        f.setBorder(fieldBorder());
        f.setPreferredSize(new Dimension(200, 38));
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { f.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e) { f.repaint(); }
        });
    }

    static void styleCombo(JComboBox<?> box) {
        box.setUI(new PixelComboUI());
        box.setBackground(PixelTheme.field());
        box.setForeground(PixelTheme.body());
        box.setBorder(new Border() {
            @Override public void paintBorder(java.awt.Component c, Graphics g, int x, int y, int w, int h) {
                g.setColor(PixelTheme.edge());
                g.drawRect(x, y, w - 1, h - 1);
                g.drawRect(x + 1, y + 1, w - 3, h - 3);
            }
            @Override public Insets getBorderInsets(java.awt.Component c) { return new Insets(4, 10, 4, 4); }
            @Override public boolean isBorderOpaque() { return true; }
        });
        box.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                javax.swing.JLabel l = (javax.swing.JLabel) super.getListCellRendererComponent(list, value, index,
                        selected, focus);
                l.setOpaque(true);
                l.setBackground(selected ? PixelTheme.edge() : PixelTheme.field());
                l.setForeground(PixelTheme.body());
                l.setBorder(GuiTheme.padding(4, 8, 4, 8));
                return l;
            }
        });
    }

    /** A square scroll thumb in the edge colour. */
    static void styleScroll(JScrollPane pane) {
        pane.setBorder(null);
        pane.getViewport().setOpaque(false);
        pane.setOpaque(false);
        pane.getVerticalScrollBar().setOpaque(false);
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        pane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected JButton createDecreaseButton(int o) { return none(); }
            @Override protected JButton createIncreaseButton(int o) { return none(); }
            private JButton none() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty()) {
                    return;
                }
                g.setColor(isThumbRollover() ? PixelTheme.corner() : PixelTheme.edge());
                g.fillRect(r.x + 2, r.y, r.width - 4, r.height);
            }
        });
    }

    private static final class PixelComboUI extends javax.swing.plaf.basic.BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(PixelTheme.field());
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(PixelTheme.hilite());
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    for (int i = 0; i < 4; i++) {
                        g2.fillRect(cx - 6 + i * 2, cy - 3 + i * 2 - 1, 14 - i * 4, 2);
                    }
                    g2.dispose();
                }
            };
            b.setBorder(javax.swing.BorderFactory.createEmptyBorder());
            b.setContentAreaFilled(false);
            b.setFocusable(false);
            return b;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(PixelTheme.field());
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected javax.swing.plaf.basic.ComboPopup createPopup() {
            javax.swing.plaf.basic.BasicComboPopup p = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
            p.setBorder(javax.swing.BorderFactory.createLineBorder(PixelTheme.edge(), 2));
            p.getList().setBackground(PixelTheme.field());
            return p;
        }
    }
}
