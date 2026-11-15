package gamemaster.gui;

import gamemaster.app.GameFacade;
import gamemaster.audio.Sound;
import java.awt.CardLayout;
import javax.swing.JPanel;

/** Holds the setup screen and the play screen and switches between them. */
final class AppPanel extends JPanel {

    private static final String SETUP = "setup";
    private static final String PLAY = "play";
    private static final String BRIEFING = "briefing";

    private final CardLayout cards = new CardLayout();
    private final GamePanel play;
    private final SetupPanel setup;
    private final BriefingPanel briefing;

    AppPanel(GameFacade game) {
        super(new CardLayout());
        setLayout(cards);
        play = new GamePanel(game);
        briefing = new BriefingPanel(() -> {
            Sound.get().stopMusic();
            cards.show(this, PLAY);
        });
        setup = new SetupPanel(game,
                scenario -> {
                    play.begin(scenario);
                    briefing.show(scenario, game.getState().getTurnLimit());
                    cards.show(this, BRIEFING);
                },
                () -> {
                    play.resume();
                    Sound.get().stopMusic();
                    cards.show(this, PLAY);
                });
        play.setOnMenu(() -> {
            setup.showMenu();
            Sound.get().startMenuMusic();
            cards.show(this, SETUP);
        });
        add(setup, SETUP);
        add(briefing, BRIEFING);
        add(play, PLAY);
        cards.show(this, SETUP);
        Sound.get().startMenuMusic();
    }

    GamePanel playScreen() { return play; }

    void showPlay() {
        Sound.get().stopMusic();
        cards.show(this, PLAY);
    }

    boolean isShowingPlay() { return play.isVisible() || briefing.isVisible(); }
}
