package gamemaster.game;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Command pattern invoker's record: the actions that ran, most recent first. */
public class ActionHistory {

    private final Deque<Action> done = new ArrayDeque<>();

    public void record(Action action) {
        done.push(action);
    }

    /** Undoes the most recent action, or returns empty if there is nothing to undo. */
    public Optional<Action> undoLast(GameState state) {
        if (done.isEmpty()) {
            return Optional.empty();
        }
        Action last = done.pop();
        last.undo(state);
        return Optional.of(last);
    }

    public int size() { return done.size(); }
    public boolean isEmpty() { return done.isEmpty(); }
    public void clear() { done.clear(); }
}
