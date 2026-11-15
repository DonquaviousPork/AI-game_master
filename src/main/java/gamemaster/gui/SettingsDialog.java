package gamemaster.gui;

import gamemaster.app.GameFacade;
import java.awt.BorderLayout;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/** The settings reachable from the title screen: colour mode, demo mode and sound. */
final class SettingsDialog extends PixelDialog {

    SettingsDialog(MainWindow owner, GameFacade game) {
        super(owner, "SETTINGS");
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        body.add(heading("APPEARANCE"));
        JPanel modes = row();
        for (GuiTheme.Mode m : GuiTheme.Mode.values()) {
            PixelButton b = new PixelButton(m == GuiTheme.Mode.DARK ? "DUSK" : "DAYTIME", false);
            b.setChosen(m == GuiTheme.mode());
            b.addActionListener(e -> {
                dispose();
                owner.switchMode(m);
            });
            modes.add(b);
        }
        body.add(modes);

        body.add(heading("DEMO MODE"));
        PixelCheck demo = new PixelCheck("5 turns and one easy enemy, so a victory can be shown quickly",
                game.isDemoMode());
        demo.addActionListener(e -> game.setDemoMode(demo.isSelected()));
        JPanel demoRow = row();
        demoRow.add(demo);
        body.add(demoRow);

        body.add(heading("SOUND"));
        PixelButton sound = new PixelButton("SOUND SETTINGS...", false);
        sound.addActionListener(e -> new SoundDialog(owner).open(this));
        JPanel soundRow = row();
        soundRow.add(sound);
        body.add(soundRow);

        body().add(body, BorderLayout.CENTER);
        PixelButton close = new PixelButton("CLOSE", true);
        close.addActionListener(e -> dispose());
        setButtons(close);
        getRootPane().setDefaultButton(close);
    }

    private static JPanel row() {
        JPanel p = new JPanel() {
            @Override public java.awt.Component add(java.awt.Component c) {
                if (getComponentCount() > 0) {
                    super.add(javax.swing.Box.createHorizontalStrut(8));
                }
                return super.add(c);
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(0f);
        return p;
    }

    private static PixelLabel heading(String text) {
        PixelLabel l = new PixelLabel(text, 2, PixelTheme::text);
        l.setAlignmentX(0f);
        l.setBorder(GuiTheme.padding(18, 0, 8, 0));
        return l;
    }
}
