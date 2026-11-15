package gamemaster.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/** The title screen's animated scene, blurred and dimmed, as the background for the screens that follow it. */
class PixelBackdrop extends JPanel {

    private final CoverArt art = new CoverArt(GuiTheme.mode() == GuiTheme.Mode.DARK);
    private final long start = System.nanoTime();
    private final javax.swing.Timer ticker = new javax.swing.Timer(50, e -> repaint());
    private BufferedImage small;

    PixelBackdrop(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(Color.BLACK);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        ticker.start();
    }

    @Override
    public void removeNotify() {
        ticker.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage frame = art.frame((System.nanoTime() - start) / 1e9);
        if (small == null) {
            small = new BufferedImage(CoverArt.W / 8, CoverArt.H / 8, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D sg = small.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        BufferedImage half = new BufferedImage(CoverArt.W / 2, CoverArt.H / 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D hg = half.createGraphics();
        hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        hg.drawImage(frame, 0, 0, half.getWidth(), half.getHeight(), null);
        hg.dispose();
        sg.drawImage(half, 0, 0, small.getWidth(), small.getHeight(), null);
        sg.dispose();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = Math.max(getWidth() / (double) CoverArt.W, getHeight() / (double) CoverArt.H);
        int w = (int) Math.ceil(CoverArt.W * scale), h = (int) Math.ceil(CoverArt.H * scale);
        g2.drawImage(small, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
        g2.setComposite(AlphaComposite.SrcOver.derive(0.55f));
        g2.setColor(PixelTheme.plate());
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
