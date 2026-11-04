package gamemaster.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;

/** Colours, fonts and Swing defaults: a dark forest green with a purple glow. */
final class GuiTheme {

    /** The two colour schemes. Dark is a forest green with a purple glow; light is the same idea on pale mint. */
    enum Mode { DARK, LIGHT }

    static Color BG, SURFACE, SURFACE_ALT, BORDER, TEXT, MUTED;
    static Color PURPLE, PURPLE_DARK, PURPLE_SOFT;
    static Color HEALTH_OK, HEALTH_MID, HEALTH_LOW, RULES, ERROR;
    /** Text on a filled primary button, on the filled part of a health bar, and the fill colours behind them. */
    static Color ON_PRIMARY, ON_BAR, PRIMARY_FILL, PRIMARY_HOVER, COMBAT_FILL, SCENE_TEXT;
    /** Soft background glows (already carry their transparency). */
    static Color GLOW_PURPLE, GLOW_GREEN;

    private static Mode mode = Mode.DARK;

    private static final Set<String> FAMILIES =
            Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());

    private GuiTheme() {}

    static Font serif(int style, float size) {
        return new Font(pick("Georgia", "Cambria", "Palatino Linotype", "Book Antiqua", "Serif"), style, 1).deriveFont(size);
    }

    static Font sans(int style, float size) {
        return new Font(pick("Segoe UI", "Helvetica Neue", "Inter", "Dialog"), style, 1).deriveFont(size);
    }

    static Font mono(int style, float size) {
        return new Font(pick("Consolas", "Menlo", "DejaVu Sans Mono", "Monospaced"), style, 1).deriveFont(size);
    }

    private static String pick(String... names) {
        for (String n : names) {
            if (FAMILIES.contains(n)) {
                return n;
            }
        }
        return names[names.length - 1];
    }

    static Border padding(int top, int left, int bottom, int right) {
        return BorderFactory.createEmptyBorder(top, left, bottom, right);
    }

    static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), padding(8, 12, 8, 12));
    }

    static Border fieldBorderTight() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), padding(4, 8, 4, 4));
    }

    static Mode mode() { return mode; }

    /** The mode the player used last time, or dark when nothing was saved. */
    static Mode savedMode() {
        try {
            return Mode.valueOf(Preferences.userNodeForPackage(GuiTheme.class).get("mode", "DARK"));
        } catch (RuntimeException e) {
            return Mode.DARK;
        }
    }

    static void saveMode(Mode m) {
        try {
            Preferences.userNodeForPackage(GuiTheme.class).put("mode", m.name());
        } catch (RuntimeException ignored) {
            // remembering the choice is a convenience only
        }
    }

    /** Sets the palette for a mode. Components read it when they are built, so the window is rebuilt after a change. */
    static void setPalette(Mode m) {
        mode = m;
        if (m == Mode.DARK) {
            BG = c(0x0A1410); SURFACE = c(0x11241B); SURFACE_ALT = c(0x183327); BORDER = c(0x26493B);
            TEXT = c(0xE6F0EA); MUTED = c(0x86A396);
            PURPLE = c(0x9B7BE0); PURPLE_DARK = c(0x5E3FA8); PURPLE_SOFT = c(0xBBA6F4);
            HEALTH_OK = c(0x5FD39A); HEALTH_MID = c(0xE3B04B); HEALTH_LOW = c(0xD9617A);
            RULES = c(0x7FD6C2); ERROR = c(0xF08A9C);
            ON_PRIMARY = c(0xFFFFFF); ON_BAR = c(0x0A1410); PRIMARY_FILL = c(0x5E3FA8); PRIMARY_HOVER = c(0x7452CC);
            COMBAT_FILL = c(0x5A2433); SCENE_TEXT = c(0xC9D8CF);
            GLOW_PURPLE = new Color(0x7D, 0x5B, 0xD6, 70); GLOW_GREEN = new Color(0x2E, 0x8B, 0x63, 45);
        } else {
            BG = c(0xEEF3EF); SURFACE = c(0xFBFDFB); SURFACE_ALT = c(0xE1ECE5); BORDER = c(0xA9C2B3);
            TEXT = c(0x13241B); MUTED = c(0x4F6A5B);
            PURPLE = c(0x5B3CC4); PURPLE_DARK = c(0xDACFF7); PURPLE_SOFT = c(0x4B2FA8);
            HEALTH_OK = c(0x1F7F55); HEALTH_MID = c(0xA86A00); HEALTH_LOW = c(0xB8324F);
            RULES = c(0x0F6B5A); ERROR = c(0xB3203F);
            ON_PRIMARY = c(0xFFFFFF); ON_BAR = c(0xFFFFFF); PRIMARY_FILL = c(0x5B3CC4); PRIMARY_HOVER = c(0x4A2FA8);
            COMBAT_FILL = c(0xF7D3DB); SCENE_TEXT = c(0x33493D);
            GLOW_PURPLE = new Color(0x7D, 0x5B, 0xD6, 40); GLOW_GREEN = new Color(0x2E, 0x8B, 0x63, 30);
        }
    }

    private static Color c(int rgb) { return new Color(rgb); }

    /** Metal theme driven by our palette, so menus, dialogs, buttons and pop-ups all share its colours and none is light-on-light. */
    private static final class PaletteTheme extends DefaultMetalTheme {
        @Override public String getName() { return "AI Game Master"; }
        @Override protected javax.swing.plaf.ColorUIResource getPrimary1() { return new ColorUIResource(PURPLE); }
        @Override protected javax.swing.plaf.ColorUIResource getPrimary2() { return new ColorUIResource(PURPLE_DARK); }
        @Override protected javax.swing.plaf.ColorUIResource getPrimary3() { return new ColorUIResource(PURPLE_DARK); }
        @Override protected javax.swing.plaf.ColorUIResource getSecondary1() { return new ColorUIResource(BORDER); }
        @Override protected javax.swing.plaf.ColorUIResource getSecondary2() { return new ColorUIResource(MUTED); }
        @Override protected javax.swing.plaf.ColorUIResource getSecondary3() { return new ColorUIResource(SURFACE_ALT); }
        @Override protected javax.swing.plaf.ColorUIResource getBlack() { return new ColorUIResource(TEXT); }
        @Override protected javax.swing.plaf.ColorUIResource getWhite() { return new ColorUIResource(SURFACE); }
    }

    /** Installs the look and feel and the colours of the current mode on every standard component the app uses. */
    static void install() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        if (BG == null) {
            setPalette(Mode.DARK);
        }
        try {
            UIManager.put("swing.boldMetal", Boolean.FALSE);
            MetalLookAndFeel.setCurrentTheme(new PaletteTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {
            // keep the default look if Metal is somehow unavailable
        }
        FontUIResource body = new FontUIResource(sans(Font.PLAIN, 13f));
        for (String key : new String[] {"Label.font", "Button.font", "ComboBox.font", "TextField.font", "List.font",
                "Menu.font", "MenuItem.font", "RadioButtonMenuItem.font", "CheckBoxMenuItem.font", "MenuBar.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "ToolTip.font", "Spinner.font", "FormattedTextField.font"}) {
            UIManager.put(key, body);
        }
        put("Panel.background", BG);
        put("OptionPane.background", SURFACE);
        put("OptionPane.messageForeground", TEXT);
        put("OptionPane.foreground", TEXT);
        put("Label.foreground", TEXT);
        put("Menu.background", SURFACE);
        put("Menu.foreground", TEXT);
        put("Menu.selectionBackground", PURPLE_DARK);
        put("Menu.selectionForeground", TEXT);
        put("MenuBar.background", SURFACE);
        put("MenuBar.foreground", TEXT);
        put("MenuItem.background", SURFACE);
        put("MenuItem.foreground", TEXT);
        put("MenuItem.selectionBackground", PURPLE_DARK);
        put("MenuItem.selectionForeground", TEXT);
        put("RadioButtonMenuItem.background", SURFACE);
        put("RadioButtonMenuItem.foreground", TEXT);
        put("RadioButtonMenuItem.selectionBackground", PURPLE_DARK);
        put("RadioButtonMenuItem.selectionForeground", TEXT);
        put("PopupMenu.background", SURFACE);
        put("PopupMenu.foreground", TEXT);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        put("TextField.background", SURFACE_ALT);
        put("TextField.foreground", TEXT);
        put("TextField.caretForeground", PURPLE_SOFT);
        put("TextField.selectionBackground", PURPLE_DARK);
        put("TextField.selectionForeground", TEXT);
        put("TextPane.background", SURFACE);
        put("TextPane.foreground", TEXT);
        put("ComboBox.background", SURFACE_ALT);
        put("ComboBox.foreground", TEXT);
        put("ComboBox.selectionBackground", PURPLE_DARK);
        put("ComboBox.selectionForeground", TEXT);
        put("ComboBox.buttonBackground", SURFACE_ALT);
        put("List.background", SURFACE_ALT);
        put("List.foreground", TEXT);
        put("List.selectionBackground", PURPLE_DARK);
        put("List.selectionForeground", TEXT);
        put("Spinner.background", SURFACE_ALT);
        put("Spinner.foreground", TEXT);
        put("FormattedTextField.background", SURFACE_ALT);
        put("FormattedTextField.foreground", TEXT);
        put("FormattedTextField.caretForeground", PURPLE_SOFT);
        put("Button.background", SURFACE_ALT);
        put("Button.foreground", TEXT);
        put("ScrollBar.thumb", BORDER);
        put("ScrollBar.thumbShadow", BORDER);
        put("ScrollBar.thumbHighlight", BORDER);
        put("ScrollBar.track", SURFACE);
        put("ScrollBar.background", SURFACE);
        put("ScrollPane.background", SURFACE);
        put("Viewport.background", SURFACE);
        put("ToolTip.background", SURFACE_ALT);
        put("ToolTip.foreground", TEXT);
        UIManager.put("ScrollBar.width", 12);
    }

    private static void put(String key, Color c) {
        UIManager.put(key, new ColorUIResource(c));
    }
}
