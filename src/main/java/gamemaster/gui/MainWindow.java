package gamemaster.gui;

import gamemaster.app.GameFacade;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

/** The application window. */
final class MainWindow extends JFrame {

    private final GameFacade game;
    private AppPanel app;

    MainWindow(GameFacade game) {
        super("AI Game Master");
        this.game = game;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { requestClose(); }
        });
        app = new AppPanel(game);
        setContentPane(app);
        setJMenuBar(buildMenuBar());
        setMinimumSize(new Dimension(980, 640));
        setSize(1180, 760);
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        ButtonGroup group = new ButtonGroup();
        for (GuiTheme.Mode m : GuiTheme.Mode.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(m == GuiTheme.Mode.DARK ? "Dark mode" : "Light mode",
                    m == GuiTheme.mode());
            item.setAccelerator(KeyStroke.getKeyStroke(m == GuiTheme.Mode.DARK ? KeyEvent.VK_D : KeyEvent.VK_L,
                    java.awt.event.InputEvent.CTRL_DOWN_MASK));
            item.addActionListener(e -> switchMode(m));
            group.add(item);
            view.add(item);
        }
        bar.add(view);
        return bar;
    }

    /**
     * Rebuilds the window in the other colour mode. The game itself lives in the facade, so only the story text
     * and the screen being shown are carried over to the new panels.
     */
    void switchMode(GuiTheme.Mode mode) {
        if (mode == GuiTheme.mode()) {
            return;
        }
        if (app.playScreen().isBusy()) {
            JOptionPane.showMessageDialog(this, "Wait for the game master to finish this turn, then switch.",
                    "Colour mode", JOptionPane.INFORMATION_MESSAGE);
            setJMenuBar(buildMenuBar());
            return;
        }
        List<GamePanel.StoryEntry> story = app.playScreen().exportStory();
        boolean unsaved = app.playScreen().hasUnsavedProgress();
        boolean playing = app.isShowingPlay();
        app.playScreen().detach();

        GuiTheme.setPalette(mode);
        GuiTheme.saveMode(mode);
        GuiTheme.install();

        app = new AppPanel(game);
        setContentPane(app);
        setJMenuBar(buildMenuBar());
        if (playing) {
            app.playScreen().importStory(story, unsaved);
            app.showPlay();
        }
        revalidate();
        repaint();
    }

    /** Closes the window, but first warns if the current game has progress that was never saved. */
    void requestClose() {
        GamePanel play = app.playScreen();
        if (play.hasUnsavedProgress()) {
            String[] options = {"Save and quit", "Quit without saving", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this,
                    "You have unsaved progress. If you quit now it will be lost.",
                    "Unsaved game", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, options, options[0]);
            if (choice == 0) {
                if (!play.save()) {
                    return;     // the save was cancelled or failed: stay open
                }
            } else if (choice != 1) {
                return;         // Cancel, or the dialog was dismissed
            }
        }
        dispose();
        System.exit(0);
    }
}
