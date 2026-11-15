package gamemaster.gui;

import java.awt.Color;

/**
 * The colours of the title screen's menu, shared by the setup screen and every dialog: a dark plate with a
 * purple edge and gold highlight for the dusk scene, and the same plate in deep green with a lime edge for the
 * sunny scene. They follow the current {@link GuiTheme} mode each time they are read.
 */
final class PixelTheme {

    private PixelTheme() {}

    private static boolean dusk() { return GuiTheme.mode() == GuiTheme.Mode.DARK; }

    private static Color pick(int dusk, int day) { return new Color(dusk() ? dusk : day); }

    static Color plate()   { return pick(0x0A0614, 0x0B2A16); }
    static Color edge()    { return pick(0x5E3FA8, 0x2E7D32); }
    static Color corner()  { return pick(0xB79CF2, 0x8BE04E); }
    static Color hilite()  { return new Color(0xFFE27A); }
    static Color hiliteFill() { return new Color(0xFFC857); }
    static Color text()    { return pick(0xBFB0F0, 0xD4F0C0); }
    static Color body()    { return pick(0xE8E0FF, 0xF0FFE6); }
    static Color muted()   { return pick(0x8C7FC0, 0x8DB88A); }
    static Color field()   { return pick(0x150C2B, 0x0F3A1D); }
    static Color ok()      { return new Color(0x5FD39A); }
    static Color bad()     { return new Color(0xF08A9C); }
    static Color shadow()  { return pick(0x05030A, 0x04160B); }
}
