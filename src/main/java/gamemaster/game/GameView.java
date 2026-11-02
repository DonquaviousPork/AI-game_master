package gamemaster.game;

/** Observer: anything that displays the game (CLI, GUI) implements this. */
public interface GameView {
    void update(GameState state);
}
