package gamemaster.cli;

import gamemaster.model.Stat;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads a line like "STR 4 DEX=3 con:5" into stat points. Names can be shortened to three letters. */
final class StatInput {

    private static final Pattern PAIR = Pattern.compile("([A-Za-z]+)\\s*[=:]?\\s*(-?\\d+)");

    private StatInput() {}

    static Map<Stat, Integer> parse(String line) {
        String text = line == null ? "" : line.trim();
        Map<Stat, Integer> out = new EnumMap<>(Stat.class);
        Matcher m = PAIR.matcher(text);
        while (m.find()) {
            Stat stat = find(m.group(1));
            if (out.containsKey(stat)) {
                throw new IllegalArgumentException(stat + " was given twice.");
            }
            out.put(stat, Integer.parseInt(m.group(2)));
        }
        if (!PAIR.matcher(text).replaceAll("").replace(",", "").isBlank()) {
            throw new IllegalArgumentException("Could not read \"" + text + "\". Use pairs like STR 4 DEX 3.");
        }
        return out;
    }

    private static Stat find(String word) {
        String w = word.toUpperCase();
        if (w.length() >= 3) {
            for (Stat s : Stat.values()) {
                if (s.name().startsWith(w)) {
                    return s;
                }
            }
        }
        throw new IllegalArgumentException("Unknown stat \"" + word + "\". Use STR, DEX, CON, INT, WIS or CHA.");
    }
}
