package gamemaster.gui;

/** Text that wraps at a fixed width inside a JLabel (Swing labels only wrap when given HTML). */
final class WrappedText {

    private WrappedText() {}

    static String html(String text, int widthPixels) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><body style='width:" + widthPixels + "px'>" + safe + "</body></html>";
    }
}
