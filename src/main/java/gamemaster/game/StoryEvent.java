package gamemaster.game;

/** One narrated beat of the story, shown to the player before they choose an action. */
public record StoryEvent(String description) {
    public StoryEvent {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Story event description must not be blank");
        }
    }
}
