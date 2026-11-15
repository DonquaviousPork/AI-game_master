package gamemaster.ai;

import gamemaster.game.GameState;
import gamemaster.model.Character;
import gamemaster.model.Monster;
import java.util.List;

/**
 * Turns the real game state into text for the model. The model is told the truth about
 * health and phase every turn, so it never has to remember or guess numbers.
 */
final class StateDescriber {

    static final int RECENT_LOG_LINES = 5;

    private StateDescriber() {}

    /** The story's acts: which are done, which is current, what it needs, and what each scene must add. */
    private static void describeActs(StringBuilder sb, GameState state) {
        var scenario = state.getScenario();
        var acts = scenario.milestones();
        sb.append("STORY ACTS (tracked by the game; an act ends only when the game sees what it needs, and you "
                + "cannot skip ahead; never tell the player the act numbers):\n");
        for (int i = 0; i < acts.size(); i++) {
            String mark = i < state.getMilestoneIndex() ? "[done]" : i == state.getMilestoneIndex() ? "[CURRENT]" : "[later]";
            sb.append("  ").append(mark).append(" Act ").append(i + 1).append(": ").append(acts.get(i).title())
                    .append(" - ").append(acts.get(i).description()).append('\n');
        }
        sb.append("  ").append(state.inFinale() ? "[CURRENT]" : "[later]").append(" Final act: the showdown with ")
                .append(scenario.target()).append(" (the climax)\n");
        var current = state.currentMilestone();
        if (current != null) {
            sb.append("THE CURRENT ACT IS \"").append(current.title()).append("\". Play out what it describes: ")
                    .append(current.description()).append(" ");
            if (current.kind() == gamemaster.game.MilestoneKind.CHALLENGE) {
                sb.append("It ends when a hero succeeds at a roll that moves it forward: put a real obstacle or "
                        + "puzzle in the scene and call roll_check (or saving_throw) for it.\n");
            } else {
                sb.append("It ends when the party defeats an enemy: bring in a minor foe now with spawn_monster "
                        + "(NOT ").append(scenario.target()).append(") and fight it.\n");
            }
            sb.append("Every scene must add something new (a clue, a reveal, a complication or higher stakes). "
                    + "Never write filler like \"you are getting closer\"; show a concrete event instead. "
                    + "The boss \"").append(scenario.target()).append("\" cannot appear until the final act.\n");
            if (state.getBeatTurns() >= 5) {
                sb.append("This act has already lasted ").append(state.getBeatTurns()).append(" turns: it is overdue, so "
                        + "force it to a head this turn.\n");
            }
        } else {
            boolean onField = state.getMonsters().stream().anyMatch(m -> m.getName().equalsIgnoreCase(scenario.target()));
            sb.append(onField ? "THE FINAL ACT: this is the climax. Make it tense and dramatic; the fight with \""
                    + scenario.target() + "\" decides the story.\n"
                    : "THE FINAL ACT BEGINS: bring in \"" + scenario.target() + "\" now with spawn_monster using exactly "
                    + "that name, with a dramatic entrance. It is the climax of the story.\n");
        }
    }

