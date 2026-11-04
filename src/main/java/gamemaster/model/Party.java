package gamemaster.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** The player's group of 1 to 4 characters. */
public class Party {

    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 4;

    private final List<Character> characters = new ArrayList<>();

    public void addCharacter(Character c) {
        Objects.requireNonNull(c, "character");
        if (characters.size() >= MAX_SIZE) {
            throw new IllegalStateException("The party is full (maximum " + MAX_SIZE + " characters).");
        }
        characters.add(c);
    }

    public void removeCharacter(Character c) {
        if (!characters.contains(c)) {
            throw new IllegalArgumentException("That character is not in the party.");
        }
        if (characters.size() <= MIN_SIZE) {
            throw new IllegalStateException("The party must keep at least " + MIN_SIZE + " character.");
        }
        characters.remove(c);
    }

    /** Swaps in a whole new set of members (used when a saved game is loaded). Validates before changing anything. */
    public void replaceMembers(List<Character> members) {
        Objects.requireNonNull(members, "members");
        if (members.size() < MIN_SIZE || members.size() > MAX_SIZE) {
            throw new IllegalArgumentException("A party needs " + MIN_SIZE + " to " + MAX_SIZE
                    + " characters, not " + members.size() + ".");
        }
        if (members.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("A party member must not be null.");
        }
        characters.clear();
        characters.addAll(members);
    }

    /** True when the party size is within the allowed range. */
    public boolean isValidSize() {
        return characters.size() >= MIN_SIZE && characters.size() <= MAX_SIZE;
    }

    public boolean isDefeated() {
        return !characters.isEmpty() && characters.stream().noneMatch(Character::isAlive);
    }

    public List<Character> getCharacters() {
        return Collections.unmodifiableList(characters);
    }

    public int size() { return characters.size(); }
}
