package gamemaster.ai;

import gamemaster.llm.LLMConfig;
import gamemaster.llm.OpenAIClient;

/**
 * Manual check that your .env works: makes one real model call (a few hundred
 * tokens) and prints the scenario it produced. Run it from the IDE.
 */
public class LiveCheck {
    public static void main(String[] args) {
        LLMConfig config = LLMConfig.load();
        System.out.println("Using " + config);
        GeneratedScenario s = new ScenarioGenerator(new OpenAIClient(config))
                .generate(args.length > 0 ? String.join(" ", args) : null);
        System.out.println(s.scenario().title() + "\n" + s.scenario().description());
        System.out.println("First: " + s.openingEvent().description());
        s.monsters().forEach(m -> System.out.println("Enemy: " + m));
    }
}
