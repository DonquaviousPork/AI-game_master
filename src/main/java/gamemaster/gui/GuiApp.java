package gamemaster.gui;

import gamemaster.app.GameFacade;
import gamemaster.llm.LLMConfig;
import gamemaster.llm.LLMException;
import gamemaster.llm.OpenAIClient;
import gamemaster.save.SaveRepository;
import gamemaster.save.SaveService;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Starts the graphical game: reads .env, connects to the model and opens the window. */
public class GuiApp {

    public static void main(String[] args) {
        GuiTheme.install();
        GameFacade game;
        try {
            LLMConfig config = LLMConfig.load();
            game = new GameFacade(new OpenAIClient(config), new SaveService(new SaveRepository()));
        } catch (LLMException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "AI Game Master", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        GameFacade finalGame = game;
        SwingUtilities.invokeLater(() -> new MainWindow(finalGame).setVisible(true));
    }
}
