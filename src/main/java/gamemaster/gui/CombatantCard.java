package gamemaster.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A rounded card for one hero or enemy: name, class or weapon, and a health bar. */
final class CombatantCard extends JPanel {

    private final Color accent;
    private final boolean alive;
    private final CardReaction reaction;
    private final long reactionStart;
    private javax.swing.Timer timer;

    CombatantCard(GameViewModel.Card card, Color accent) {
        this(card, accent, null, 0);
    }

    /** {@code reactionStart} is a System.nanoTime() value, so a rebuilt card carries on the same animation. */
    CombatantCard(GameViewModel.Card card, Color accent, CardReaction reaction, long reactionStart) {
        super(new BorderLayout(8, 0));
        this.accent = accent;
        this.reaction = reaction;
        this.reactionStart = reactionStart;
        this.alive = card.alive();
        setOpaque(false);
        if (!card.tooltip().isEmpty()) {
            setToolTipText(card.tooltip());
        }
        setAlignmentX(LEFT_ALIGNMENT);
        setBorder(GuiTheme.padding(10, 14, 12, 14));
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        String title = card.name();
        String details = card.details();
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                JLabel body = new JLabel(details);
                body.setForeground(GuiTheme.TEXT);
                javax.swing.JOptionPane.showMessageDialog(CombatantCard.this, body, title,
                        javax.swing.JOptionPane.PLAIN_MESSAGE);
            }
        });

        JLabel name = new JLabel(card.alive() ? card.name() : card.name() + "  (defeated)");
        name.setFont(GuiTheme.serif(java.awt.Font.BOLD, 16f));
        name.setForeground(card.alive() ? GuiTheme.TEXT : GuiTheme.MUTED);
        JLabel detail = new JLabel(card.detail());
        detail.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 12f));
        detail.setForeground(GuiTheme.MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.NORTH);
        top.add(detail, BorderLayout.SOUTH);
        JPanel text = new JPanel(new BorderLayout(0, 6));
        text.setOpaque(false);
        text.add(top, BorderLayout.NORTH);
        text.add(new HealthBar(card.health(), card.maxHealth()), BorderLayout.SOUTH);
        SpriteIcon sprite = new SpriteIcon(card.look(), card.name(), card.alive(),
                HealthBar.isLow(card.health(), card.maxHealth()));
        JPanel spriteBox = new JPanel(new java.awt.GridBagLayout());
        spriteBox.setOpaque(false);
        spriteBox.add(sprite);
        add(spriteBox, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        setBorder(BorderFactory.createCompoundBorder(getBorder(), BorderFactory.createEmptyBorder()));
    }

    private double progress() {
        if (reaction == null) {
            return 1;
        }
        return Math.min(1.0, (System.nanoTime() - reactionStart) / 1e6 / CardReaction.DURATION_MS);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (reaction != null && progress() < 1.0) {
            timer = new javax.swing.Timer(30, e -> {
                repaint();
                if (progress() >= 1.0) {
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            });
            timer.start();
        }
    }

    @Override
    public void removeNotify() {
        if (timer != null) {
            timer.stop();
        }
        super.removeNotify();
    }

    @Override
    public void paint(Graphics g) {
        double t = progress();
        if (reaction == null || t >= 1.0) {
            super.paint(g);
            return;
        }
        int dx = 0;
        int dy = 0;
        double quick = Math.min(1.0, t / 0.35);
        switch (reaction.kind()) {
            case HIT, DEFEAT -> dx = (int) Math.round(Math.sin(t * 38) * 7 * (1 - quick));
            case CRIT -> dx = (int) Math.round(Math.sin(t * 46) * 10 * (1 - quick));
            case MISS -> dx = (int) Math.round(Math.sin(Math.min(1.0, t / 0.4) * Math.PI) * 14);
            case CHEER -> dy = -(int) Math.round(Math.abs(Math.sin(t * Math.PI * 3)) * 7 * (1 - t));
            default -> { }
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(dx, dy);
        super.paint(g2);
        g2.dispose();
        paintEffects((Graphics2D) g.create(), t);
    }

    /** Flash, floating number and speech bubble, drawn over the card. */
    private void paintEffects(Graphics2D g2, double t) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color flash = switch (reaction.kind()) {
            case HIT, DEFEAT -> new Color(255, 59, 59);
            case CRIT -> new Color(255, 200, 40);
            case HEAL -> new Color(70, 220, 120);
            default -> null;
        };
        if (flash != null && t < 0.4) {
            float a = (float) (0.45 * (1 - t / 0.4));
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(a));
            g2.setColor(flash);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
        }
        String text = reaction.floating();
        if (!text.isEmpty()) {
            boolean crit = reaction.kind() == CardReaction.Kind.CRIT;
            g2.setFont(GuiTheme.sans(java.awt.Font.BOLD, crit ? 24f : 18f));
            Color c = switch (reaction.kind()) {
                case HEAL -> new Color(70, 220, 120);
                case MISS -> new Color(190, 190, 200);
                case CRIT -> new Color(255, 200, 40);
                default -> new Color(255, 90, 90);
            };
            float a = (float) Math.max(0, Math.min(1, 1.6 * (1 - t)));
            int x = getWidth() - g2.getFontMetrics().stringWidth(text) - 18;
            int y = (int) (getHeight() * 0.62 - t * 34);
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(a));
            g2.setColor(new Color(0, 0, 0));
            g2.drawString(text, x + 1, y + 1);
            g2.setColor(c);
            g2.drawString(text, x, y);
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
        }
        if (t < 0.9) {
            g2.setFont(GuiTheme.sans(java.awt.Font.BOLD, 11f));
            String line = reaction.bubble();
            int w = g2.getFontMetrics().stringWidth(line) + 16;
            int x = Math.max(4, getWidth() - w - 6);
            int y = 3;
            float a = (float) (t < 0.1 ? t / 0.1 : t > 0.75 ? (0.9 - t) / 0.15 : 1.0);
            g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, a))));
            g2.setColor(new Color(255, 255, 255));
            g2.fillRoundRect(x, y, w, 20, 10, 10);
            g2.fillPolygon(new int[] {x + 12, x + 20, x + 14}, new int[] {y + 19, y + 19, y + 25}, 3);
            g2.setColor(new Color(30, 30, 40));
            g2.drawString(line, x + 8, y + 14);
            g2.setComposite(java.awt.AlphaComposite.SrcOver);
        }
        g2.dispose();
    }

    /** Test hook: the reaction this card is playing, or null. */
    CardReaction reaction() { return reaction; }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(alive ? GuiTheme.SURFACE_ALT : GuiTheme.SURFACE);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
        g2.setColor(alive ? accent : GuiTheme.BORDER);
        g2.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);
        g2.setColor(GuiTheme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
        g2.dispose();
    }
}
