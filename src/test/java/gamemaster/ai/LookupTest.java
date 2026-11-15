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
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import org.junit.jupiter.api.Test;

/** Name matching (the model sometimes adds a role to a name) and the four fixed attacks per hero. */
class LookupTest {

    private final CharacterFactory factory = new CharacterFactory();

    private GameEngine engine(int... rolls) {
        Party party = new Party();
        party.addCharacter(factory.createFromClass("David", new FighterClass()));
        party.addCharacter(factory.createFromClass("Yashna", new RogueClass()));
        party.addCharacter(factory.createFromClass("Noah", new WizardClass()));
        GameEngine e = new GameEngine(new GameState(party), new RuleEngine(new FixedDice(rolls)));
        e.setScenario(new Scenario("Cave", "Dark."));
        e.addMonster(new Monster("Emberwolf", 12, 13, Weapon.DAGGER));
        e.addMonster(new Monster("Ashen Brute", 18, 15, Weapon.LONGSWORD));
        return e;
    }

    private String run(GameEngine e, String args) {
        return ToolRegistry.defaultFor(e).execute(new ToolCall("id", "attack", args));
    }

    // ---- names ----

    @Test
    void exactNamesStillWorkIgnoringCase() {
        GameState s = engine().getState();
        assertEquals("David", Lookup.find(s, "david").getName());
        assertEquals("Ashen Brute", Lookup.find(s, "ASHEN BRUTE").getName());
    }

    @Test
    void aRoleAddedToTheNameStillFindsTheHero() {
        GameState s = engine().getState();
        assertEquals("David", Lookup.find(s, "David the Fighter").getName());
        assertEquals("David", Lookup.find(s, "David (Fighter)").getName());
        assertEquals("Yashna", Lookup.find(s, "Yashna the Rogue").getName());
    }

    @Test
    void anArticleOrPartOfAMonsterNameStillFindsTheMonster() {
        GameState s = engine().getState();
        assertEquals("Emberwolf", Lookup.find(s, "the Emberwolf").getName());
        assertEquals("Ashen Brute", Lookup.find(s, "Brute").getName());
        assertEquals("Ashen Brute", Lookup.find(s, "the Ashen Brute").getName());
    }

    @Test
    void anUnknownNameListsTheExactNamesToUse() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Lookup.find(engine().getState(), "Gandalf"));
        assertTrue(ex.getMessage().contains("exact names"), ex.getMessage());
        assertTrue(ex.getMessage().contains("David"), ex.getMessage());
    }

    @Test
    void aNameThatCouldMeanTwoDifferentCombatantsIsRejected() {
        GameEngine e = engine();
        e.addMonster(new Monster("Ashen Hound", 8, 12, Weapon.DAGGER));
        assertThrows(IllegalArgumentException.class, () -> Lookup.find(e.getState(), "Ashen"));
    }

    @Test
    void theAttackToolAcceptsANameWithARole() {
        GameEngine e = engine(15, 4);
        String out = run(e, "{\"attacker\":\"David the Fighter\",\"target\":\"Emberwolf\"}");
        assertTrue(out.contains("David hits Emberwolf"), out);
    }

    // ---- fixed attacks ----

    @Test
    void everyClassHasFourDistinctAttacks() {
        for (var cls : new gamemaster.model.CharacterClass[] {new FighterClass(), new RogueClass(), new WizardClass()}) {
            assertEquals(4, cls.attacks().size(), cls.name());
            assertEquals(4, (int) cls.attacks().stream().map(w -> w.name()).distinct().count(), cls.name());
        }
    }

    @Test
    void aNamedAttackIsUsedAndReportedByName() {
        GameEngine e = engine(15, 4);   // Shield Bash: STR +2 and +2 to hit
        String out = run(e, "{\"attacker\":\"David\",\"target\":\"Emberwolf\",\"attack\":\"Shield Bash\"}");
        assertTrue(out.contains("with Shield Bash"), out);
        assertTrue(out.contains("rolled 19 vs AC 13"), out);   // 15 + 2 (STR) + 2 (attack bonus)
    }

    @Test
    void attackNamesAreMatchedLoosely() {
        GameEngine e = engine(15, 4);
        assertTrue(run(e, "{\"attacker\":\"David\",\"target\":\"Emberwolf\",\"attack\":\"power strike\"}")
                .contains("with Power Strike"));
    }

    @Test
    void leavingTheAttackOutUsesTheClassesFirstAttack() {
        GameEngine e = engine(15, 4);
        assertTrue(run(e, "{\"attacker\":\"David\",\"target\":\"Emberwolf\"}").contains("with Slash"));
    }

    @Test
    void anAttackThatIsNotOnTheListIsATriedAndFailedAttemptNotAnError() {
        GameEngine e = engine();   // no dice scripted: a failed attempt must not roll anything
        String out = run(e, "{\"attacker\":\"David\",\"target\":\"Emberwolf\",\"attack\":\"Kill Everyone\"}");
        assertFalse(out.startsWith("Error:"), out);
        assertTrue(out.contains("David tries \"Kill Everyone\" but cannot use that move"), out);
        assertTrue(out.contains("the turn is wasted"), out);
        assertTrue(out.contains("can only use: Slash"), out);
        assertEquals(12, e.getState().getMonsters().get(0).getHealth());
        assertEquals(24, e.getState().getParty().getCharacters().get(0).getHealth());
    }

    @Test
    void aWizardCannotUseAFightersAttack() {
        GameEngine e = engine();
        String out = run(e, "{\"attacker\":\"Noah\",\"target\":\"Emberwolf\",\"attack\":\"Power Strike\"}");
        assertTrue(out.contains("Noah tries \"Power Strike\" but cannot use that move"), out);
        assertTrue(out.contains("Magic Missile"), out);
    }

    @Test
    void theGameStateTextListsEachLivingHerosAttacks() {
        String text = StateDescriber.describe(engine().getState());
        assertTrue(text.contains("name \"David\" (Fighter)"), text);
        assertTrue(text.contains("attacks: Slash (d8, +0 to hit)"), text);
        assertTrue(text.contains("attacks: Magic Missile"), text);
    }

    @Test
    void monstersIgnoreAnAttackNameAndUseTheirOwnWeapon() {
        GameEngine e = engine(15, 4);
        String out = run(e, "{\"attacker\":\"Emberwolf\",\"target\":\"David\",\"attack\":\"Fireball\"}");
        assertTrue(out.contains("with Dagger"), out);
    }

    @Test
    void aDefeatedEnemyIsNotFollowedByAHealthSentence() {
        GameEngine e = engine(15, 8, 15, 8);   // two hits of 10 damage each on the 12-HP wolf
        String first = run(e, "{\"attacker\":\"David\",\"target\":\"Emberwolf\"}");
        assertTrue(first.contains("now has 2/12 HP"), first);
        String second = run(e, "{\"attacker\":\"David\",\"target\":\"Emberwolf\"}");
        assertTrue(second.contains("Emberwolf is defeated."), second);
        assertFalse(second.contains("now has"), second);
    }
}
