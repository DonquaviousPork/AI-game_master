package gamemaster.rules;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import org.junit.jupiter.api.Test;

class AttackOddsTest {

    @Test
    void hitChanceCountsRollsThatReachTheArmorClassPlusAlwaysHittingOnA20() {
        // +2 to hit vs AC 13: rolls 11..20 hit = 10 of 20
        assertEquals(50, AttackOdds.of(8, 2, 0, 13).hitPercent());
        // +0 vs AC 12: rolls 12..20 = 9 of 20
        assertEquals(45, AttackOdds.of(8, 0, 0, 12).hitPercent());
    }

    @Test
    void aNaturalTwentyAlwaysHitsAndANaturalOneAlwaysMisses() {
        assertEquals(5, AttackOdds.of(8, 0, 0, 30).hitPercent());      // only the 20 hits
        assertEquals(95, AttackOdds.of(8, 10, 0, 1).hitPercent());     // everything but the 1 hits
    }

    @Test
    void damageRangeIsTheDiePlusTheModifierNeverBelowOne() {
        AttackOdds o = AttackOdds.of(8, 2, 0, 12);
        assertEquals(3, o.minDamage());
        assertEquals(10, o.maxDamage());
        AttackOdds weak = AttackOdds.of(4, -3, 0, 12);
        assertEquals(1, weak.minDamage());
        assertEquals(1, weak.maxDamage());
        assertEquals("1", weak.damageText());
        assertEquals("3-10", o.damageText());
    }

    @Test
    void averageDamageUsesTheSameFloorAsTheRules() {
        // d4 with -3: results 1,1,1,1 -> average 1; d4 +0: 2.5
        assertTrue(Math.abs(AttackOdds.of(4, -3, 0, 12).averageDamage() - 1.0) < 1e-9);
        assertTrue(Math.abs(AttackOdds.of(4, 0, 0, 12).averageDamage() - 2.5) < 1e-9);
    }

    @Test
    void ratingGrowsWithTheExpectedDamage() {
        assertEquals(AttackOdds.Rating.WEAK, AttackOdds.of(4, 0, 0, 20).rating());
        assertEquals(AttackOdds.Rating.VERY_STRONG, AttackOdds.of(12, 3, 0, 8).rating());
        AttackOdds.Rating[] order = AttackOdds.Rating.values();
        double a = AttackOdds.of(6, 1, 0, 14).expectedDamage();
        double b = AttackOdds.of(6, 1, 0, 10).expectedDamage();
        assertTrue(b > a);
        assertEquals(4, order.length);
    }

    @Test
    void anAttackAgainstALowerArmorClassIsNeverWorse() {
        for (int ac = 8; ac < 18; ac++) {
            assertTrue(AttackOdds.of(8, 1, 0, ac).hitPercent() >= AttackOdds.of(8, 1, 0, ac + 1).hitPercent());
        }
    }

    @Test
    void oddsForARealHeroMatchTheRulesEngine() {
        var hero = new CharacterFactory().createFromClass("Brenna", new FighterClass());
        var slash = hero.getCharacterClass().attacks().get(0);
        AttackOdds o = AttackOdds.of(hero, slash, 13);
        int mod = hero.getModifier(slash.stat());
        assertEquals(Math.max(1, 1 + mod), o.minDamage());
        // a roll of exactly (13 - mod - bonus) hits under the rules: check the boundary
        int needed = 13 - mod - slash.attackBonus();
        int expectedHits = 0;
        for (int r = 1; r <= 20; r++) {
            if (r == 20 || (r != 1 && r >= needed)) expectedHits++;
        }
        assertEquals(expectedHits * 5, o.hitPercent());
    }
}
