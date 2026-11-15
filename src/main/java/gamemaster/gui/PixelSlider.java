package gamemaster.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** A 0 to 100 slider drawn as a row of blocks with a square handle. */
final class PixelSlider extends JComponent {

    private static final int BLOCKS = 20;
    private int value;
    private final List<ChangeListener> listeners = new ArrayList<>();

    PixelSlider(int value) {
        this.value = Math.max(0, Math.min(100, value));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusable(true);
        MouseAdapter m = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); set(e.getX()); }
            @Override public void mouseDragged(MouseEvent e) { set(e.getX()); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "up");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "down");
        getActionMap().put("up", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { setValue(value + 5); }
        });
        getActionMap().put("down", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { setValue(value - 5); }
        });
    }

    int getValue() { return value; }

    void addChangeListener(ChangeListener l) { listeners.add(l); }

    void setValue(int v) {
        v = Math.max(0, Math.min(100, v));
        if (v != value) {
            value = v;
            repaint();
            for (ChangeListener l : listeners) {
                l.stateChanged(new ChangeEvent(this));
            }
        }
    }

    private void set(int x) {
        setValue((int) Math.round(100.0 * (x - 8) / Math.max(1, getWidth() - 16)));
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(260, 30); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth() - 16, h = getHeight();
        int bw = w / BLOCKS;
        int lit = (int) Math.round(value / 100.0 * BLOCKS);
        for (int i = 0; i < BLOCKS; i++) {
            g2.setColor(i < lit ? PixelTheme.hilite() : PixelTheme.field());
            g2.fillRect(8 + i * bw, h / 2 - 6, bw - 2, 12);
            if (i >= lit) {
                g2.setColor(PixelTheme.edge());
                g2.drawRect(8 + i * bw, h / 2 - 6, bw - 3, 11);
            }
        }
        if (isFocusOwner()) {
            g2.setColor(PixelTheme.hilite());
            g2.drawRect(2, h / 2 - 11, getWidth() - 5, 21);
        }
        g2.dispose();
    }
}
