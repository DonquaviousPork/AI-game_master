package gamemaster.gui;

import gamemaster.app.GameFacade;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/** The application window. */
final class MainWindow extends JFrame {

    private final AppPanel app;

    MainWindow(GameFacade game) {
        super("AI Game Master");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { requestClose(); }
        });
        app = new AppPanel(game);
        setContentPane(app);
        setMinimumSize(new Dimension(980, 640));
        setSize(1180, 760);
        setLocationRelativeTo(null);
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
