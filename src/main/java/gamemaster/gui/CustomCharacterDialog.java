package gamemaster.gui;

import gamemaster.model.CharacterFactory;
import gamemaster.model.Stat;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/** Lets the player spend exactly 20 points across the six stats, with a live "points left" count. */
final class CustomCharacterDialog {

    private static final int MAX_POINTS_PER_STAT = CharacterFactory.MAX_STAT - CharacterFactory.BASE_STAT;

    private CustomCharacterDialog() {}

    /** Shows the dialog; returns the chosen points, or empty if cancelled. */
    static Optional<Map<Stat, Integer>> ask(Component parent, String characterName) {
        JDialog dialog = new JDialog(javax.swing.SwingUtilities.getWindowAncestor(parent),
                "Custom stats for " + characterName, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(GuiTheme.SURFACE);
        content.setBorder(GuiTheme.padding(20, 24, 20, 24));

        JLabel intro = new JLabel("<html>Spend exactly " + CharacterFactory.POINT_POOL + " points. Every stat starts at "
                + CharacterFactory.BASE_STAT + " and can reach " + CharacterFactory.MAX_STAT + ".</html>");
        intro.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 13f));
        intro.setForeground(GuiTheme.MUTED);
        content.add(intro, BorderLayout.NORTH);

        Map<Stat, JSpinner> spinners = new EnumMap<>(Stat.class);
        JPanel grid = new JPanel(new GridLayout(Stat.values().length, 2, 12, 8));
        grid.setOpaque(false);
        for (Stat s : Stat.values()) {
            JLabel label = new JLabel(capitalise(s.name()));
            label.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, MAX_POINTS_PER_STAT, 1));
            spinner.setFont(GuiTheme.sans(java.awt.Font.BOLD, 14f));
            spinners.put(s, spinner);
            grid.add(label);
            grid.add(spinner);
        }
        content.add(grid, BorderLayout.CENTER);

        JLabel left = new JLabel("", SwingConstants.LEFT);
        left.setFont(GuiTheme.sans(java.awt.Font.BOLD, 13f));
        StyledButton ok = new StyledButton("Add character", true);
        StyledButton cancel = new StyledButton("Cancel", false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancel);
        buttons.add(ok);
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(left, BorderLayout.WEST);
        south.add(buttons, BorderLayout.EAST);
        content.add(south, BorderLayout.SOUTH);

        AtomicReference<Optional<Map<Stat, Integer>>> result = new AtomicReference<>(Optional.empty());
        Runnable refresh = () -> {
            int spent = spinners.values().stream().mapToInt(sp -> (Integer) sp.getValue()).sum();
            int remaining = CharacterFactory.POINT_POOL - spent;
            left.setText(remaining == 0 ? "All points spent" : remaining > 0 ? remaining + " points left"
                    : (-remaining) + " points too many");
            left.setForeground(remaining == 0 ? GuiTheme.HEALTH_OK : remaining > 0 ? GuiTheme.PURPLE_SOFT : GuiTheme.ERROR);
            ok.setEnabled(remaining == 0);
        };
        spinners.values().forEach(sp -> sp.addChangeListener(e -> refresh.run()));
        refresh.run();

        ok.addActionListener(e -> {
            Map<Stat, Integer> points = new EnumMap<>(Stat.class);
            spinners.forEach((stat, sp) -> points.put(stat, (Integer) sp.getValue()));
            result.set(Optional.of(points));
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return result.get();
    }

    private static String capitalise(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
