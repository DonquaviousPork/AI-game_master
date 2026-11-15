package gamemaster.game;

import java.util.List;

/**
 * The setting of one adventure: what the story is about, and the one goal the heroes are working towards.
 * {@code type} says how that goal is decided and {@code target} names the enemy for DEFEAT_TARGET.
 * {@code milestones} are the acts before the final showdown; empty means the story has no fixed acts.
 */
public record Scenario(String title, String description, String objective, ObjectiveType type, String target,
                       List<Milestone> milestones) {

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
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
    }

    public Scenario(String title, String description, String objective, ObjectiveType type, String target) {
        this(title, description, objective, type, target, List.of());
    }

    public Scenario(String title, String description, String objective) {
        this(title, description, objective, ObjectiveType.NARRATIVE, "");
    }

    public Scenario(String title, String description) {
        this(title, description, "");
    }
}
