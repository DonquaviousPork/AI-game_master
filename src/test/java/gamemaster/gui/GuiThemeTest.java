package gamemaster.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import org.junit.jupiter.api.Test;

/** Guards readability: every text colour must contrast with the background it is drawn on, in both modes. */
class GuiThemeTest {

    private static double luminance(Color c) {
        double[] v = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        for (int i = 0; i < 3; i++) {
            v[i] = v[i] <= 0.03928 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }

    static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static void need(double minimum, String what, Color text, Color background) {
        double ratio = contrast(text, background);
        assertTrue(ratio >= minimum, GuiTheme.mode() + ": " + what + " contrast is " + String.format("%.2f", ratio)
                + ", needs " + minimum);
    }

    private void checkMode(GuiTheme.Mode mode) {
        GuiTheme.setPalette(mode);
        need(4.5, "text on window", GuiTheme.TEXT, GuiTheme.BG);
        need(4.5, "text on card", GuiTheme.TEXT, GuiTheme.SURFACE);
        need(4.5, "text on field or button", GuiTheme.TEXT, GuiTheme.SURFACE_ALT);
        need(4.5, "muted text on window", GuiTheme.MUTED, GuiTheme.BG);
        need(4.5, "muted text on card", GuiTheme.MUTED, GuiTheme.SURFACE);
        need(4.5, "muted text on field", GuiTheme.MUTED, GuiTheme.SURFACE_ALT);
        need(4.5, "accent text on card", GuiTheme.PURPLE_SOFT, GuiTheme.SURFACE);
        need(4.5, "accent text on window", GuiTheme.PURPLE_SOFT, GuiTheme.BG);
        need(4.5, "accent text on field", GuiTheme.PURPLE_SOFT, GuiTheme.SURFACE_ALT);
        need(4.5, "player line on story", GuiTheme.PURPLE, GuiTheme.SURFACE);
        need(4.5, "rules line on story", GuiTheme.RULES, GuiTheme.SURFACE);
        need(4.5, "scene text on story", GuiTheme.SCENE_TEXT, GuiTheme.SURFACE);
        need(4.5, "error text on window", GuiTheme.ERROR, GuiTheme.BG);
        need(4.5, "error text on card", GuiTheme.ERROR, GuiTheme.SURFACE);
        need(4.0, "saved message on window", GuiTheme.HEALTH_OK, GuiTheme.BG);
        need(4.5, "button text", GuiTheme.ON_PRIMARY, GuiTheme.PRIMARY_FILL);
        need(4.5, "button text when hovered", GuiTheme.ON_PRIMARY, GuiTheme.PRIMARY_HOVER);
        need(4.5, "selected item text", GuiTheme.TEXT, GuiTheme.PURPLE_DARK);
        need(4.5, "combat label", GuiTheme.TEXT, GuiTheme.COMBAT_FILL);
        need(4.0, "health number on full bar", GuiTheme.ON_BAR, GuiTheme.HEALTH_OK);
        need(4.0, "health number on amber bar", GuiTheme.ON_BAR, GuiTheme.HEALTH_MID);
        need(4.0, "health number on red bar", GuiTheme.ON_BAR, GuiTheme.HEALTH_LOW);
    }

    @Test
    void darkModeIsReadable() {
        try {
            checkMode(GuiTheme.Mode.DARK);
        } finally {
            GuiTheme.setPalette(GuiTheme.Mode.DARK);
        }
    }

    @Test
    void lightModeIsReadable() {
        try {
            checkMode(GuiTheme.Mode.LIGHT);
        } finally {
            GuiTheme.setPalette(GuiTheme.Mode.DARK);
        }
    }

    @Test
    void theTwoModesReallyDiffer() {
        GuiTheme.setPalette(GuiTheme.Mode.DARK);
        Color darkBg = GuiTheme.BG;
        GuiTheme.setPalette(GuiTheme.Mode.LIGHT);
        assertTrue(luminance(darkBg) < 0.1 && luminance(GuiTheme.BG) > 0.7);
        GuiTheme.setPalette(GuiTheme.Mode.DARK);
    }
}
