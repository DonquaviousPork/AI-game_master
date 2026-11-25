package gamemaster.cli;

import java.util.ArrayList;
import java.util.List;

/** Command-line options for the jar: which front end to start and where the CLI reads and writes. */
public final class CliOptions {

    public static final String USAGE = String.join(System.lineSeparator(),
            "AI Game Master",
            "",
            "Usage: java -jar AIGameMaster.jar [options]",
            "",
            "  (no options)      start the graphical game",
            "  -cli              start the command-line game and read commands from the keyboard",
            "  -input <file>     start the command-line game and run the commands in <file>, one per line",
            "                    (lines starting with # are comments; the run ends at the end of the file or /quit)",
            "  -output <file>    also write everything the CLI prints to <file>",
            "  -LLMAPI           use the OpenAI-compatible model API set in .env (this is the default)",
            "  -help             show this text",
            "",
            "Example: java -jar AIGameMaster.jar -LLMAPI -input task.txt -output result.txt");

    public final boolean help;
    public final boolean cli;
    public final String input;
    public final String output;

    private CliOptions(boolean help, boolean cli, String input, String output) {
        this.help = help;
        this.cli = cli;
        this.input = input;
        this.output = output;
    }

    /** Reads the options; throws IllegalArgumentException with a readable message for a bad option. */
    public static CliOptions parse(String[] args) {
        boolean help = false;
        boolean cli = false;
        String input = null;
        String output = null;
        List<String> a = new ArrayList<>(List.of(args));
        for (int i = 0; i < a.size(); i++) {
            String s = a.get(i);
            switch (s.toLowerCase()) {
                case "-help", "--help", "-h" -> help = true;
                case "-cli", "--cli" -> cli = true;
                case "-llmapi", "--llmapi" -> { /* the model API in .env is always used */ }
                case "-input", "--input" -> {
                    input = value(a, ++i, s);
                    cli = true;
                }
                case "-output", "--output" -> {
                    output = value(a, ++i, s);
                    cli = true;
                }
                default -> throw new IllegalArgumentException("Unknown option: " + s + ". Use -help to see the options.");
            }
        }
        return new CliOptions(help, cli, input, output);
    }

    private static String value(List<String> a, int i, String option) {
        if (i >= a.size() || a.get(i).startsWith("-")) {
            throw new IllegalArgumentException("Option " + option + " needs a file name.");
        }
        return a.get(i);
    }
}
