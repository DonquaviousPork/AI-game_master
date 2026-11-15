package gamemaster.gui;

import gamemaster.audio.Sound;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/** The sound settings: music volume, effects volume and mute. Changes apply at once and are remembered. */
final class SoundDialog extends JDialog {

    SoundDialog(java.awt.Frame owner) {
        super(owner, "Sound settings", true);
        Sound sound = Sound.get();

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(GuiTheme.BG);
        body.setBorder(GuiTheme.padding(18, 22, 12, 22));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        JSlider music = slider(sound.musicVolume());
        music.addChangeListener(e -> sound.setMusicVolume(music.getValue()));
        JSlider effects = slider(sound.effectsVolume());
        effects.addChangeListener(e -> sound.setEffectsVolume(effects.getValue()));
        JCheckBox mute = new JCheckBox("Mute everything", sound.isMuted());
        mute.setOpaque(false);
        mute.setForeground(GuiTheme.TEXT);
        mute.setFont(GuiTheme.sans(java.awt.Font.PLAIN, 14f));
        mute.addActionListener(e -> sound.setMuted(mute.isSelected()));

        row(body, c, 0, "Music", music);
        row(body, c, 1, "Effects", effects);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        body.add(mute, c);

        StyledButton close = new StyledButton("Close", true);
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setBackground(GuiTheme.BG);
        south.setBorder(GuiTheme.padding(0, 0, 12, 0));
        south.add(close);

        setLayout(new BorderLayout());
        add(body, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private static JSlider slider(int value) {
        JSlider s = new JSlider(0, 100, value);
        s.setOpaque(false);
        s.setForeground(GuiTheme.PURPLE_SOFT);
        return s;
    }

    private static void row(JPanel body, GridBagConstraints c, int y, String text, Component control) {
        JLabel label = new JLabel(text);
        label.setForeground(GuiTheme.TEXT);
        label.setFont(GuiTheme.sans(java.awt.Font.BOLD, 14f));
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 1;
        body.add(label, c);
        c.gridx = 1;
        body.add(control, c);
    }
}