    static String describe(GameState state) {
        StringBuilder sb = new StringBuilder();
        if (state.getScenario() != null) {
            sb.append("Adventure: ").append(state.getScenario().title()).append(" - ")
                    .append(state.getScenario().description()).append('\n');
        }
        if (state.getScenario() != null && !state.getScenario().objective().isBlank()) {
            sb.append("OBJECTIVE (fixed, the only goal; never add another): ")
                    .append(state.getScenario().objective()).append('\n');
            switch (state.getScenario().type()) {
                case DEFEAT_TARGET -> sb.append("ENGINE-DECIDED: the objective is met when the enemy \"")
                        .append(state.getScenario().target()).append("\" is defeated. The game ends the story as a "
                                + "victory by itself at that moment; do not call end_story to claim it.\n");
                case DEFEAT_ALL -> sb.append("ENGINE-DECIDED: the objective is met when every enemy is defeated. "
                        + "The game ends the story as a victory by itself at that moment; do not call end_story "
                        + "to claim it.\n");
                case SURVIVE -> sb.append("ENGINE-DECIDED: the objective is met if at least one hero is still "
                        + "standing when the turn limit is reached; the game ends the story by itself then.\n");
                default -> {
                    if (state.isEasyWin()) {
                        sb.append("DEMO: the objective is achieved as soon as the enemy is defeated. When it falls, call "
                                + "end_story with objective_achieved true.\n");
                    }
                }
            }
            var scenario = state.getScenario();
            boolean targetOnField = state.getMonsters().stream().anyMatch(m -> m.getName().equalsIgnoreCase(scenario.target()));
            if (!scenario.milestones().isEmpty()) {
                describeActs(sb, state);
            } else if (scenario.type() == gamemaster.game.ObjectiveType.DEFEAT_TARGET && !targetOnField) {
                sb.append("The enemy \"").append(scenario.target()).append("\" has not appeared yet. Build the story "
                        + "first (scenes, clues, talk), then bring it in with spawn_monster using exactly that name "
                        + "by about the halfway turn, so the showdown happens and the adventure has a proper ending.\n");
            }
            int turn = state.getTurnsTaken();
            sb.append("Turn ").append(turn).append(" of ").append(state.getTurnLimit()).append(". ");
            if (turn >= state.getTurnLimit()) {
                sb.append("THIS IS THE FINAL TURN: bring the story to its end now by calling end_story "
                        + "(objective_achieved true only if the heroes achieved the objective).\n");
            } else if (state.turnsLeft() <= 3) {
                sb.append("Only ").append(state.turnsLeft()).append(" turns remain after this one: steer the scene "
                        + "toward the objective and its conclusion. Do not open new plot threads.\n");
            } else {
                sb.append("Keep every scene moving toward the objective.\n");
            }
        }
        if (state.getScenario() != null && state.getPhase().allows(gamemaster.game.ActionKind.ABILITY_CHECK)) {
            sb.append("Enemy budget: at most ").append(EncounterBudget.slotsLeft(state)).append(" more enemies (")
                    .append(EncounterBudget.hpLeft(state)).append(" total health left to spend, ")
                    .append(EncounterBudget.MAX_SPAWNS_PER_TURN).append(" new enemies per turn at most). ")
                    .append("The final enemy is exempt but its health is capped at ")
                    .append(EncounterBudget.bossCap(state)).append(".\n");
            boolean wizard = state.getParty().getCharacters().stream()
                    .anyMatch(c -> c.isAlive() && c.getCharacterClass().name().equalsIgnoreCase("Wizard"));
            if (wizard) {
                sb.append("Healing Light (heal_party, wizards only, heals every living hero): ")
                        .append(state.healsLeft()).append(" uses left.\n");
            }
        }
        sb.append("Phase: ").append(state.getPhase().name()).append('\n');
        sb.append("Party:\n");
        for (Character c : state.getParty().getCharacters()) {
            sb.append("  - name \"").append(c.getName()).append("\" (").append(c.getCharacterClass().name())
                    .append("), HP ").append(c.getHealth()).append('/').append(c.getMaxHealth())
                    .append(", AC ").append(c.getArmorClass()).append(c.isAlive() ? "" : " [DEFEATED]").append('\n');
            if (c.isAlive()) {
                sb.append("      attacks: ").append(Lookup.attackList(c.getCharacterClass().attacks())).append('\n');
            }
            appendStatuses(sb, state, c.getName());
        }
        List<Monster> monsters = state.getMonsters();
        if (monsters.isEmpty()) {
            sb.append("Enemies: none\n");
        } else {
            sb.append("Enemies:\n");
            for (Monster m : monsters) {
                sb.append("  - name \"").append(m.getName()).append("\", HP ").append(m.getHealth()).append('/')
                        .append(m.getMaxHealth()).append(", AC ").append(m.getArmorClass())
                        .append(m.isAlive() ? "" : " [DEFEATED]").append('\n');
                appendStatuses(sb, state, m.getName());
            }
        }
        if (state.getCurrentEvent() != null) {
            sb.append("Current scene: ").append(state.getCurrentEvent().description()).append('\n');
        }
        List<String> log = state.getLog();
        if (!log.isEmpty()) {
            sb.append("Recent game events:\n");
            for (String line : log.subList(Math.max(0, log.size() - RECENT_LOG_LINES), log.size())) {
                sb.append("  - ").append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /** One line listing the effects on a combatant, e.g. "effects: burning (2 turns left)". */
    private static void appendStatuses(StringBuilder sb, GameState state, String name) {
        var active = state.statusesOf(name);
        if (active.isEmpty()) {
            return;
        }
        sb.append("      effects: ");
        boolean first = true;
        for (var e : active.entrySet()) {
            sb.append(first ? "" : ", ").append(e.getKey().adjective()).append(" (").append(e.getValue())
                    .append(e.getValue() == 1 ? " turn left)" : " turns left)");
            first = false;
        }
        sb.append('\n');
    }
}
