package gamemaster.game;

/** The setting of one adventure: what the story is about, and the one goal the heroes are working towards. */
public record Scenario(String title, String description, String objective) {

    public Scenario {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Scenario title must not be blank");
        }
        if (description == null) {
            description = "";
        }
        if (objective == null) {
            objective = "";
        }
    }

    public Scenario(String title, String description) {
        this(title, description, "");
    }
}
