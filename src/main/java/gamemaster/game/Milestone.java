package gamemaster.game;

/**
 * One act of the story before the final showdown: a title, what happens in it (a discovery, a complication, a
 * higher stake) and how the engine knows it is finished.
 */
public record Milestone(String title, String description, MilestoneKind kind) {

    public Milestone {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("A milestone needs a title");
        }
        description = description == null ? "" : description;
        kind = kind == null ? MilestoneKind.CHALLENGE : kind;
    }
}
