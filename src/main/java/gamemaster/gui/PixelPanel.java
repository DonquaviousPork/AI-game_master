package gamemaster.gui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import javax.swing.JPanel;

/** A framed plate in the title screen's style. Slightly see-through so the scene shows behind it. */
final class PixelPanel extends JPanel {

    private final float alpha;

    PixelPanel(LayoutManager layout) {
        this(layout, 0.84f);
    }

    PixelPanel(LayoutManager layout, float alpha) {
        super(layout);
        this.alpha = alpha;
        setOpaque(false);
        setBorder(GuiTheme.padding(16, 18, 16, 18));
    }

    /** A framed plate never shrinks below its content, so a layout that is short of room cannot clip the text. */
    @Override
    public java.awt.Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        PixelUi.frame(g2, getWidth(), getHeight(), alpha);
        g2.dispose();
    }
}
