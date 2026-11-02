package gamemaster.game;

/**
 * State pattern: the current phase decides which actions are legal and what
 * phase comes next. The engine asks the phase instead of using if/else on a flag.
 */
public interface GamePhase {

    String name();

    boolean allows(ActionKind kind);

    /** The phase the game should be in given its current state; returns this if nothing changed. */
    GamePhase next(GameState state);
}
