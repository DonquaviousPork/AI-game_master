package gamemaster.save;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import gamemaster.game.GameSnapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Memento caretaker: stores snapshots as JSON files, one per named slot, and gives them back.
 * It never looks inside a snapshot. Slot names are restricted to letters, digits, "-" and "_"
 * so a name can never point outside the save folder.
 */
public class SaveRepository {

    private static final Pattern SLOT = Pattern.compile("[A-Za-z0-9_-]{1,40}");
    private static final String EXTENSION = ".json";

    private final Path directory;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public SaveRepository(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    public SaveRepository() {
        this(Path.of("saves"));
    }

    public void write(String slot, GameSnapshot snapshot) {
        Path target = pathFor(slot);
        try {
            Files.createDirectories(directory);
            Path temp = Files.createTempFile(directory, "save-", ".tmp");
            try {
                Files.writeString(temp, gson.toJson(snapshot), StandardCharsets.UTF_8);
                try {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new SaveException("Could not write the save file: " + e.getMessage(), e);
        }
    }

    public GameSnapshot read(String slot) {
        Path file = pathFor(slot);
        if (!Files.isRegularFile(file)) {
            throw new SaveException("There is no saved game named \"" + slot + "\".");
        }
        GameSnapshot snapshot;
        try {
            snapshot = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), GameSnapshot.class);
        } catch (IOException e) {
            throw new SaveException("Could not read the save file: " + e.getMessage(), e);
        } catch (JsonParseException e) {
            throw new SaveException("The save file \"" + slot + "\" is damaged and cannot be read.", e);
        }
        if (snapshot == null) {
            throw new SaveException("The save file \"" + slot + "\" is empty.");
        }
        if (snapshot.version() != GameSnapshot.CURRENT_VERSION) {
            throw new SaveException("The save file \"" + slot + "\" has an unsupported version ("
                    + snapshot.version() + ").");
        }
        return snapshot;
    }

    /** Names of all saved games, sorted. */
    public List<String> list() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(EXTENSION))
                    .map(n -> n.substring(0, n.length() - EXTENSION.length()))
                    .filter(n -> SLOT.matcher(n).matches())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new SaveException("Could not list saved games: " + e.getMessage(), e);
        }
    }

    public void delete(String slot) {
        try {
            Files.deleteIfExists(pathFor(slot));
        } catch (IOException e) {
            throw new SaveException("Could not delete the save file: " + e.getMessage(), e);
        }
    }

    private Path pathFor(String slot) {
        if (slot == null || !SLOT.matcher(slot).matches()) {
            throw new SaveException("A save name may use letters, digits, - and _ only (1 to 40 characters).");
        }
        return directory.resolve(slot + EXTENSION);
    }
}
