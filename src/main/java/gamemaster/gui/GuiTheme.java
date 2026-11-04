package gamemaster.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;

/** Colours, fonts and Swing defaults: a dark forest green with a purple glow. */
final class GuiTheme {

    static final Color BG = new Color(0x0A1410);
    static final Color SURFACE = new Color(0x11241B);
    static final Color SURFACE_ALT = new Color(0x183327);
    static final Color BORDER = new Color(0x26493B);
    static final Color TEXT = new Color(0xE6F0EA);
    static final Color MUTED = new Color(0x86A396);
    static final Color PURPLE = new Color(0x9B7BE0);
    static final Color PURPLE_DARK = new Color(0x5E3FA8);
    static final Color PURPLE_SOFT = new Color(0xBBA6F4);
    static final Color HEALTH_OK = new Color(0x5FD39A);
    static final Color HEALTH_MID = new Color(0xE3B04B);
    static final Color HEALTH_LOW = new Color(0xD9617A);
    static final Color RULES = new Color(0x7FD6C2);
    static final Color ERROR = new Color(0xF08A9C);

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

    /** Applies the dark theme to every standard Swing component the app uses. */
    static void install() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // keep the default look if Metal is somehow unavailable
        }
        put("Panel.background", BG);
        put("OptionPane.background", SURFACE);
        put("OptionPane.messageForeground", TEXT);
        put("OptionPane.foreground", TEXT);
        put("Label.foreground", TEXT);
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
