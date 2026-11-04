package gamemaster.save;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.game.GameSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SaveRepositoryTest {

    private static GameSnapshot snapshot(String title) {
        return new GameSnapshot(GameSnapshot.CURRENT_VERSION, "now", title, "desc", "event", false,
                List.of(new GameSnapshot.CharacterData("Brenna", "Fighter",
                        java.util.Map.of("STRENGTH", 15), 12)),
                List.of(), List.of("line one"), "summary", List.of("turn"));
    }

    private static Path tempDir() throws Exception {
        return Files.createTempDirectory("saves-test");
    }

    @Test
    void writeThenReadReturnsTheSameSnapshot() throws Exception {
        SaveRepository repo = new SaveRepository(tempDir());
        repo.write("slot1", snapshot("Grove"));
        GameSnapshot back = repo.read("slot1");
        assertEquals("Grove", back.scenarioTitle());
        assertEquals("Brenna", back.party().get(0).name());
        assertEquals(12, back.party().get(0).health());
        assertEquals(List.of("line one"), back.log());
        assertEquals("summary", back.memorySummary());
        assertEquals(List.of("turn"), back.memoryRecent());
    }

    @Test
    void theSaveFolderIsCreatedOnDemandAndSavesAreOverwritten() throws Exception {
        Path dir = tempDir().resolve("nested").resolve("saves");
        SaveRepository repo = new SaveRepository(dir);
        repo.write("a", snapshot("First"));
        repo.write("a", snapshot("Second"));
        assertEquals("Second", repo.read("a").scenarioTitle());
        assertTrue(Files.isRegularFile(dir.resolve("a.json")));
    }

    @Test
    void noTemporaryFilesAreLeftBehind() throws Exception {
        Path dir = tempDir();
        new SaveRepository(dir).write("a", snapshot("x"));
        try (var files = Files.list(dir)) {
            assertEquals(List.of("a.json"), files.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void listReturnsSortedSlotNamesOnly() throws Exception {
        Path dir = tempDir();
        SaveRepository repo = new SaveRepository(dir);
        assertEquals(List.of(), repo.list());
        repo.write("zeta", snapshot("z"));
        repo.write("alpha", snapshot("a"));
        Files.writeString(dir.resolve("notes.txt"), "ignore me");
        assertEquals(List.of("alpha", "zeta"), repo.list());
        assertEquals(List.of(), new SaveRepository(dir.resolve("missing")).list());
    }

    @Test
    void readingAMissingSlotGivesAHelpfulError() throws Exception {
        SaveException e = assertThrows(SaveException.class, () -> new SaveRepository(tempDir()).read("nope"));
        assertTrue(e.getMessage().contains("nope"));
    }

    @Test
    void damagedAndEmptyFilesAreReportedNotCrashed() throws Exception {
        Path dir = tempDir();
        SaveRepository repo = new SaveRepository(dir);
        Files.writeString(dir.resolve("broken.json"), "{ this is not json");
        Files.writeString(dir.resolve("empty.json"), "");
        assertTrue(assertThrows(SaveException.class, () -> repo.read("broken")).getMessage().contains("damaged"));
        assertThrows(SaveException.class, () -> repo.read("empty"));
    }

    @Test
    void anUnsupportedVersionIsRefused() throws Exception {
        Path dir = tempDir();
        Files.writeString(dir.resolve("old.json"), "{\"version\":99}");
        assertTrue(assertThrows(SaveException.class, () -> new SaveRepository(dir).read("old"))
                .getMessage().contains("version"));
    }

    @Test
    void slotNamesCannotEscapeTheSaveFolder() throws Exception {
        SaveRepository repo = new SaveRepository(tempDir());
        for (String bad : new String[] {"../evil", "a/b", "a\\b", "", " ", "a b", "x".repeat(41), "..", null}) {
            assertThrows(SaveException.class, () -> repo.write(bad, snapshot("x")), "name: " + bad);
            assertThrows(SaveException.class, () -> repo.read(bad), "name: " + bad);
        }
        repo.write("ok_name-1", snapshot("x"));
    }

    @Test
    void deleteRemovesTheFileAndIsHarmlessWhenMissing() throws Exception {
        SaveRepository repo = new SaveRepository(tempDir());
        repo.write("gone", snapshot("x"));
        repo.delete("gone");
        assertEquals(List.of(), repo.list());
        repo.delete("gone");
    }
}
