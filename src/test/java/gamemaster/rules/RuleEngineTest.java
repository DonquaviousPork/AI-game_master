package gamemaster.rules;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.model.Character;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.RogueClass;
import gamemaster.model.Stat;
import gamemaster.model.Weapon;
import gamemaster.model.WizardClass;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private final CharacterFactory factory = new CharacterFactory();

    // Fighter: STR 15 (+2), DEX 12 (+1), AC 11
    private Character fighter() { return factory.createFromClass("Brenna", new FighterClass()); }
    // Wizard: STR 8 (-1), DEX 12 (+1), INT 15 (+2), WIS 14 (+2), AC 11
    private Character wizard() { return factory.createFromClass("Mira", new WizardClass()); }
    // Rogue: DEX 15 (+2)
    private Character rogue() { return factory.createFromClass("Kest", new RogueClass()); }

    @Test
    void abilityCheckSucceedsWhenTotalMeetsDifficulty() {
        RuleEngine engine = new RuleEngine(new FixedDice(10));
        CheckResult r = engine.rollAbilityCheck(fighter(), Stat.STRENGTH, 12);
        assertEquals(10, r.roll());
        assertEquals(2, r.modifier());
        assertEquals(12, r.total());
        assertTrue(r.success());
    }

    @Test
    void abilityCheckFailsWhenTotalIsBelowDifficulty() {
        RuleEngine engine = new RuleEngine(new FixedDice(9));
        assertFalse(engine.rollAbilityCheck(fighter(), Stat.STRENGTH, 12).success());
    }

    @Test
    void abilityCheckUsesDefaultDifficultyWhenNoneGiven() {
        RuleEngine engine = new RuleEngine(new FixedDice(10));
        CheckResult r = engine.rollAbilityCheck(fighter(), Stat.STRENGTH);
        assertEquals(RuleEngine.DEFAULT_DIFFICULTY, r.difficulty());
    }

    @Test
    void attackHitsWhenTotalMeetsArmorClass() {
        RuleEngine engine = new RuleEngine(new FixedDice(9));
        AttackResult r = engine.rollAttack(fighter(), Weapon.LONGSWORD, wizard());
        assertEquals(11, r.total());          // 9 + STR modifier 2
        assertEquals(11, r.targetArmorClass());
        assertTrue(r.hit());
    }

    @Test
    void attackMissesWhenTotalIsBelowArmorClass() {
        RuleEngine engine = new RuleEngine(new FixedDice(8));
        assertFalse(engine.rollAttack(fighter(), Weapon.LONGSWORD, wizard()).hit());
    }

    @Test
    void attackUsesTheWeaponsStatNotAlwaysStrength() {
        RuleEngine engine = new RuleEngine(new FixedDice(10));
        AttackResult r = engine.rollAttack(rogue(), Weapon.DAGGER, fighter());
        assertEquals(2, r.modifier());        // Rogue DEX 15 -> +2
    }

    @Test
    void attackingADefeatedTargetIsRejectedBeforeRolling() {
        FixedDice dice = new FixedDice(15);
        RuleEngine engine = new RuleEngine(dice);
        Character target = wizard();
        target.applyDamage(1000);
        assertThrows(IllegalStateException.class,
                () -> engine.rollAttack(fighter(), Weapon.LONGSWORD, target));
        assertEquals(1, dice.remaining());    // the die was never rolled
    }

    @Test
    void aDefeatedCharacterCannotAttack() {
        RuleEngine engine = new RuleEngine(new FixedDice(15));
        Character attacker = fighter();
        attacker.applyDamage(1000);
        assertThrows(IllegalStateException.class,
                () -> engine.rollAttack(attacker, Weapon.LONGSWORD, wizard()));
    }

    @Test
    void damageIsDieRollPlusModifier() {
        RuleEngine engine = new RuleEngine(new FixedDice(5));
        assertEquals(7, engine.rollDamage(fighter(), Weapon.LONGSWORD));   // 5 + 2
    }

    @Test
    void damageIsNeverBelowOneOnAHit() {
        RuleEngine engine = new RuleEngine(new FixedDice(1));
        // Wizard STR 8 gives -1, so 1 + (-1) = 0 -> raised to 1
        assertEquals(1, engine.rollDamage(wizard(), Weapon.QUARTERSTAFF));
    }

    @Test
    void damageDieMatchesTheWeapon() {
        // A roll of 8 is legal on a d8 longsword but impossible on a d4 dagger.
        assertDoesNotThrow(() -> new RuleEngine(new FixedDice(8)).rollDamage(fighter(), Weapon.LONGSWORD));
        assertThrows(IllegalStateException.class,
                () -> new RuleEngine(new FixedDice(8)).rollDamage(fighter(), Weapon.DAGGER));
    }

    @Test
    void savingThrowSucceedsWhenTotalMeetsDifficulty() {
        RuleEngine engine = new RuleEngine(new FixedDice(10));
        SaveResult r = engine.rollSavingThrow(wizard(), Stat.WISDOM, 12);
        assertEquals(12, r.total());          // 10 + WIS modifier 2
        assertTrue(r.success());
    }

    @Test
    void savingThrowFailsWhenTotalIsBelowDifficulty() {
        RuleEngine engine = new RuleEngine(new FixedDice(9));
        assertFalse(engine.rollSavingThrow(wizard(), Stat.WISDOM, 12).success());
    }

    @Test
    void savingThrowUsesDefaultDifficultyWhenNoneGiven() {
        RuleEngine engine = new RuleEngine(new FixedDice(10));
        assertEquals(RuleEngine.DEFAULT_DIFFICULTY, engine.rollSavingThrow(wizard(), Stat.WISDOM).difficulty());
    }

    @Test
    void aDefeatedCharacterCannotMakeASavingThrow() {
        RuleEngine engine = new RuleEngine(new FixedDice(10));
        Character target = wizard();
        target.applyDamage(1000);
        assertThrows(IllegalStateException.class, () -> engine.rollSavingThrow(target, Stat.WISDOM, 12));
    }

    @Test
    void nonPositiveDifficultyIsRejected() {
        RuleEngine engine = new RuleEngine(new FixedDice(10, 10));
        assertThrows(IllegalArgumentException.class, () -> engine.rollAbilityCheck(fighter(), Stat.STRENGTH, 0));
        assertThrows(IllegalArgumentException.class, () -> engine.rollSavingThrow(wizard(), Stat.WISDOM, -3));
    }

    @Test
    void randomDiceStaysWithinRange() {
        RandomDice dice = new RandomDice(new java.util.Random(42));
        for (int i = 0; i < 1000; i++) {
            int v = dice.roll(20);
            assertTrue(v >= 1 && v <= 20, "rolled " + v);
        }
        assertThrows(IllegalArgumentException.class, () -> dice.roll(0));
    }
}
