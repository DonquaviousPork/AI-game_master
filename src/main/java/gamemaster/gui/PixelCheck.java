package gamemaster.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/** A square check box with a readable label next to it. */
final class PixelCheck extends JComponent {

    private final String label;
    private boolean selected;
    private final List<ActionListener> listeners = new ArrayList<>();

    PixelCheck(String label, boolean selected) {
        this.label = label;
        this.selected = selected;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggle(); }
        });
        getInputMap().put(javax.swing.KeyStroke.getKeyStroke(' '), "toggle");
        getActionMap().put("toggle", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggle(); }
        });
    }

    boolean isSelected() { return selected; }

    void addActionListener(ActionListener l) { listeners.add(l); }

    private void toggle() {
        selected = !selected;
        repaint();
        for (ActionListener l : listeners) {
            l.actionPerformed(new java.awt.event.ActionEvent(this, java.awt.event.ActionEvent.ACTION_PERFORMED, label));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getFontMetrics(GuiTheme.sans(java.awt.Font.PLAIN, 14f)).stringWidth(label) + 40, 26);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int by = (getHeight() - 16) / 2;
        g2.setColor(isFocusOwner() ? PixelTheme.hilite() : PixelTheme.corner());
        g2.fillRect(0, by, 16, 16);
        g2.setColor(PixelTheme.plate());
        g2.fillRect(2, by + 2, 12, 12);
        if (selected) {
            g2.setColor(PixelTheme.hilite());
            g2.fillRect(4, by + 4, 8, 8);
        }
        g2.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
        g2.setColor(selected ? PixelTheme.hilite() : PixelTheme.body());
        g2.drawString(label, 28, (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
        g2.dispose();
    }
}
