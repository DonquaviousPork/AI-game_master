package gamemaster.gui;

import gamemaster.game.StatusKind;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JComponent;

/** Small pixel icons for the effects on a card (flame, snowflake, down arrow), each with its turns left. */
final class StatusBadges extends JComponent {

    private static final int PX = 3;            // screen pixels per icon pixel
    private static final int ICON = 7 * PX;
    private static final int GAP = 6;

    private static final String[] FLAME = {
        "...#...",
        "..##...",
        "..###..",
        ".#####.",
        ".#####.",
        ".#####.",
        "..###.."};
    private static final String[] SNOW = {
        "#..#..#",
        ".#.#.#.",
        "..###..",
        "#######",
        "..###..",
        ".#.#.#.",
        "#..#..#"};
    private static final String[] ARROW = {
        "..###..",
        "..###..",
        "..###..",
        "#######",
        ".#####.",
        "..###..",
        "...#..."};

    private final List<GameViewModel.Effect> effects;

    StatusBadges(List<GameViewModel.Effect> effects) {
        this.effects = List.copyOf(effects);
        setOpaque(false);
        setToolTipText(tooltip());
    }

    static Color colour(StatusKind kind) {
        return switch (kind) {
            case BURN -> new Color(0xFF, 0x8A, 0x2A);
            case CHILL -> new Color(0x5E, 0xD0, 0xFF);
            case WEAKEN -> new Color(0xB5, 0x8C, 0xFF);
        };
    }

    static String[] shape(StatusKind kind) {
        return switch (kind) {
            case BURN -> FLAME;
            case CHILL -> SNOW;
            case WEAKEN -> ARROW;
        };
    }

    private String tooltip() {
        StringBuilder sb = new StringBuilder("<html>");
        for (var e : effects) {
            sb.append("<b>").append(e.kind().label()).append("</b>: ").append(e.kind().effectText())
                    .append(" (").append(e.turns()).append(e.turns() == 1 ? " turn left" : " turns left")
                    .append(")<br>");
        }
        return effects.isEmpty() ? null : sb.append("</html>").toString();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(effects.size() * (ICON + 12 + GAP), ICON + 4);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(GuiTheme.sans(Font.BOLD, 12f));
        int x = 0;
        int y = Math.max(0, (getHeight() - ICON) / 2);
        for (var e : effects) {
            g.setColor(colour(e.kind()));
            String[] rows = shape(e.kind());
            for (int r = 0; r < rows.length; r++) {
                for (int c = 0; c < rows[r].length(); c++) {
                    if (rows[r].charAt(c) == '#') {
                        g.fillRect(x + c * PX, y + r * PX, PX, PX);
                    }
                }
            }
            g.drawString(String.valueOf(e.turns()), x + ICON + 2, y + ICON - 2);
            x += ICON + 12 + GAP;
        }
        g.dispose();
    }

    /** Test hook. */
    List<GameViewModel.Effect> effects() { return effects; }
}
