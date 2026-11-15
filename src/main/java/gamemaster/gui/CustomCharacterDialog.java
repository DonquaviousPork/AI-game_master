package gamemaster.gui;

import gamemaster.model.CharacterFactory;
import gamemaster.model.Stat;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Lets the player spend exactly 20 points across the six stats, with a live "points left" count. */
final class CustomCharacterDialog {

    private static final int MAX_POINTS_PER_STAT = CharacterFactory.MAX_STAT - CharacterFactory.BASE_STAT;

    private CustomCharacterDialog() {}

    /** Shows the dialog; returns the chosen points, or empty if cancelled. */
    static Optional<Map<Stat, Integer>> ask(Component parent, String characterName) {
        PixelDialog dialog = new PixelDialog(PixelDialog.ownerOf(parent), "CUSTOM STATS: " + characterName);
        JLabel intro = new JLabel(WrappedText.html("Spend exactly " + CharacterFactory.POINT_POOL
                + " points. Every stat starts at " + CharacterFactory.BASE_STAT + " and can reach "
                + CharacterFactory.MAX_STAT + ".", 400));
        intro.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 13f));
        intro.setForeground(PixelTheme.muted());
        dialog.body().add(intro, BorderLayout.NORTH);

        Map<Stat, PixelStepper> steppers = new EnumMap<>(Stat.class);
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 4, 16);
        c.anchor = GridBagConstraints.WEST;
        int y = 0;
        for (Stat s : Stat.values()) {
            PixelStepper stepper = new PixelStepper(0, 0, MAX_POINTS_PER_STAT);
            steppers.put(s, stepper);
            c.gridx = 0;
            c.gridy = y++;
            grid.add(new PixelLabel(s.name(), 2, PixelTheme::text), c);
            c.gridx = 1;
            grid.add(stepper, c);
        }
        dialog.body().add(grid, BorderLayout.CENTER);

        JLabel left = new JLabel();
        left.setFont(GuiTheme.sans(java.awt.Font.BOLD, 13f));
        PixelButton ok = new PixelButton("ADD CHARACTER", true);
        PixelButton cancel = new PixelButton("CANCEL", false);
        dialog.body().add(left, BorderLayout.SOUTH);
        dialog.setButtons(cancel, ok);

        AtomicReference<Optional<Map<Stat, Integer>>> result = new AtomicReference<>(Optional.empty());
        Runnable refresh = () -> {
            int spent = steppers.values().stream().mapToInt(PixelStepper::getValue).sum();
            int remaining = CharacterFactory.POINT_POOL - spent;
            left.setText(remaining == 0 ? "All points spent" : remaining > 0 ? remaining + " points left"
                    : (-remaining) + " points too many");
            left.setForeground(remaining == 0 ? PixelTheme.ok() : remaining > 0 ? PixelTheme.hilite() : PixelTheme.bad());
            ok.setEnabled(remaining == 0);
        };
        steppers.values().forEach(sp -> sp.addChangeListener(e -> refresh.run()));
        refresh.run();

        ok.addActionListener(e -> {
            Map<Stat, Integer> points = new EnumMap<>(Stat.class);
            steppers.forEach((stat, sp) -> points.put(stat, sp.getValue()));
            result.set(Optional.of(points));
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());
        dialog.open(parent);
        return result.get();
    }
}
