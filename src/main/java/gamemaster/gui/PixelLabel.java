package gamemaster.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.function.Supplier;
import javax.swing.JComponent;

/** Lettering in the 5 by 7 pixel font, with a drop shadow; the colour is read at paint time so mode switches apply. */
final class PixelLabel extends JComponent {

    private String text;
    private final int scale;
    private final Supplier<Color> top, bottom;

    PixelLabel(String text, int scale, Supplier<Color> colour) {
        this(text, scale, colour, colour);
    }

    /** A gradient: the top row of each letter is {@code top}, the bottom row is {@code bottom}. */
    PixelLabel(String text, int scale, Supplier<Color> top, Supplier<Color> bottom) {
        this.text = text;
        this.scale = scale;
        this.top = top;
        this.bottom = bottom;
        setOpaque(false);
    }

    void setText(String text) {
        this.text = text;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        java.awt.Insets in = getInsets();
        return new Dimension(PixelFont.width(text, scale) + scale + in.left + in.right,
                PixelFont.height(scale) + scale + in.top + in.bottom);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(getInsets().left, getInsets().top);
        PixelFont.paint(g2, text, scale, scale, scale, PixelTheme.shadow());
        PixelFont.paintGradient(g2, text, 0, 0, scale, top.get(), bottom.get());
        g2.dispose();
    }
}
