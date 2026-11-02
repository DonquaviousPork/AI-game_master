package gamemaster.ai;

import static org.junit.jupiter.api.Assertions.*;

import gamemaster.game.GameEngine;
import gamemaster.game.GameState;
import gamemaster.llm.LLMException;
import gamemaster.model.CharacterFactory;
import gamemaster.model.FighterClass;
import gamemaster.model.Party;
import gamemaster.model.Weapon;
import gamemaster.rules.FixedDice;
import gamemaster.rules.RuleEngine;
import org.junit.jupiter.api.Test;

class ScenarioGeneratorTest {

    private static final String VALID = """
            {"title":"The Hollow Crypt","description":"A cold wind blows from an old crypt.",
             "opening_event":"A skeleton rises from a tomb.",
             "monsters":[{"name":"Skeleton","health":9,"armor_class":12,"weapon":"Dagger"}]}""";

    private static String json(String monster) {
        return "{\"title\":\"T\",\"description\":\"D\",\"opening_event\":\"E\",\"monsters\":[" + monster + "]}";
    }

    @Test
    void acceptsAValidScenario() {
        GeneratedScenario s = new ScenarioGenerator(new FakeLLM(VALID)).generate(null);
        assertEquals("The Hollow Crypt", s.scenario().title());
        assertEquals("A skeleton rises from a tomb.", s.openingEvent().description());
        assertEquals(1, s.monsters().size());
        assertEquals(9, s.monsters().get(0).getMaxHealth());
        assertEquals(Weapon.DAGGER, s.monsters().get(0).getWeapon());
    }

    @Test
    void toleratesCodeFencesAndChatterAroundTheJson() {
        GeneratedScenario s = new ScenarioGenerator(new FakeLLM("Sure! Here you go:\n```json\n" + VALID + "\n```"))
                .generate("crypt");
        assertEquals("The Hollow Crypt", s.scenario().title());
    }

    @Test
    void aScenarioWithNoMonstersIsFine() {
        GeneratedScenario s = new ScenarioGenerator(new FakeLLM(
                "{\"title\":\"T\",\"description\":\"D\",\"opening_event\":\"E\",\"monsters\":[]}")).generate(null);
        assertTrue(s.monsters().isEmpty());
    }

    @Test
    void invalidFirstReplyIsRetriedAndTheModelIsToldWhatWasWrong() {
        FakeLLM llm = new FakeLLM("I cannot do that", VALID);
        GeneratedScenario s = new ScenarioGenerator(llm).generate(null);
        assertEquals("The Hollow Crypt", s.scenario().title());
        assertEquals(2, llm.calls.size());
        var second = llm.calls.get(1);
        assertEquals("assistant", second.get(second.size() - 2).role());
        assertTrue(second.get(second.size() - 1).content().contains("rejected"));
    }

    @Test
    void givesUpAfterTwoInvalidReplies() {
        FakeLLM llm = new FakeLLM("nope", "still nope");
        LLMException e = assertThrows(LLMException.class, () -> new ScenarioGenerator(llm).generate(null));
        assertTrue(e.getMessage().contains("valid scenario"));
        assertEquals(2, llm.calls.size());
    }

    @Test
    void themeIsPassedToTheModelAndSystemPromptForbidsDecidingOutcomes() {
        FakeLLM llm = new FakeLLM(VALID);
        new ScenarioGenerator(llm).generate("haunted lighthouse");
        var messages = llm.calls.get(0);
        assertEquals("system", messages.get(0).role());
        assertTrue(messages.get(0).content().contains("Do not decide any outcomes"));
        assertTrue(messages.get(1).content().contains("haunted lighthouse"));
    }

    @Test
    void outOfRangeMonsterStatsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScenarioGenerator.parse(
                json("{\"name\":\"Titan\",\"health\":9999,\"armor_class\":12,\"weapon\":\"dagger\"}")));
        assertThrows(IllegalArgumentException.class, () -> ScenarioGenerator.parse(
                json("{\"name\":\"Ghost\",\"health\":5,\"armor_class\":30,\"weapon\":\"dagger\"}")));
        assertThrows(IllegalArgumentException.class, () -> ScenarioGenerator.parse(
                json("{\"name\":\"Ghost\",\"health\":0,\"armor_class\":10,\"weapon\":\"dagger\"}")));
    }

    @Test
    void unknownWeaponsAndMissingFieldsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScenarioGenerator.parse(
                json("{\"name\":\"Imp\",\"health\":5,\"armor_class\":10,\"weapon\":\"death ray\"}")));
        assertThrows(IllegalArgumentException.class, () -> ScenarioGenerator.parse(
                json("{\"name\":\"Imp\",\"health\":5,\"weapon\":\"dagger\"}")));
        assertThrows(IllegalArgumentException.class, () -> ScenarioGenerator.parse(
                "{\"title\":\"\",\"description\":\"D\",\"opening_event\":\"E\",\"monsters\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> ScenarioGenerator.parse(
                "{\"title\":\"T\",\"description\":\"D\",\"opening_event\":\"E\"}"));
    }

    @Test
    void tooManyMonstersAreRejected() {
        String m = "{\"name\":\"Rat\",\"health\":2,\"armor_class\":9,\"weapon\":\"dagger\"}";
        assertThrows(IllegalArgumentException.class,
                () -> ScenarioGenerator.parse(json(m + "," + m + "," + m + "," + m)));
    }

    @Test
    void applyingAScenarioStartsTheGameAndCombat() {
        Party party = new Party();
        party.addCharacter(new CharacterFactory().createFromClass("Brenna", new FighterClass()));
        GameEngine engine = new GameEngine(new GameState(party), new RuleEngine(new FixedDice()));
        new ScenarioGenerator(new FakeLLM(VALID)).generate(null).applyTo(engine);
        assertEquals("Combat", engine.getState().getPhase().name());
        assertEquals("The Hollow Crypt", engine.getState().getScenario().title());
        assertEquals("A skeleton rises from a tomb.", engine.getState().getCurrentEvent().description());
    }
}
