package gamemaster.game;

/** Before the adventure starts: needs a valid party and a scenario. */
public class SetupPhase implements GamePhase {
    @Override public String name() { return "Setup"; }
    @Override public boolean allows(ActionKind kind) { return false; }

    @Override
    public GamePhase next(GameState s) {
        if (s.getScenario() != null && s.getParty().isValidSize()) {
            return new StoryPhase();
        }
        return this;
    }
}
