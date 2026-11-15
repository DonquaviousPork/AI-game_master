package gamemaster.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** A number with minus and plus buttons, in place of a spinner. */
final class PixelStepper extends JPanel {

    private int value;
    private final int min, max;
    private final List<ChangeListener> listeners = new ArrayList<>();
    private final JComponent number = new JComponent() {
        @Override public Dimension getPreferredSize() { return new Dimension(PixelFont.width("00", 3) + 12, 40); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(PixelTheme.field());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(PixelTheme.edge());
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            String s = String.valueOf(value);
            PixelUi.text(g2, s, (getWidth() - PixelFont.width(s, 3)) / 2, (getHeight() - PixelFont.height(3)) / 2, 3,
                    value > 0 ? PixelTheme.hilite() : PixelTheme.text());
            g2.dispose();
        }
    };

    PixelStepper(int value, int min, int max) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        this.value = value;
        this.min = min;
        this.max = max;
        setOpaque(false);
        PixelButton minus = new PixelButton("-", false);
        PixelButton plus = new PixelButton("+", false);
        minus.setPreferredSize(new Dimension(40, 40));
        plus.setPreferredSize(new Dimension(40, 40));
        minus.addActionListener(e -> setValue(this.value - 1));
        plus.addActionListener(e -> setValue(this.value + 1));
        add(minus);
        add(number);
        add(plus);
    }

    int getValue() { return value; }

    void addChangeListener(ChangeListener l) { listeners.add(l); }

    void setValue(int v) {
        v = Math.max(min, Math.min(max, v));
        if (v != value) {
            value = v;
            number.repaint();
            for (ChangeListener l : listeners) {
                l.stateChanged(new ChangeEvent(this));
            }
        }
    }
}
