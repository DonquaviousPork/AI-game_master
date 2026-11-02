package gamemaster.game;

/** The setting of one adventure: what the story is about and how it opens. */
public record Scenario(String title, String description) {
    public Scenario {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Scenario title must not be blank");
        }
        if (description == null) {
            description = "";
        }
    }
}
