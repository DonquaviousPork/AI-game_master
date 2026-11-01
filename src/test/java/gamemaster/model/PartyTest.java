package gamemaster.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PartyTest {

    private final CharacterFactory factory = new CharacterFactory();

    private Character member(String name) {
        return factory.createFromClass(name, new RogueClass());
    }

    @Test
    void emptyPartyIsNotValidUntilSomeoneJoins() {
        Party party = new Party();
        assertFalse(party.isValidSize());
        party.addCharacter(member("A"));
        assertTrue(party.isValidSize());
    }

    @Test
    void fifthCharacterIsRejected() {
        Party party = new Party();
        for (String n : new String[] {"A", "B", "C", "D"}) {
            party.addCharacter(member(n));
        }
        assertThrows(IllegalStateException.class, () -> party.addCharacter(member("E")));
        assertEquals(4, party.size());
    }

    @Test
    void lastCharacterCannotBeRemoved() {
        Party party = new Party();
        Character only = member("A");
        party.addCharacter(only);
        assertThrows(IllegalStateException.class, () -> party.removeCharacter(only));
        assertEquals(1, party.size());
    }

    @Test
    void removingAnotherMemberWorks() {
        Party party = new Party();
        Character a = member("A");
        Character b = member("B");
        party.addCharacter(a);
        party.addCharacter(b);
        party.removeCharacter(a);
        assertEquals(1, party.size());
    }

    @Test
    void partyIsDefeatedOnlyWhenEveryoneIsDown() {
        Party party = new Party();
        Character a = member("A");
        Character b = member("B");
        party.addCharacter(a);
        party.addCharacter(b);
        a.applyDamage(1000);
        assertFalse(party.isDefeated());
        b.applyDamage(1000);
        assertTrue(party.isDefeated());
    }
}
