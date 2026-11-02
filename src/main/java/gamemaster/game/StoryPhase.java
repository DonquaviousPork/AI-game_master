package gamemaster.game;

/** Exploration and narration: only ability checks make sense here. */
public class StoryPhase implements GamePhase {
    @Override public String name() { return "Story"; }
    @Override public boolean allows(ActionKind kind) { return kind == ActionKind.ABILITY_CHECK; }

    @Override
    public GamePhase next(GameState s) {
        if (s.isConcluded() || s.getParty().isDefeated()) {
            return new EndedPhase();
        }
        if (s.hasLivingMonsters()) {
            return new CombatPhase();
        }
        return this;
    }
}
