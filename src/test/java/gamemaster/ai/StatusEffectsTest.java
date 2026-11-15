package gamemaster.ai;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.game.GameEngine;
import gamemaster.game.GameSnapshot;
import gamemaster.game.GameState;
import gamemaster.game.Scenario;
import gamemaster.game.StatusKind;
import gamemaster.llm.ToolCall;
import gamemaster.model.Character;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Monster;
import gamemaster.model.Party;
import gamemaster.model.RogueClass;
import gamemaster.model.Weapon;
import gamemaster.model.WizardClass;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatusEffectsTest {

    private final CharacterFactory factory = new CharacterFactory();
    private final Character fighter = factory.createFromClass("Brenna", new FighterClass());
    private final Character rogue = factory.createFromClass("Rook", new RogueClass());
    private final Character wizard = factory.createFromClass("Mira", new WizardClass());
    private final Monster wraith = new Monster("Wraith", 30, 10, Weapon.DAGGER);

    private GameEngine engine(int... rolls) {
        Party party = new Party();
        party.addCharacter(fighter);
        party.addCharacter(rogue);
        party.addCharacter(wizard);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(rolls)));
        e.setScenario(new Scenario("Cave", "d"));
        e.addMonster(wraith);
        return e;
    }

    private String run(GameEngine e, String tool, String args) {
        return ToolRegistry.defaultFor(e).execute(new ToolCall("id", tool, args));
    }

    @Test
    void wizardFireBoltBurnsAndFrostRayChillsOnAHit() {
        GameEngine e = engine(15, 4, 15, 4);
        String a = run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Fire Bolt\"}");
        assertTrue(a.contains("burning"), a);
        assertEquals(StatusKind.DURATION, e.getState().statusTurns("Wraith", StatusKind.BURN));
        String b = run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Frost Ray\"}");
        assertTrue(b.contains("chilled"), b);
        assertEquals(StatusKind.DURATION, e.getState().statusTurns("Wraith", StatusKind.CHILL));
    }

    @Test
    void rogueSneakAttackWeakensAndFightersApplyNothing() {
        GameEngine e = engine(15, 4, 15, 4);
        run(e, "attack", "{\"attacker\":\"Rook\",\"target\":\"Wraith\",\"attack\":\"Sneak Attack\"}");
        assertEquals(StatusKind.DURATION, e.getState().statusTurns("Wraith", StatusKind.WEAKEN));
        run(e, "attack", "{\"attacker\":\"Brenna\",\"target\":\"Wraith\",\"attack\":\"Slash\"}");
        assertEquals(1, e.getState().statusesOf("Wraith").size());
    }

    @Test
    void aMissAppliesNoEffect() {
        GameEngine e = engine(2);      // 2 + modifiers is below AC 10
        run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Fire Bolt\"}");
        assertTrue(e.getState().statusesOf("Wraith").isEmpty());
    }

    @Test
    void anEffectIsRefreshedNotStacked() {
        GameEngine e = engine(15, 4, 15, 4);
        run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Fire Bolt\"}");
        e.tickStatuses();
        assertEquals(1, e.getState().statusTurns("Wraith", StatusKind.BURN));
        run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Fire Bolt\"}");
        assertEquals(StatusKind.DURATION, e.getState().statusTurns("Wraith", StatusKind.BURN));
    }

    @Test
    void burnDealsDamageEachTurnAndEndsAfterTwo() {
        GameEngine e = engine(15, 4);
        run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Fire Bolt\"}");
        int hp = wraith.getHealth();
        var first = e.tickStatuses();
        assertEquals(hp - 2, wraith.getHealth());
        assertTrue(first.get(0).contains("burn damage"), first.toString());
        var second = e.tickStatuses();
        assertEquals(hp - 4, wraith.getHealth());
        assertTrue(second.stream().anyMatch(l -> l.contains("no longer burning")), second.toString());
        assertTrue(e.getState().statusesOf("Wraith").isEmpty());
        assertTrue(e.tickStatuses().isEmpty());
        assertEquals(hp - 4, wraith.getHealth());
    }

    @Test
    void burnCanDefeatAnEnemyAndTheFightEnds() {
        Monster imp = new Monster("Imp", 6, 10, Weapon.DAGGER);
        Party party = new Party();
        party.addCharacter(wizard);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(15, 1)));
        e.setScenario(new Scenario("Cave", "d"));
        e.addMonster(imp);
        e.getState().applyStatus("Imp", StatusKind.BURN);
        imp.applyDamage(imp.getMaxHealth() - 2);
        assertTrue(imp.isAlive());
        e.tickStatuses();
        assertFalse(imp.isAlive());
        assertEquals("Story", e.getState().getPhase().name());
        assertTrue(e.getState().statusNames().isEmpty());
    }

    @Test
    void chillLowersTheAttackRollAndWeakenLowersArmorClass() {
        GameEngine e = engine(15, 4, 12);
        // Wraith AC 10 weakened to 8; a Fighter Slash roll of 8 now hits (8 + 0 >= 8)
        e.getState().applyStatus("Wraith", StatusKind.WEAKEN);
        String hit = run(e, "attack", "{\"attacker\":\"Brenna\",\"target\":\"Wraith\",\"attack\":\"Slash\"}");
        assertTrue(hit.contains("vs AC 8"), hit);
        // a chilled attacker loses 3 from the roll
        GameEngine f = engine(10);
        f.getState().applyStatus("Mira", StatusKind.CHILL);
        String chilled = run(f, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Staff Strike\"}");
        int total = Integer.parseInt(chilled.replaceAll(".*rolled (\\d+) vs.*", "$1"));
        GameEngine g = engine(10);
        String normal = run(g, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Staff Strike\"}");
        int plain = Integer.parseInt(normal.replaceAll(".*rolled (\\d+) vs.*", "$1"));
        assertEquals(plain - StatusKind.CHILL_PENALTY, total);
    }

    @Test
    void undoRemovesTheEffectTheAttackPutOn() {
        GameEngine e = engine(15, 4);
        run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Fire Bolt\"}");
        assertEquals(StatusKind.DURATION, e.getState().statusTurns("Wraith", StatusKind.BURN));
        assertTrue(e.undoLast());
        assertEquals(0, e.getState().statusTurns("Wraith", StatusKind.BURN));
    }

    @Test
    void anEnemyCanBurnAHeroOncePerTurn() {
        GameEngine e = engine(15, 1, 15, 1, 15, 1);
        e.getState().startTurn();
        String a = run(e, "attack", "{\"attacker\":\"Wraith\",\"target\":\"Brenna\",\"effect\":\"burn\"}");
        assertTrue(a.contains("burning"), a);
        assertEquals(StatusKind.DURATION, e.getState().statusTurns("Brenna", StatusKind.BURN));
        String b = run(e, "attack", "{\"attacker\":\"Wraith\",\"target\":\"Rook\",\"effect\":\"chill\"}");
        assertFalse(b.contains("chilled"), b);
        assertEquals(0, e.getState().statusTurns("Rook", StatusKind.CHILL));
        e.getState().startTurn();
        String c = run(e, "attack", "{\"attacker\":\"Wraith\",\"target\":\"Rook\",\"effect\":\"chill\"}");
        assertTrue(c.contains("chilled"), c);
    }

    @Test
    void anUnknownEffectNameIsRefused() {
        GameEngine e = engine(15, 1);
        assertTrue(run(e, "attack", "{\"attacker\":\"Wraith\",\"target\":\"Brenna\",\"effect\":\"doom\"}").startsWith("Error:"));
    }

    @Test
    void effectsAreSavedAndRestored() {
        GameEngine e = engine(15, 4);
        run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Fire Bolt\"}");
        e.getState().applyStatus("Brenna", StatusKind.CHILL);
        GameSnapshot snap = e.getState().createSnapshot("", java.util.List.of());
        GameEngine other = engine(1);
        other.load(snap);
        assertEquals(StatusKind.DURATION, other.getState().statusTurns("Wraith", StatusKind.BURN));
        assertEquals(StatusKind.DURATION, other.getState().statusTurns("Brenna", StatusKind.CHILL));
    }

    @Test
    void effectsAreShownToTheModelAndOnTheCards() {
        GameEngine e = engine(15, 4);
        run(e, "attack", "{\"attacker\":\"Mira\",\"target\":\"Wraith\",\"attack\":\"Fire Bolt\"}");
        assertTrue(StateDescriber.describe(e.getState()).contains("effects: burning (2 turns left)"));
    }

    @Test
    void aRefusedMoveAfterTheFightEndsIsNotShownInTheActivity() {
        Monster rat = new Monster("Rat", 1, 5, Weapon.DAGGER);
        Party party = new Party();
        party.addCharacter(fighter);
        party.addCharacter(rogue);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(20, 8, 8)));
        e.setScenario(new Scenario("Cave", "d"));
        e.addMonster(rat);
        var llm = new gamemaster.ai.FakeLLM()
                .thenTool("attack", "{\"attacker\":\"Brenna\",\"target\":\"Rat\",\"attack\":\"Slash\"}")
                .thenTool("attack", "{\"attacker\":\"Rook\",\"target\":\"Rat\",\"attack\":\"Quick Stab\"}")
                .thenText("The rat falls.");
        var result = new AgentOrchestrator(llm, e).takeTurn("attack");
        assertEquals(1, result.toolActivity().size(), result.toolActivity().toString());
    }

    @Test
    void whenTheLastHeroFallsTheModelIsToldTheAdventureIsLost() {
        Monster brute = new Monster("Brute", 30, 10, Weapon.LONGSWORD);
        Party party = new Party();
        party.addCharacter(wizard);
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(20, 8, 8)));
        e.setScenario(new Scenario("Cave", "d"));
        e.addMonster(brute);
        wizard.applyDamage(wizard.getMaxHealth() - 1);
        var llm = new gamemaster.ai.FakeLLM()
                .thenTool("attack", "{\"attacker\":\"Brute\",\"target\":\"Mira\"}")
                .thenText("Mira falls.");
        var result = new AgentOrchestrator(llm, e).takeTurn("attack");
        assertTrue(result.gameOver());
        boolean told = llm.calls.stream().flatMap(List::stream).anyMatch(m -> m.content() != null
                && m.content().contains("every hero has fallen and the adventure is lost"));
        assertTrue(told);
    }
}
