package gamemaster.save;

import gamemaster.ai.MemoryManager;
import gamemaster.game.GameEngine;
import gamemaster.game.GameSnapshot;
import java.util.List;
import java.util.Objects;

/** F14: saves the live game and story memory to a named slot, and loads them back. */
public class SaveService {

    private final SaveRepository repository;

    public SaveService(SaveRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void save(String slot, GameEngine engine, MemoryManager memory) {
        GameSnapshot snapshot = engine.getState().createSnapshot(memory.getSummary(), memory.getRecentTurns());
        repository.write(slot, snapshot);
    }

    /** Loads a slot. If the file is damaged nothing in the running game changes. */
    public void load(String slot, GameEngine engine, MemoryManager memory) {
        GameSnapshot snapshot = repository.read(slot);
        try {
            engine.load(snapshot);
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException e) {
            throw new SaveException("The save file \"" + slot + "\" is damaged: " + e.getMessage(), e);
        }
        memory.restore(snapshot.memorySummary(), snapshot.memoryRecent());
    }

    public List<String> listSaves() {
        return repository.list();
    }
}
