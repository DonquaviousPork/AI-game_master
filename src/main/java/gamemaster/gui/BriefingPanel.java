package gamemaster.gui;

import gamemaster.ai.GeneratedScenario;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Shown once, right after the adventure is created: what the story is and the goal the party is working towards. */
final class BriefingPanel extends BackdropPanel {

    private final JLabel title = new JLabel(" ");
    private final JLabel description = text(GuiTheme.sans(java.awt.Font.PLAIN, 16f), GuiTheme.TEXT);
    private final JLabel objective = text(GuiTheme.serif(java.awt.Font.BOLD, 24f), GuiTheme.TEXT);
    private final JLabel rules = text(GuiTheme.sans(java.awt.Font.PLAIN, 14f), GuiTheme.MUTED);
    private final StyledButton begin = new StyledButton("Begin the adventure", true);

    BriefingPanel(Runnable onBegin) {
        super(new GridBagLayout());
        setBorder(GuiTheme.padding(30, 60, 30, 60));

        title.setFont(GuiTheme.serif(java.awt.Font.BOLD, 40f));
        title.setForeground(GuiTheme.PURPLE_SOFT);
        JLabel heading = new JLabel("YOUR OBJECTIVE");
        heading.setFont(GuiTheme.sans(java.awt.Font.BOLD, 13f));
        heading.setForeground(GuiTheme.PURPLE_SOFT);

        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GuiTheme.BORDER, 1, true), GuiTheme.padding(18, 22, 18, 22)));
        card.add(heading, BorderLayout.NORTH);
        card.add(objective, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        c.gridy = 0;
        add(title, c);
        c.gridy = 1;
        add(description, c);
        c.gridy = 2;
        c.insets = new Insets(18, 0, 10, 0);
        add(card, c);
        c.gridy = 3;
        c.insets = new Insets(6, 0, 18, 0);
        add(rules, c);
        c.gridy = 4;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        add(begin, c);
        begin.addActionListener(e -> onBegin.run());
    }

    void show(GeneratedScenario s, int turns) {
        title.setText(s.scenario().title());
        description.setText(WrappedText.html(s.scenario().description(), 820));
        objective.setText(WrappedText.html(s.scenario().objective(), 740));
        rules.setText(WrappedText.html("You have " + turns + " turns. Reach the objective before they run out and you win. "
                + "If the party falls, or the turns run out first, the adventure ends in defeat. "
                + "The turn counter and your objective stay at the top of the screen.", 820));
    }

    private static JLabel text(java.awt.Font font, java.awt.Color colour) {
        JLabel a = new JLabel(" ");
        a.setFont(font);
        a.setForeground(colour);
        a.setBorder(BorderFactory.createEmptyBorder());
        return a;
    }
}
