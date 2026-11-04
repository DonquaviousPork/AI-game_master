package gamemaster.ai;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.game.GameEngine;
import gamemaster.game.GameState;
import gamemaster.game.Scenario;
import gamemaster.llm.ToolCall;
import gamemaster.model.Character;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import gamemaster.model.Weapon;
import gamemaster.model.WizardClass;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    private final CharacterFactory factory = new CharacterFactory();
    private final Character fighter = factory.createFromClass("Brenna", new FighterClass()); // STR +2
    private final Character wizard = factory.createFromClass("Mira", new WizardClass());

    private GameEngine engine(int... rolls) {
        Party party = new Party();
        party.addCharacter(fighter);
        party.addCharacter(wizard);
        return new GameEngine(new GameState(party), new RuleEngine(new FixedDice(rolls)));
    }

    private GameEngine started(int... rolls) {
        GameEngine e = engine(rolls);
        e.setScenario(new Scenario("Cave", "Dark."));
        return e;
    }

    private String run(GameEngine e, String tool, String args) {
        return ToolRegistry.defaultFor(e).execute(new ToolCall("id", tool, args));
    }

    private static final String WOLF = "{\"name\":\"Wolf\",\"health\":10,\"armor_class\":12,\"weapon\":\"dagger\"}";

    // ---- registry ----

    @Test
    void offersFourToolsWithSchemas() {
        var specs = ToolRegistry.defaultFor(engine()).specs();
        assertEquals(java.util.List.of("roll_check", "attack", "spawn_monster", "end_story"),
                specs.stream().map(s -> s.name()).toList());
        specs.forEach(s -> assertEquals("object", s.parameters().get("type").getAsString()));
    }

    @Test
    void unknownToolAndBadJsonBecomeErrorMessagesNotExceptions() {
        GameEngine e = started();
        assertTrue(run(e, "launch_missiles", "{}").startsWith("Error: unknown tool"));
        assertTrue(run(e, "roll_check", "this is not json").startsWith("Error:"));
        assertTrue(run(e, "roll_check", "[1,2]").startsWith("Error:"));
    }

    // ---- roll_check ----

    @Test
    void rollCheckRunsTheEngineAndReportsTheOutcome() {
        GameEngine e = started(15); // +2 STR -> 17 vs 12
        String r = run(e, "roll_check", "{\"character\":\"Brenna\",\"stat\":\"STRENGTH\"}");
        assertTrue(r.contains("success"), r);
        assertEquals(1, e.getHistory().size());
    }

    @Test
    void rollCheckHonoursDifficultyAndIsCaseInsensitive() {
        GameEngine e = started(10); // 12 vs 15 fails
        String r = run(e, "roll_check", "{\"character\":\"brenna\",\"stat\":\"strength\",\"difficulty\":15}");
        assertTrue(r.contains("failure"), r);
    }

    @Test
    void rollCheckRejectsBadArguments() {
        GameEngine e = started(10, 10, 10, 10);
        assertTrue(run(e, "roll_check", "{\"character\":\"Nobody\",\"stat\":\"STRENGTH\"}").contains("No one named"));
        assertTrue(run(e, "roll_check", "{\"character\":\"Brenna\",\"stat\":\"LUCK\"}").startsWith("Error:"));
        assertTrue(run(e, "roll_check", "{\"character\":\"Brenna\",\"stat\":\"STRENGTH\",\"difficulty\":99}")
                .startsWith("Error:"));
        assertTrue(run(e, "roll_check", "{\"stat\":\"STRENGTH\"}").startsWith("Error:"));
        assertEquals(0, e.getHistory().size());
    }

    @Test
    void rollCheckBeforeTheGameStartsIsRefused() {
        assertTrue(run(engine(10), "roll_check", "{\"character\":\"Brenna\",\"stat\":\"STRENGTH\"}")
                .contains("not allowed"));
    }

    // ---- attack ----

    @Test
    void attackUsesTheEngineAndReportsRemainingHealth() {
        GameEngine e = started(15, 4); // +2 -> 17 vs AC 12 hits; d8 4 + 2 = 6
        run(e, "spawn_monster", WOLF);
        String r = run(e, "attack", "{\"attacker\":\"Brenna\",\"target\":\"Wolf\"}");
        assertTrue(r.contains("6 damage"), r);
        assertTrue(r.contains("Wolf now has 4/10 HP"), r);
        assertEquals(4, e.getState().getMonsters().get(0).getHealth());
    }

    @Test
    void aMonsterCanAttackAHeroThroughTheTool() {
        GameEngine e = started(18, 3); // dagger DEX +0 vs AC 11 hits; d4 3
        run(e, "spawn_monster", WOLF);
        run(e, "attack", "{\"attacker\":\"Wolf\",\"target\":\"Brenna\"}");
        assertEquals(fighter.getMaxHealth() - 3, fighter.getHealth());
    }

    @Test
    void attackOutsideCombatAndFriendlyFireAreRefused() {
        GameEngine e = started(10);
        assertTrue(run(e, "attack", "{\"attacker\":\"Brenna\",\"target\":\"Mira\"}").startsWith("Error:"));
        run(e, "spawn_monster", WOLF);
        String friendly = run(e, "attack", "{\"attacker\":\"Brenna\",\"target\":\"Mira\"}");
        assertTrue(friendly.contains("fight each other"), friendly);
        assertEquals(wizard.getMaxHealth(), wizard.getHealth());
    }

    @Test
    void whenTwoMonstersShareANameTheLivingOneIsTargeted() {
        GameEngine e = started(15, 8, 15, 1);
        run(e, "spawn_monster", WOLF);
        run(e, "spawn_monster", WOLF);
        e.getState().getMonsters().get(0).applyDamage(100); // first wolf is dead
        run(e, "attack", "{\"attacker\":\"Brenna\",\"target\":\"Wolf\"}");
        assertTrue(e.getState().getMonsters().get(1).getHealth() < 10);
    }

    // ---- spawn_monster ----

    @Test
    void spawnMonsterAddsAnEnemyAndStartsCombat() {
        GameEngine e = started();
        String r = run(e, "spawn_monster", WOLF);
        assertTrue(r.contains("Wolf appears"), r);
        assertEquals("Combat", e.getState().getPhase().name());
        assertEquals(Weapon.DAGGER, e.getState().getMonsters().get(0).getWeapon());
    }

    @Test
    void spawnMonsterEnforcesTheSameLimitsAsScenarioGeneration() {
        GameEngine e = started();
        assertTrue(run(e, "spawn_monster",
                "{\"name\":\"Titan\",\"health\":9999,\"armor_class\":12,\"weapon\":\"dagger\"}").startsWith("Error:"));
        assertTrue(run(e, "spawn_monster",
                "{\"name\":\"Imp\",\"health\":5,\"armor_class\":12,\"weapon\":\"laser\"}").startsWith("Error:"));
        assertTrue(e.getState().getMonsters().isEmpty());
    }

    @Test
    void spawnMonsterCapsLivingEnemiesAndRefusesOutsideTheGame() {
        assertTrue(run(engine(), "spawn_monster", WOLF).startsWith("Error:"));
        GameEngine e = started();
        for (int i = 0; i < 4; i++) {
            run(e, "spawn_monster", WOLF);
        }
        assertTrue(run(e, "spawn_monster", WOLF).contains("already 4"));
        assertEquals(4, e.getState().getMonsters().size());
    }

    // ---- end_story ----

    @Test
    void endStoryEndsTheGameAndLogsTheEnding() {
        GameEngine e = started();
        String r = run(e, "end_story", "{\"ending\":\"The grove blooms again.\"}");
        assertTrue(r.contains("Ended"), r);
        assertEquals("Ended", e.getState().getPhase().name());
        assertTrue(e.getState().getLog().get(0).contains("The grove blooms again."));
    }

    @Test
    void endStoryNeedsAnEndingAndARunningGame() {
        assertTrue(run(started(), "end_story", "{}").startsWith("Error:"));
        assertTrue(run(engine(), "end_story", "{\"ending\":\"x\"}").startsWith("Error:"));
    }

    @Test
    void aMonsterObjectInTheGameIsNotAffectedByRejectedSpawns() {
        GameEngine e = started();
        run(e, "spawn_monster", WOLF);
        Monster wolf = e.getState().getMonsters().get(0);
        run(e, "spawn_monster", "{\"name\":\"Bad\"}");
        assertEquals(1, e.getState().getMonsters().size());
        assertEquals(10, wolf.getHealth());
    }
}
