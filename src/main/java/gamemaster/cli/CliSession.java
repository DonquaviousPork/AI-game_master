package gamemaster.cli;

import gamemaster.ai.GeneratedScenario;
import gamemaster.ai.TurnResult;
import gamemaster.app.GameFacade;
import gamemaster.game.GameState;
import gamemaster.model.Character;
import gamemaster.model.CharacterFactory;
import gamemaster.model.Monster;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Objects;

/**
 * The command-line game. It reads lines, runs slash commands (/add, /start, /save ...) and sends
 * any other text to the AI game master as the party's action. It talks only to GameFacade.
 * Input and output are passed in so a whole session can be run and checked in tests.
 */
public class CliSession {

    static final int WIDTH = 78;

    private final GameFacade game;
    private final BufferedReader in;
    private final PrintStream out;

    public CliSession(GameFacade game, BufferedReader in, PrintStream out) {
        this.game = Objects.requireNonNull(game, "game");
        this.in = Objects.requireNonNull(in, "in");
        this.out = Objects.requireNonNull(out, "out");
        game.onRateLimitWait(this::announceWait);
    }

    private int lastAnnounced = -1;

    /** Prints a short note when a rate-limited request is waiting, about every 10 seconds. */
    private void announceWait(int secondsLeft) {
        if (secondsLeft == 0) {
            lastAnnounced = -1;
            out.println("Retrying now...");
        } else if (lastAnnounced < 0 || lastAnnounced - secondsLeft >= 10) {
            lastAnnounced = secondsLeft;
            out.println("Rate limit reached - retrying automatically in " + secondsLeft + " s...");
        }
    }

    public void run() throws IOException {
        out.println("=== AI Game Master ===");
        out.println("Build your party, then start an adventure. Type /help for commands.");
        while (true) {
            out.print(game.isStarted() ? "> " : "setup> ");
            out.flush();
            String line = in.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                if (!handle(line)) {
                    break;
                }
            } catch (RuntimeException e) {
                out.println("Error: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
            }
        }
        out.println("Goodbye.");
    }

    /** Returns false when the player quits. */
    private boolean handle(String line) throws IOException {
        if (line.startsWith("/")) {
            return command(line.substring(1).trim());
        }
        if (!game.isStarted()) {
            out.println("Add characters with /add, then use /start. Type /help for all commands.");
            return true;
        }
        if (game.isOver()) {
            out.println("The story has ended. Use /load to continue a saved game, or /quit.");
            return true;
        }
        out.println("...");
        printTurn(game.submitAction(line));
        return true;
    }

    private boolean command(String text) throws IOException {
        String[] parts = text.split("\\s+", 2);
        String name = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";
        switch (name) {
            case "help" -> printHelp();
            case "add" -> add(arg, false);
            case "custom" -> add(arg, true);
            case "remove" -> {
                game.removeCharacter(arg);
                out.println("Removed " + arg + ".");
            }
            case "party" -> printParty();
            case "start" -> start(arg);
            case "status" -> printStatus();
            case "undo" -> out.println(game.undo()
                    ? "Last action undone (damage and fleeing are reversed; dice are not re-rolled)."
                    : "Nothing to undo.");
            case "save" -> {
                requireArg(arg, "Usage: /save <name>");
                game.save(arg);
                out.println("Saved as \"" + arg + "\".");
            }
            case "load" -> {
                requireArg(arg, "Usage: /load <name>");
                game.load(arg);
                out.println("Loaded \"" + arg + "\".");
                printStatus();
            }
            case "saves" -> out.println(game.listSaves().isEmpty()
                    ? "No saved games." : "Saved games: " + String.join(", ", game.listSaves()));
            case "demo" -> {
                game.setDemoMode(!"off".equalsIgnoreCase(arg));
                out.println(game.isDemoMode()
                        ? "Demo mode on: the next adventure has " + game.getTurnLimit() + " turns and one weak enemy."
                        : "Demo mode off: normal adventures of " + game.getTurnLimit() + " turns.");
            }
            case "turns" -> {
                requireArg(arg, "Usage: /turns <1-" + GameState.TURN_LIMIT + ">  (applies to the next adventure)");
                game.setTurnLimit(Integer.parseInt(arg.trim()));
                out.println("The next adventure will last " + game.getTurnLimit() + " turns.");
            }
            case "usage" -> out.println(game.usageSummary() + ".");
            case "new" -> {
                game.returnToMenu();
                out.println("Adventure abandoned (use /save first to keep one). Add characters with /add, then /start.");
            }
            case "quit", "exit" -> {
                return false;
            }
            default -> out.println("Unknown command /" + name + ". Type /help.");
        }
        return true;
    }

