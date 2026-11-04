package gamemaster.gui;

import gamemaster.app.GameFacade;
import java.awt.CardLayout;
import javax.swing.JPanel;

/** Holds the setup screen and the play screen and switches between them. */
final class AppPanel extends JPanel {

    private static final String SETUP = "setup";
    private static final String PLAY = "play";

    private final CardLayout cards = new CardLayout();
    private final GamePanel play;

    AppPanel(GameFacade game) {
        super(new CardLayout());
        setLayout(cards);
        play = new GamePanel(game);
        SetupPanel setup = new SetupPanel(game,
                scenario -> {
                    play.begin(scenario);
                    cards.show(this, PLAY);
                },
                () -> {
                    play.resume();
                    cards.show(this, PLAY);
                });
        add(setup, SETUP);
        add(play, PLAY);
        cards.show(this, SETUP);
    }

    GamePanel playScreen() { return play; }
}
