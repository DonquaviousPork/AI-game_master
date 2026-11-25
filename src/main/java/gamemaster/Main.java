package gamemaster;

import gamemaster.cli.CliApp;
import gamemaster.cli.CliOptions;
import gamemaster.gui.GuiApp;

/** Entry point of the jar: the GUI by default, the command-line game with -cli or -input. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
            return;
        }
        if (options.help) {
            System.out.println(CliOptions.USAGE);
        } else if (options.cli) {
            CliApp.run(options);
        } else {
            GuiApp.main(args);
        }
    }
}
