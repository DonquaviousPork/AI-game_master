package gamemaster.gui;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.ai.FakeLLM;
import gamemaster.app.GameFacade;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import gamemaster.save.SaveRepository;
import gamemaster.save.SaveService;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattlePlanTest {

    private static final List<BattlePlan.Pick> PICKS = List.of(
            new BattlePlan.Pick("Matthew", "Slash", "Forest Sprite"),
            new BattlePlan.Pick("David", "Quick Stab", "Forest Sprite"));

    @Test
    void theInstructionSpellsOutEveryChoiceWithExactNames() {
        String text = BattlePlan.instruction(PICKS);
        assertTrue(text.contains("Matthew uses Slash on Forest Sprite."), text);
        assertTrue(text.contains("David uses Quick Stab on Forest Sprite."), text);
        assertTrue(text.contains("Then let each living enemy act."), text);
    }

    @Test
    void theStoryPaneShowsAShortForm() {
        assertEquals("Matthew: Slash on Forest Sprite; David: Quick Stab on Forest Sprite", BattlePlan.display(PICKS));
    }

    @Test
    void theBattleMenuListsLivingHeroesAndEnemiesAndReturnsTheChoices() throws Exception {
        String scenario = "{\"title\":\"T\",\"description\":\"D\",\"opening_event\":\"E\","
                + "\"monsters\":[{\"name\":\"Wolf\",\"health\":9,\"armor_class\":12,\"weapon\":\"dagger\"}]}";
        GameFacade g = new GameFacade(new FakeLLM(scenario),
                new SaveService(new SaveRepository(Files.createTempDirectory("battle"))), new RuleEngine(new FixedDice()));
        g.addCharacter("Brenna", "Fighter");
        g.addCharacter("Mira", "Wizard");
        g.startAdventure("x");
        BattlePanel panel = new BattlePanel(p -> {});
        panel.refresh(GameViewModel.from(g.getState()));
        List<BattlePlan.Pick> picks = panel.picks();
        assertEquals(2, picks.size());
        assertEquals("Brenna", picks.get(0).hero());
        assertEquals("Slash", picks.get(0).attack());      // the class's first attack is preselected
        assertEquals("Wolf", picks.get(0).target());
        assertEquals("Magic Missile", picks.get(1).attack());
    }
}
