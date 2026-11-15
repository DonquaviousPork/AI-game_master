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
import gamemaster.model.RogueClass;
import gamemaster.model.Weapon;
import gamemaster.model.WizardClass;
import gamemaster.rules.AttackResult;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import org.junit.jupiter.api.Test;

/** Checks that the engine, not the model, decides what happened and that the log says exactly what the dice did. */
class ReliabilityTest {

    private final CharacterFactory factory = new CharacterFactory();
    private final Character fighter = factory.createFromClass("Brenna", new FighterClass());
    private final Character rogue = factory.createFromClass("Rook", new RogueClass());
    private final Character wizard = factory.createFromClass("Mira", new WizardClass());

    private GameEngine engine(int... rolls) {
        Party party = new Party();
        party.addCharacter(fighter);
        party.addCharacter(rogue);
        party.addCharacter(wizard);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(rolls)));
        e.setScenario(new Scenario("Cave", "d"));
        return e;
    }

    private String run(GameEngine e, String tool, String args) {
        return ToolRegistry.defaultFor(e).execute(new ToolCall("id", tool, args));
    }

    private Weapon frostRay() {
        return wizard.getCharacterClass().attacks().stream().filter(w -> w.name().equals("Frost Ray")).findFirst().orElseThrow();
    }

    // ---- B: natural 1 versus an ordinary low total ----

    @Test
    void aTotalOfThreeFromANaturalFiveIsAnOrdinaryMissNotAFumble() {
        Monster target = new Monster("Wraith", 20, 12, Weapon.DAGGER);
        AttackResult r = new RuleEngine(new FixedDice(3)).rollAttack(wizard, frostRay(), target);
        assertFalse(r.hit());
        assertFalse(r.fumble());
        assertEquals(3, r.roll());
    }

    @Test
    void aNaturalOneIsAFumbleEvenWhenTheModifierMakesTheTotalHigher() {
        Monster target = new Monster("Wraith", 20, 5, Weapon.DAGGER);     // AC 5, the total (1 + modifier) would otherwise hit
        AttackResult r = new RuleEngine(new FixedDice(1)).rollAttack(wizard, frostRay(), target);
        assertTrue(r.fumble());
        assertFalse(r.hit());
    }

    @Test
    void theLogShowsTheNaturalDieSoATotalOfThreeFromANaturalOneIsExplained() {
        GameEngine e = engine(1);
        e.addMonster(new Monster("Thug", 20, 12, Weapon.DAGGER));
        String out = run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Thug\",\"attack\":\"Frost Ray\"}");
        assertTrue(out.contains("FUMBLE"), out);
        assertTrue(out.contains("d20 1 +"), out);     // "rolled 3 vs AC 12: d20 1 +2"
    }

    @Test
    void anOrdinaryMissDoesNotSayFumble() {
        GameEngine e = engine(5);
        e.addMonster(new Monster("Thug", 20, 12, Weapon.DAGGER));
        String out = run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Thug\",\"attack\":\"Frost Ray\"}");
        assertTrue(out.contains("misses"), out);
        assertFalse(out.contains("FUMBLE"), out);
    }

    // ---- A: the engine's health is the truth ----

    @Test
    void healingThenDamageLeavesTheHealthTheEngineComputed() {
        GameEngine e = engine(4, 15, 5);            // heal d6 4 (+2 WIS) = 6; monster hits, d8 5
        e.addMonster(new Monster("Nocturne", 30, 14, Weapon.LONGSWORD));
        wizard.applyDamage(7);                      // 13 / 20
        run(e, "heal_party", "{\"caster\":\"Mira\"}");
        assertEquals(19, wizard.getHealth());
        String hit = run(e, "attack", "{\"attacker\":\"Nocturne\",\"target\":\"Mira\"}");
        int damage = Integer.parseInt(hit.replaceAll(".*for (\\d+) damage.*", "$1"));
        assertEquals(19 - damage, wizard.getHealth());
        assertTrue(hit.contains("Mira now has " + wizard.getHealth() + "/20 HP"), hit);
    }

    @Test
    void theNextPromptCarriesTheLatestHealth() {
        GameEngine e = engine(4);
        e.addMonster(new Monster("Wolf", 20, 12, Weapon.DAGGER));
        wizard.applyDamage(7);
        var llm = new FakeLLM().thenTool("heal_party", "{\"caster\":\"Mira\"}").thenText("Warm light.").thenText("Next.");
        var orchestrator = new AgentOrchestrator(llm, e);
        orchestrator.takeTurn("heal us");
        orchestrator.takeTurn("look around");
        String prompt = llm.calls.get(llm.calls.size() - 1).get(1).content();
        assertTrue(prompt.contains("\"Mira\" (Wizard), HP 19/20"), prompt);
    }

    @Test
    void afterAHeroAttackTheModelIsToldWhichEnemiesStillMustAttack() {
        GameEngine e = engine(15, 4);
        e.addMonster(new Monster("Wolf", 30, 5, Weapon.DAGGER));
        e.addMonster(new Monster("Crow", 30, 5, Weapon.DAGGER));
        var llm = new FakeLLM().thenTool("attack", "{\"attacker\":\"Brenna\",\"target\":\"Wolf\",\"attack\":\"Slash\"}")
                .thenText("Brenna swings.");
        new AgentOrchestrator(llm, e).takeTurn("attack the wolf");
        boolean reminded = llm.calls.get(1).stream().anyMatch(m -> m.content() != null
                && m.content().contains("enemies that have not attacked this round yet: Wolf, Crow"));
        assertTrue(reminded);
    }

    // ---- C: spawning ----

    @Test
    void anInvalidSpawnIsRefusedAndChangesNothing() {
        GameEngine e = engine();
        String out = run(e, "spawn_monster", "{\"name\":\"Ghost\",\"health\":0,\"armor_class\":12,\"weapon\":\"dagger\"}");
        assertTrue(out.startsWith("Error:"), out);
        assertTrue(e.getState().getMonsters().isEmpty());
        assertEquals("Story", e.getState().getPhase().name());
        assertEquals(0, e.getState().getSpawnsThisTurn());
    }

    @Test
    void aValidSpawnSucceedsAndStartsCombat() {
        GameEngine e = engine();
        String out = run(e, "spawn_monster", "{\"name\":\"Ghost\",\"health\":12,\"armor_class\":12,\"weapon\":\"dagger\"}");
        assertTrue(out.contains("Ghost appears (HP 12, AC 12)"), out);
        assertEquals(1, e.getState().getMonsters().size());
        assertEquals("Combat", e.getState().getPhase().name());
    }

    @Test
    void aTooLargeHealthIsLoweredAndSaidSoInsteadOfBeingRefusedAgainAndAgain() {
        GameEngine e = engine();
        String out = run(e, "spawn_monster", "{\"name\":\"Lady\",\"health\":45,\"armor_class\":14,\"weapon\":\"longsword\"}");
        assertFalse(out.startsWith("Error:"), out);
        assertTrue(out.contains("lowered to the maximum of 30"), out);
        assertEquals(30, e.getState().getMonsters().get(0).getMaxHealth());
    }

    // ---- combat sequencing ----

    @Test
    void aDefeatedEnemyCannotAttack() {
        GameEngine e = engine();
        Monster rat = new Monster("Rat", 3, 5, Weapon.DAGGER);
        Monster wolf = new Monster("Wolf", 20, 5, Weapon.DAGGER);
        e.addMonster(rat);
        e.addMonster(wolf);
        rat.applyDamage(10);
        int before = fighter.getHealth();
        String out = run(e, "attack", "{\"attacker\":\"Rat\",\"target\":\"Brenna\"}");
        assertTrue(out.startsWith("Error:"), out);
        assertEquals(before, fighter.getHealth());
    }

    @Test
    void combatActionsAreRefusedOnceTheFightIsOver() {
        GameEngine e = engine(20, 8, 8);
        Monster rat = new Monster("Rat", 1, 5, Weapon.DAGGER);
        e.addMonster(rat);
        run(e, "attack", "{\"attacker\":\"Brenna\",\"target\":\"Rat\",\"attack\":\"Slash\"}");
        assertEquals("Story", e.getState().getPhase().name());
        String out = run(e, "attack", "{\"attacker\":\"Rook\",\"target\":\"Rat\",\"attack\":\"Quick Stab\"}");
        assertTrue(out.startsWith("Error:"), out);
    }
}
