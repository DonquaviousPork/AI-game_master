package gamemaster.cli;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import gamemaster.app.GameFacade;
import gamemaster.llm.LLMConfig;
import gamemaster.llm.LLMException;
import gamemaster.llm.OpenAIClient;
import gamemaster.save.SaveRepository;
import gamemaster.save.SaveService;

/** Starts the command-line game: reads .env, connects to the model and runs a session. */
public class CliApp {

    public static void main(String[] args) throws IOException {
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
            return;
        }
        run(options);
    }

    /** Runs the command-line game with the given options (keyboard input unless -input is set). */
    public static void run(CliOptions options) throws IOException {
        GameFacade game;
        try {
            LLMConfig config = LLMConfig.load();
            game = new GameFacade(new OpenAIClient(config), new SaveService(new SaveRepository()));
        } catch (LLMException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }
        BufferedReader in;
        if (options.input != null) {
            Path file = Path.of(options.input);
            if (!Files.isReadable(file)) {
                System.err.println("Cannot read input file: " + options.input);
                System.exit(2);
                return;
            }
            in = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        } else {
            in = new BufferedReader(new InputStreamReader(System.in));
        }
        PrintStream out = System.out;
        if (options.output != null) {
            OutputStream both = new TeeStream(System.out, new FileOutputStream(options.output));
            out = new PrintStream(both, true, StandardCharsets.UTF_8);
        }
        CliSession session = new CliSession(game, in, out);
        session.setEchoInput(options.input != null);
        session.run();
        out.flush();
    }

    /** Writes everything to two streams. */
    private static final class TeeStream extends OutputStream {
        private final OutputStream a;
        private final OutputStream b;

        TeeStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void write(int c) throws IOException {
            a.write(c);
            b.write(c);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            a.write(buf, off, len);
            b.write(buf, off, len);
        }

        @Override
        public void flush() throws IOException {
            a.flush();
            b.flush();
        }

        @Override
        public void close() throws IOException {
            b.close();
        }
    }
}
