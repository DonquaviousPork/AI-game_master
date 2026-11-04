package gamemaster.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import gamemaster.app.GameFacade;
import gamemaster.llm.LLMConfig;
import gamemaster.llm.LLMException;
import gamemaster.llm.OpenAIClient;
import gamemaster.save.SaveRepository;
import gamemaster.save.SaveService;

/** Starts the command-line game: reads .env, connects to the model and runs a session. */
public class CliApp {

    public static void main(String[] args) throws IOException {
        GameFacade game;
        try {
            LLMConfig config = LLMConfig.load();
            game = new GameFacade(new OpenAIClient(config), new SaveService(new SaveRepository()));
        } catch (LLMException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }
        new CliSession(game, new BufferedReader(new InputStreamReader(System.in)), System.out).run();
    }
}
