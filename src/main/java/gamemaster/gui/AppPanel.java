package gamemaster.gui;

import gamemaster.app.GameFacade;
import gamemaster.audio.Sound;
import java.awt.CardLayout;
import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Holds the title screen, the setup screen, the briefing and the play screen and switches between them. */
final class AppPanel extends JPanel {

    private static final String COVER = "cover";
    private static final String SETUP = "setup";
    private static final String PLAY = "play";
    private static final String BRIEFING = "briefing";

    private final CardLayout cards = new CardLayout();
    private final GamePanel play;
    private final SetupPanel setup;
    private final BriefingPanel briefing;
    private final CoverPanel cover;

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
        setup.setOnBack(this::showCover);
        cover = new CoverPanel(GuiTheme.mode() == GuiTheme.Mode.DARK,
                () -> {
                    setup.showMenu();
                    cards.show(this, SETUP);
                },
                () -> setup.load(),
                () -> {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w instanceof MainWindow mw) {
                        new SettingsDialog(mw, game).open(mw);
                    }
                },
                () -> {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w != null) {
                        w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
                    }
                },
                game::isDemoMode, game::setDemoMode);
        play.setOnMenu(() -> {
            setup.showMenu();
            showCover();
        });
        add(cover, COVER);
        add(setup, SETUP);
        add(briefing, BRIEFING);
        add(play, PLAY);
        showCover();
    }

    private void showCover() {
        Sound.get().startMenuMusic();
        cards.show(this, COVER);
        cover.requestFocusInWindow();
    }

    GamePanel playScreen() { return play; }

    void showPlay() {
        Sound.get().stopMusic();
        cards.show(this, PLAY);
    }

    boolean isShowingPlay() { return play.isVisible() || briefing.isVisible(); }
}
