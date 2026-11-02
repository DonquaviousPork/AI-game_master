package gamemaster.game;

/** The game is over: everyone is defeated or the story has concluded. */
public class EndedPhase implements GamePhase {
    @Override public String name() { return "Ended"; }
    @Override public boolean allows(ActionKind kind) { return false; }
    @Override public GamePhase next(GameState s) { return this; }
}
