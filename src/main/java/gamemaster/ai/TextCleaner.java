package gamemaster.ai;

/** Replaces typographic characters models like to use with plain ones, so every console shows them. */
final class TextCleaner {

    private TextCleaner() {}

    static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("[\\u2010-\\u2015\\u2212]", "-")
                .replaceAll("[\\u2018\\u2019]", "'")
                .replaceAll("[\\u201C\\u201D]", "\"")
                .replace(' ', ' ')
                .replace(' ', ' ')
                .replace("…", "...")
                .trim();
    }
}
