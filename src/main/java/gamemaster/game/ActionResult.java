package gamemaster.game;

/**
 * What happened when an action ran, as one line of text for the log and the views.
 * {@code success} is true when the action was a roll that succeeded (a passed check or saving throw).
 */
public record ActionResult(String summary, boolean success) {

    public ActionResult(String summary) {
        this(summary, false);
    }
}
