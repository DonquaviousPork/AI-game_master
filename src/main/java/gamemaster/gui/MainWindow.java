package gamemaster.gui;

import gamemaster.app.GameFacade;
import java.awt.Dimension;
import javax.swing.JFrame;

/** The application window. */
final class MainWindow extends JFrame {

    MainWindow(GameFacade game) {
        super("AI Game Master");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setContentPane(new AppPanel(game));
        setMinimumSize(new Dimension(980, 640));
        setSize(1180, 760);
        setLocationRelativeTo(null);
    }
}
