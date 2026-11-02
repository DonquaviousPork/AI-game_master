package gamemaster.game;

/** Enemies are present: attacks, checks and fleeing are all allowed. */
public class CombatPhase implements GamePhase {
    @Override public String name() { return "Combat"; }
    @Override public boolean allows(ActionKind kind) { return true; }

    @Override
    public GamePhase next(GameState s) {
        if (s.isConcluded() || s.getParty().isDefeated()) {
            return new EndedPhase();
        }
        if (!s.hasLivingMonsters()) {
            return new StoryPhase();
        }
        return this;
    }
}
