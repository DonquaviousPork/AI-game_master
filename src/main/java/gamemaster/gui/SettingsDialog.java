package gamemaster.gui;

import gamemaster.app.GameFacade;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/** The settings reachable from the title screen: colour mode, demo mode and sound. */
final class SettingsDialog extends JDialog {

    SettingsDialog(MainWindow owner, GameFacade game) {
        super(owner, "Settings", true);
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(GuiTheme.BG);
        body.setBorder(GuiTheme.padding(18, 22, 8, 22));

        body.add(heading("Appearance"));
        JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        modes.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (GuiTheme.Mode m : GuiTheme.Mode.values()) {
            JRadioButton b = new JRadioButton(m == GuiTheme.Mode.DARK ? "Dark (dusk)" : "Light (daytime)",
                    m == GuiTheme.mode());
            style(b);
            b.addActionListener(e -> {
                dispose();
                owner.switchMode(m);
            });
            group.add(b);
            modes.add(b);
        }
        body.add(modes);

        body.add(heading("Demo mode"));
        JCheckBox demo = new JCheckBox("5 turns and one easy enemy, so a victory can be shown quickly",
                game.isDemoMode());
        style(demo);
        demo.addActionListener(e -> game.setDemoMode(demo.isSelected()));
        JPanel demoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        demoRow.setOpaque(false);
        demoRow.add(demo);
        body.add(demoRow);

        body.add(heading("Sound"));
        StyledButton sound = new StyledButton("Sound settings...", false);
        sound.addActionListener(e -> new SoundDialog(owner).setVisible(true));
        JPanel soundRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        soundRow.setOpaque(false);
        soundRow.add(sound);
        body.add(soundRow);

        StyledButton close = new StyledButton("Close", true);
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setBackground(GuiTheme.BG);
        south.setBorder(GuiTheme.padding(8, 0, 12, 0));
        south.add(close);

        setLayout(new BorderLayout());
        add(body, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(GuiTheme.TEXT);
        l.setFont(GuiTheme.sans(java.awt.Font.BOLD, 14f));
        l.setBorder(GuiTheme.padding(14, 12, 6, 0));
        l.setAlignmentX(0f);
        return l;
    }

    private static void style(javax.swing.AbstractButton b) {
        b.setOpaque(false);
        b.setForeground(GuiTheme.TEXT);
        b.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
    }
}
