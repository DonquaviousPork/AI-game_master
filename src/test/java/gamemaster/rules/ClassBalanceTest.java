package gamemaster.rules;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.model.CharacterClass;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.RogueClass;
import gamemaster.model.WizardClass;
import org.junit.jupiter.api.Test;

/** Every default class gives the player a real choice: at least one Strong and one Weak attack against a typical enemy. */
class ClassBalanceTest {

    private static final int TYPICAL_AC = 12;

    @Test
    void everyDefaultClassHasAStrongAndAWeakAttackAgainstATypicalEnemy() {
        for (CharacterClass cc : new CharacterClass[] {new FighterClass(), new RogueClass(), new WizardClass()}) {
            var hero = new CharacterFactory().createFromClass("Test", cc);
            boolean strong = false;
            boolean weak = false;
            for (var w : cc.attacks()) {
                var rating = AttackOdds.of(hero, w, TYPICAL_AC).rating();
                strong |= rating == AttackOdds.Rating.STRONG || rating == AttackOdds.Rating.VERY_STRONG;
                weak |= rating == AttackOdds.Rating.WEAK;
            }
            assertTrue(strong, cc.name() + " has no strong attack");
            assertTrue(weak, cc.name() + " has no weak attack");
        }
    }
}
