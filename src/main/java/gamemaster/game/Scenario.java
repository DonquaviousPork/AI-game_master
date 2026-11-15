package gamemaster.game;

/**
 * The setting of one adventure: what the story is about, and the one goal the heroes are working towards.
 * {@code type} says how that goal is decided and {@code target} names the enemy for DEFEAT_TARGET.
 */
public record Scenario(String title, String description, String objective, ObjectiveType type, String target) {

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
        if (type == null) {
            type = ObjectiveType.NARRATIVE;
        }
        if (target == null) {
            target = "";
        }
    }

    public Scenario(String title, String description, String objective) {
        this(title, description, objective, ObjectiveType.NARRATIVE, "");
    }

    public Scenario(String title, String description) {
        this(title, description, "");
    }
}