    private void add(String arg, boolean custom) throws IOException {
        String[] parts = arg.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            out.println(custom ? "Usage: /custom <Fighter|Wizard|Rogue> <name>"
                    : "Usage: /add <Fighter|Wizard|Rogue> <name>");
            return;
        }
        Character c;
        if (custom) {
            out.println("Spend exactly " + CharacterFactory.POINT_POOL + " points on top of a base of "
                    + CharacterFactory.BASE_STAT + " (maximum score " + CharacterFactory.MAX_STAT + ").");
            out.println("Example: STR 5 DEX 3 CON 4 INT 3 WIS 3 CHA 2");
            out.print("points> ");
            out.flush();
            String line = in.readLine();
            if (line == null) {
                return;
            }
            c = game.addCustomCharacter(parts[1], parts[0], StatInput.parse(line));
        } else {
            c = game.addCharacter(parts[1], parts[0]);
        }
        out.println("Added " + c + ".");
        out.println(stats(c));
    }

    private void start(String theme) {
        out.println("The game master is preparing your adventure...");
        GeneratedScenario s = game.startAdventure(theme);
        out.println();
        out.println("*** " + s.scenario().title() + " ***");
        out.println(wrap(s.scenario().description()));
        out.println();
        out.println("OBJECTIVE: " + s.scenario().objective());
        out.println("You have " + game.getState().getTurnLimit() + " turns. Reach the objective before they run out to win.");
        out.println();
        out.println(wrap(s.openingEvent().description()));
        for (Monster m : s.monsters()) {
            out.println("  Enemy: " + m);
        }
        out.println();
        out.println("What do you do?");
    }

    private void printTurn(TurnResult r) {
        out.println(wrap(r.narration()));
        for (String line : r.toolActivity()) {
            out.println("  [rules] " + line);
        }
        out.println(statusLine());
        if (r.gameOver()) {
            out.println("*** The story has ended. " + game.getState().endingText() + " ***");
        }
    }

    private void printStatus() {
        GameState s = game.getState();
        if (s.getScenario() != null) {
            out.println("Adventure: " + s.getScenario().title() + "   Phase: " + s.getPhase().name()
                    + "   Turn " + Math.min(s.getTurnsTaken(), s.getTurnLimit()) + "/" + s.getTurnLimit());
            if (!s.getScenario().objective().isBlank()) {
                out.println("Objective: " + s.getScenario().objective());
            }
        }
        if (s.getCurrentEvent() != null) {
            out.println(wrap(s.getCurrentEvent().description()));
        }
        printParty();
        for (Monster m : s.getMonsters()) {
            out.println("  Enemy: " + m + (m.isAlive() ? "" : " [defeated]"));
        }
    }

    private void printParty() {
        if (game.getParty().isEmpty()) {
            out.println("Your party is empty.");
            return;
        }
        for (Character c : game.getParty()) {
            out.println("  " + c + (c.isAlive() ? "" : " [defeated]"));
            if (c.isAlive()) {
                out.println("      attacks: " + c.getCharacterClass().attacks().stream()
                        .map(w -> w.name() + " d" + w.damageSides() + " " + (w.attackBonus() >= 0 ? "+" : "") + w.attackBonus())
                        .collect(java.util.stream.Collectors.joining("; ")));
                printOdds(c);
            }
        }
    }

    /** Odds of each of the hero's attacks against the first living enemy, or against a typical AC when there is none. */
    private void printOdds(Character c) {
        Monster target = game.getState().getMonsters().stream().filter(Monster::isAlive).findFirst().orElse(null);
        int ac = target == null ? TYPICAL_AC : target.getArmorClass();
        out.println("      odds vs " + (target == null ? "a typical enemy (AC " + ac + ")"
                : target.getName() + " (AC " + ac + ")") + ":");
        for (gamemaster.model.Weapon w : c.getCharacterClass().attacks()) {
            gamemaster.rules.AttackOdds o = gamemaster.rules.AttackOdds.of(c, w, ac);
            out.println(String.format("        %-14s %-11s hit %3s   damage %s", w.name(),
                    o.rating().label(), o.hitText(), o.damageText()));
        }
    }

    private static final int TYPICAL_AC = 12;

    private String statusLine() {
        StringBuilder sb = new StringBuilder("[Turn " + Math.min(game.getState().getTurnsTaken(), game.getState().getTurnLimit())
                + "/" + game.getState().getTurnLimit() + "] Party: ");
        sb.append(String.join(" | ", game.getParty().stream()
                .map(c -> c.getName() + " " + c.getHealth() + "/" + c.getMaxHealth()).toList()));
        var monsters = game.getState().getMonsters().stream().filter(Monster::isAlive).toList();
        if (!monsters.isEmpty()) {
            sb.append("   Enemies: ").append(String.join(" | ", monsters.stream()
                    .map(m -> m.getName() + " " + m.getHealth() + "/" + m.getMaxHealth()).toList()));
        }
        return sb.toString();
    }

    private static String stats(Character c) {
        StringBuilder sb = new StringBuilder("  ");
        for (gamemaster.model.Stat s : gamemaster.model.Stat.values()) {
            sb.append(s.name(), 0, 3).append(' ').append(c.getStat(s)).append("  ");
        }
        return sb.toString().trim();
    }

    private void printHelp() {
        out.println("Setup commands:");
        out.println("  /add <Fighter|Wizard|Rogue> <name>     add a character with the class's default stats");
        out.println("  /custom <Fighter|Wizard|Rogue> <name>  add a character and spend 20 stat points yourself");
        out.println("  /remove <name>                         remove a character");
        out.println("  /party                                 show your party");
        out.println("  /start [theme]                         have the AI create an adventure and begin");
        out.println("Any time:");
        out.println("  /status   show the scene, party and enemies");
        out.println("  /undo     undo the last damage or flee (dice are not re-rolled)");
        out.println("  /save <name>, /load <name>, /saves");
        out.println("  /demo [off]  demo mode: 5 turns and one weak enemy, so a victory is quick to show");
        out.println("  /turns <n>  set the turn limit for the next adventure (a short one is handy for demos)");
        out.println("  /usage    model calls and tokens used so far");
        out.println("  /new      abandon this adventure and go back to setup");
        out.println("  /help, /quit");
        out.println("While playing, type what your party does, for example: I attack the wolf.");
    }

    private static void requireArg(String arg, String usage) {
        if (arg.isBlank()) {
            throw new IllegalArgumentException(usage);
        }
    }

    static String wrap(String text) {
        StringBuilder out = new StringBuilder();
        int lineLength = 0;
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (lineLength > 0 && lineLength + 1 + word.length() > WIDTH) {
                out.append('\n');
                lineLength = 0;
            } else if (lineLength > 0) {
                out.append(' ');
                lineLength++;
            }
            out.append(word);
            lineLength += word.length();
        }
        return out.toString();
    }
}
