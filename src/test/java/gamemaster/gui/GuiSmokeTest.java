package gamemaster.gui;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.ai.FakeLLM;
import gamemaster.app.GameFacade;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import gamemaster.save.SaveRepository;
import gamemaster.save.SaveService;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** Builds the real screens without opening a window and paints them, so a broken layout or theme fails the build. */
class GuiSmokeTest {

    private static final String SCENARIO = """
            {"title":"The Hollow Crypt","description":"A cold wind blows from an old crypt.",
             "opening_event":"A skeleton rises from a tomb.",
             "monsters":[{"name":"Skeleton","health":9,"armor_class":12,"weapon":"dagger"}]}""";

    private static int colourCount(JComponent c) {
        c.setSize(1100, 700);
        c.addNotify();
        layout(c);
        BufferedImage img = new BufferedImage(1100, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        c.paint(g);
        g.dispose();
        Set<Integer> colours = new HashSet<>();
        for (int y = 0; y < 700; y += 7) {
            for (int x = 0; x < 1100; x += 7) {
                colours.add(img.getRGB(x, y));
            }
        }
        return colours.size();
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component k : c.getComponents()) {
            if (k.getWidth() == 0 || k.getHeight() == 0) {
                k.setSize(k.getPreferredSize());
            }
            if (k instanceof Container ct) {
                layout(ct);
            }
        }
    }

    @Test
    void setupAndPlayScreensBuildAndPaint() throws Exception {
        GuiTheme.install();
        GameFacade game = new GameFacade(new FakeLLM(SCENARIO),
                new SaveService(new SaveRepository(Files.createTempDirectory("gui"))), new RuleEngine(new FixedDice()));
        game.addCharacter("Brenna", "Fighter");
        game.addCharacter("Mira", "Wizard");
        AppPanel[] app = new AppPanel[1];
        SwingUtilities.invokeAndWait(() -> app[0] = new AppPanel(game));
        int[] setupColours = new int[1];
        int[] playColours = new int[1];
        SwingUtilities.invokeAndWait(() -> setupColours[0] = colourCount(app[0]));
        SwingUtilities.invokeAndWait(() -> app[0].playScreen().begin(game.startAdventure("crypt")));
        SwingUtilities.invokeAndWait(() -> playColours[0] = colourCount(app[0].playScreen()));
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(app[0].playScreen().hasUnsavedProgress(), "a fresh game has nothing to lose");
            app[0].playScreen().markUnsaved();
            assertTrue(app[0].playScreen().hasUnsavedProgress());
            app[0].playScreen().resume();
            assertFalse(app[0].playScreen().hasUnsavedProgress(), "loading a game starts clean");
        });
        assertTrue(setupColours[0] > 20, "setup screen looks blank: " + setupColours[0]);
        assertTrue(playColours[0] > 20, "play screen looks blank: " + playColours[0]);
    }
}
