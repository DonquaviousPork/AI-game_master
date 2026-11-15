package gamemaster.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class PixelUiTest {

    private static final String[] LABELS = {"ADD", "CUSTOM STATS...", "BEGIN THE ADVENTURE", "LOAD SAVED GAME",
            "BACK TO TITLE", "REMOVE", "SETTINGS", "APPEARANCE", "DUSK", "DAYTIME", "DEMO MODE", "SOUND",
            "SOUND SETTINGS...", "CLOSE", "MUSIC", "EFFECTS", "SAVE GAME", "LOAD GAME", "SAVE", "LOAD", "CANCEL",
            "UNSAVED GAME", "SAVE AND QUIT", "QUIT WITHOUT SAVING", "SAVE AND LEAVE", "LEAVE WITHOUT SAVING",
            "COLOUR MODE", "OK", "CUSTOM STATS: BRENNA", "ADD CHARACTER", "YOUR PARTY (1 TO 4 HEROES)", "NEW HERO",
            "THE ADVENTURE", "YOUR OBJECTIVE", "AI GAME MASTER", "STRENGTH", "DEXTERITY", "CONSTITUTION",
            "INTELLIGENCE", "WISDOM", "CHARISMA", "-", "+"};

    @Test
    void everyLabelTheMenusUseCanBeDrawnInThePixelFont() {
        for (String label : LABELS) {
            for (char c : label.toCharArray()) {
                assertTrue(PixelFont.has(c), "no glyph for '" + c + "' in " + label);
            }
        }
    }

    @Test
    void theRulesScreenHeadingsCanBeDrawnInThePixelFont() {
        for (gamemaster.app.RulesText.Section sec : gamemaster.app.RulesText.sections()) {
            for (char c : sec.title().toCharArray()) {
                assertTrue(PixelFont.has(c), "no glyph for '" + c + "' in " + sec.title());
            }
        }
        for (char c : "RULES".toCharArray()) {
            assertTrue(PixelFont.has(c));
        }
    }

    @Test
    void healingLightIsPlannedAsAPartyCastNotAnAttack() {
        var picks = java.util.List.of(new BattlePlan.Pick("Mira", BattlePlan.HEAL, "the party"));
        assertEquals("Mira: Healing Light on the party", BattlePlan.display(picks));
        assertTrue(BattlePlan.instruction(picks).contains("heal_party"));
    }

    @Test
    void aPixelButtonIsAsWideAsItsLettering() {
        PixelButton b = new PixelButton("BEGIN THE ADVENTURE", true);
        assertEquals(PixelFont.width("BEGIN THE ADVENTURE", 2) + 40, b.getPreferredSize().width);
        assertEquals(b.getPreferredSize(), b.getMinimumSize());
    }

    @Test
    void themeColoursFollowTheMode() {
        GuiTheme.setPalette(GuiTheme.Mode.DARK);
        java.awt.Color dusk = PixelTheme.edge();
        GuiTheme.setPalette(GuiTheme.Mode.LIGHT);
        java.awt.Color day = PixelTheme.edge();
        GuiTheme.setPalette(GuiTheme.Mode.DARK);
        assertTrue(dusk.getRGB() != day.getRGB());
    }

    @Test
    void widgetsPaintWithoutAScreen() {
        GuiTheme.setPalette(GuiTheme.Mode.DARK);
        BufferedImage img = new BufferedImage(300, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        PixelButton b = new PixelButton("CLOSE", true);
        b.setSize(b.getPreferredSize());
        b.paint(g);
        PixelLabel l = new PixelLabel("HELLO", 3, PixelTheme::hilite);
        l.setSize(l.getPreferredSize());
        l.paint(g);
        PixelSlider s = new PixelSlider(40);
        s.setSize(s.getPreferredSize());
        s.paint(g);
        g.dispose();
        assertEquals(40, s.getValue());
        s.setValue(200);
        assertEquals(100, s.getValue());
    }

    @Test
    void aStepperStaysWithinItsLimits() {
        PixelStepper st = new PixelStepper(0, 0, 7);
        st.setValue(-3);
        assertEquals(0, st.getValue());
        st.setValue(99);
        assertEquals(7, st.getValue());
    }
}
