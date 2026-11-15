package gamemaster.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * The title screen: the animated pixel-art cover with a console-style launcher menu in the lower left
 * (New game, Load game, Settings, Quit) and a demo-mode checkbox. Arrow keys and Enter work as well as the mouse.
 */
final class CoverPanel extends JPanel {

    private final CoverArt art;
    private final boolean dark;
    private final long start = System.nanoTime();
    private final Timer ticker = new Timer(33, e -> repaint());
    private final List<Entry> entries = new ArrayList<>();
    private int selected;

    private final java.util.function.BooleanSupplier demoOn;
    private final java.util.function.Consumer<Boolean> demoSet;

    CoverPanel(boolean dark, Runnable onNew, Runnable onLoad, Runnable onSettings, Runnable onRules, Runnable onQuit,
               java.util.function.BooleanSupplier demoOn, java.util.function.Consumer<Boolean> demoSet) {
        super(new GridBagLayout());
        this.dark = dark;
        this.art = new CoverArt(dark);
        this.demoOn = demoOn;
        this.demoSet = demoSet;
        setOpaque(true);
        setBackground(Color.BLACK);
        setFocusable(true);

        JPanel menu = new MenuBox();
        menu.setLayout(new java.awt.GridLayout(1, 0, 10, 0));
        menu.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12));
        add(menu, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.SOUTH, GridBagConstraints.NONE,
                new Insets(0, 0, 6, 0), 0, 0));
        add(new DemoToggle(), new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH,
                GridBagConstraints.NONE, new Insets(0, 0, 14, 0), 0, 0));
        addEntry(menu, "NEW GAME", onNew);
        addEntry(menu, "LOAD GAME", onLoad);
        addEntry(menu, "SETTINGS", onSettings);
        addEntry(menu, "RULES", onRules);
        addEntry(menu, "QUIT", onQuit);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "down");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "up");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "go");
        getActionMap().put("down", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { move(1); }
        });
        getActionMap().put("up", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { move(-1); }
        });
        getActionMap().put("go", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { entries.get(selected).run.run(); }
        });
    }

    private void addEntry(JPanel menu, String text, Runnable run) {
        Entry entry = new Entry(text, run, entries.size());
        entries.add(entry);
        menu.add(entry);
    }

    private void move(int delta) {
        selected = (selected + delta + entries.size()) % entries.size();
        repaint();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        ticker.start();
        requestFocusInWindow();
    }

    @Override
    public void removeNotify() {
        ticker.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        double t = (System.nanoTime() - start) / 1e9;
        BufferedImage frame = art.frame(t);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        // the picture fills the window width and sits at the top; the strip at the bottom is left for the menu
        int strip = 104;
        double scale = Math.max(getWidth() / (double) CoverArt.W, (getHeight() - strip) / (double) CoverArt.H);
        int w = (int) Math.ceil(CoverArt.W * scale), h = (int) Math.ceil(CoverArt.H * scale);
        g2.drawImage(frame, (getWidth() - w) / 2, 0, w, h, null);
        // the strip behind the menu shows the same animated scene, blurred and dimmed so the lettering stays readable
        int top = getHeight() - strip;
        g2.setClip(0, top, getWidth(), strip);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(blurred(frame), (getWidth() - w) / 2, 0, w, h, null);
        g2.setColor(new Color(dark ? 0x05030A : 0x0B3A1C, false));
        g2.setComposite(AlphaComposite.SrcOver.derive(0.4f));
        g2.fillRect(0, top, getWidth(), strip);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setClip(null);
        g2.setColor(new Color(dark ? 0x4A1D46 : 0x8BE04E));
        g2.fillRect(0, top, getWidth(), 3);
        g2.dispose();
    }

    private BufferedImage small1, small2;

    /** A cheap blur: shrink the picture twice with smoothing; drawing it back up smoothly finishes the job. */
    private BufferedImage blurred(BufferedImage src) {
        if (small1 == null) {
            small1 = new BufferedImage(src.getWidth() / 4, src.getHeight() / 4, BufferedImage.TYPE_INT_RGB);
            small2 = new BufferedImage(src.getWidth() / 8, src.getHeight() / 8, BufferedImage.TYPE_INT_RGB);
        }
        shrink(src, small1);
        shrink(small1, small2);
        return small2;
    }

    private static void shrink(BufferedImage from, BufferedImage to) {
        Graphics2D g = to.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        BufferedImage mid = from;
        // halve in two steps so no pixel is skipped
        int w = from.getWidth(), h = from.getHeight();
        BufferedImage half = new BufferedImage(Math.max(1, w / 2), Math.max(1, h / 2), BufferedImage.TYPE_INT_RGB);
        Graphics2D hg = half.createGraphics();
        hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        hg.drawImage(mid, 0, 0, half.getWidth(), half.getHeight(), null);
        hg.dispose();
        g.drawImage(half, 0, 0, to.getWidth(), to.getHeight(), null);
        g.dispose();
    }

    /** The translucent plate behind the menu. */
    private static final class MenuBox extends JPanel {
        MenuBox() {
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(5 * 170 + 4 * 10 + 24, 44 + 20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(0.72f));
            g2.setColor(new Color(0x0A0614));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(new Color(0x5E3FA8));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.setColor(new Color(0xB79CF2));
            g2.fillRect(0, 0, 10, 2);
            g2.fillRect(0, 0, 2, 10);
            g2.fillRect(getWidth() - 10, getHeight() - 2, 10, 2);
            g2.fillRect(getWidth() - 2, getHeight() - 10, 2, 10);
            g2.dispose();
        }
    }

    /** One menu line: pixel lettering with a selection bar and a blinking arrow. */
    private final class Entry extends JComponent {
        private final String text;
        private final Runnable run;
        private final int index;

        Entry(String text, Runnable run, int index) {
            this.text = text;
            this.run = run;
            this.index = index;
            setPreferredSize(new Dimension(170, 44));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { selected = Entry.this.index; CoverPanel.this.repaint(); }
                @Override public void mouseClicked(MouseEvent e) { Entry.this.run.run(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            boolean on = selected == index;
            int scale = 3;
            int ty = (getHeight() - PixelFont.height(scale)) / 2;
            if (on) {
                g2.setComposite(AlphaComposite.SrcOver.derive(0.30f));
                g2.setColor(new Color(0xFFC857));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setColor(new Color(0xFFC857));
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                boolean blink = (System.nanoTime() / 400_000_000L) % 2 == 0;
                int ax = (getWidth() - PixelFont.width(text, scale)) / 2 - 26 + (blink ? 0 : 4);
                PixelFont.paint(g2, ">", ax, ty, scale, new Color(0xFFE27A));
            }
            int tx = (getWidth() - PixelFont.width(text, scale)) / 2;
            PixelFont.paint(g2, text, tx + 3, ty + 3, scale, new Color(0x0A0614));
            PixelFont.paint(g2, text, tx, ty, scale, on ? new Color(0xFFE27A) : new Color(0xBFB0F0));
            g2.dispose();
        }
    }

    /** A pixel-style checkbox under the menu: demo mode for the next adventure. */
    private final class DemoToggle extends JComponent {
        private static final String TEXT = "DEMO MODE - 5 TURNS, ONE EASY ENEMY";

        DemoToggle() {
            setPreferredSize(new Dimension(PixelFont.width(TEXT, 2) + 44, 30));
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            setToolTipText("Next adventure: 5 turns and one weak enemy, so a victory can be shown quickly");
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    demoSet.accept(!demoOn.getAsBoolean());
                    CoverPanel.this.repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(0.72f));
            g2.setColor(new Color(0x0A0614));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setComposite(AlphaComposite.SrcOver);
            boolean on = demoOn.getAsBoolean();
            g2.setColor(new Color(0x5E3FA8));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            int bx = 12, by = (getHeight() - 14) / 2;
            g2.setColor(new Color(0xB79CF2));
            g2.fillRect(bx, by, 14, 14);
            g2.setColor(new Color(0x0A0614));
            g2.fillRect(bx + 2, by + 2, 10, 10);
            if (on) {
                g2.setColor(new Color(0xFFE27A));
                g2.fillRect(bx + 4, by + 4, 6, 6);
            }
            PixelFont.paint(g2, TEXT, 36, (getHeight() - PixelFont.height(2)) / 2, 2,
                    on ? new Color(0xFFE27A) : new Color(0xBFB0F0));
            g2.dispose();
        }
    }
}
