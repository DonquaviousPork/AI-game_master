package gamemaster.gui;

import gamemaster.audio.Sound;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;

/** The sound settings: music volume, effects volume and mute. Changes apply at once and are remembered. */
final class SoundDialog extends PixelDialog {

    SoundDialog(java.awt.Frame owner) {
        super(owner, "SOUND");
        Sound sound = Sound.get();

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 4, 8, 12);
        c.anchor = GridBagConstraints.WEST;

        PixelSlider music = new PixelSlider(sound.musicVolume());
        music.addChangeListener(e -> sound.setMusicVolume(music.getValue()));
        PixelSlider effects = new PixelSlider(sound.effectsVolume());
        effects.addChangeListener(e -> sound.setEffectsVolume(effects.getValue()));
        PixelCheck mute = new PixelCheck("Mute everything", sound.isMuted());
        mute.addActionListener(e -> sound.setMuted(mute.isSelected()));

        row(body, c, 0, "MUSIC", music);
        row(body, c, 1, "EFFECTS", effects);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        body.add(mute, c);

        body().add(body, BorderLayout.CENTER);
        PixelButton close = new PixelButton("CLOSE", true);
        close.addActionListener(e -> dispose());
        setButtons(close);
        getRootPane().setDefaultButton(close);
    }

    private static void row(JPanel body, GridBagConstraints c, int y, String text, Component control) {
        c.gridx = 0;
        c.gridy = y;
        c.gridwidth = 1;
        body.add(new PixelLabel(text, 2, PixelTheme::text), c);
        c.gridx = 1;
        body.add(control, c);
    }
}
