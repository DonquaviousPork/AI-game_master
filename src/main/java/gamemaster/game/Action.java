package gamemaster.game;

import gamemaster.rules.RuleEngine;

/**
 * Command pattern: each player move is an object that can run itself and,
 * where it changed state, reverse that change. Dice rolls are never replayed.
 */
public interface Action {

    ActionKind kind();

    ActionResult execute(GameState state, RuleEngine rules);

    /** Reverses the state change made by execute(); a no-op for actions that changed nothing. */
    void undo(GameState state);
}
