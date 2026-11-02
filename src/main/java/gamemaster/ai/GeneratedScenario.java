package gamemaster.ai;

import gamemaster.game.GameEngine;
import gamemaster.game.Scenario;
import gamemaster.game.StoryEvent;
import gamemaster.model.Monster;
import java.util.List;

/** A scenario the AI proposed and the validator accepted. It does nothing until applied to the engine. */
public record GeneratedScenario(Scenario scenario, StoryEvent openingEvent, List<Monster> monsters) {

    public GeneratedScenario {
        monsters = List.copyOf(monsters);
    }

    /** Hands the proposal to the engine, which owns the phase changes that follow. */
    public void applyTo(GameEngine engine) {
        engine.setScenario(scenario);
        engine.setCurrentEvent(openingEvent);
        for (Monster m : monsters) {
            engine.addMonster(m);
        }
    }
}
