package gamemaster.save;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.ai.FakeLLM;
import gamemaster.ai.MemoryManager;
import gamemaster.game.GameEngine;
import gamemaster.game.GameState;
import gamemaster.game.Scenario;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import gamemaster.model.Weapon;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class SaveServiceTest {

    private GameEngine game(String heroName) {
        Party party = new Party();
        party.addCharacter(new CharacterFactory().createFromClass(heroName, new FighterClass()));
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice()));
        e.setScenario(new Scenario("Grove", "A withering grove."));
        return e;
    }

    @Test
    void saveThenLoadRestoresGameAndMemory() throws Exception {
        SaveService service = new SaveService(new SaveRepository(Files.createTempDirectory("svc")));
        GameEngine original = game("Brenna");
        original.addMonster(new Monster("Wolf", 10, 12, Weapon.DAGGER));
        original.getState().getParty().getCharacters().get(0).applyDamage(4);
        MemoryManager memory = new MemoryManager(new FakeLLM());
        memory.record("I attack", "The wolf snarls.", List.of());
        service.save("run1", original, memory);

        GameEngine other = game("Someone");
        MemoryManager otherMemory = new MemoryManager(new FakeLLM());
        service.load("run1", other, otherMemory);

        var hero = other.getState().getParty().getCharacters().get(0);
        assertEquals("Brenna", hero.getName());
        assertEquals(hero.getMaxHealth() - 4, hero.getHealth());
        assertEquals("Wolf", other.getState().getMonsters().get(0).getName());
        assertEquals("Combat", other.getState().getPhase().name());
        assertEquals(memory.getRecentTurns(), otherMemory.getRecentTurns());
        assertEquals(List.of("run1"), service.listSaves());
    }

    @Test
    void loadingAMissingSaveChangesNothing() throws Exception {
        SaveService service = new SaveService(new SaveRepository(Files.createTempDirectory("svc")));
        GameEngine e = game("Brenna");
        assertThrows(SaveException.class, () -> service.load("nope", e, new MemoryManager(new FakeLLM())));
        assertEquals("Brenna", e.getState().getParty().getCharacters().get(0).getName());
    }

    @Test
    void aSaveWithValidJsonButBadContentIsReportedAndTheRunningGameIsKept() throws Exception {
        Path dir = Files.createTempDirectory("svc");
        Files.writeString(dir.resolve("bad.json"), "{\"version\":1,\"party\":[]}");
        SaveService service = new SaveService(new SaveRepository(dir));
        GameEngine e = game("Brenna");
        MemoryManager memory = new MemoryManager(new FakeLLM());
        memory.record("keep", "me", List.of());
        SaveException ex = assertThrows(SaveException.class, () -> service.load("bad", e, memory));
        assertTrue(ex.getMessage().contains("damaged"));
        assertEquals("Brenna", e.getState().getParty().getCharacters().get(0).getName());
        assertEquals(1, memory.getRecentTurns().size());
    }

    @Test
    void savingOverwritesAndKeepsUsableFiles() throws Exception {
        SaveService service = new SaveService(new SaveRepository(Files.createTempDirectory("svc")));
        GameEngine e = game("Brenna");
        MemoryManager memory = new MemoryManager(new FakeLLM());
        service.save("slot", e, memory);
        e.getState().getParty().getCharacters().get(0).applyDamage(5);
        service.save("slot", e, memory);
        GameEngine back = game("Other");
        service.load("slot", back, memory);
        assertEquals(back.getState().getParty().getCharacters().get(0).getMaxHealth() - 5,
                back.getState().getParty().getCharacters().get(0).getHealth());
    }
}
