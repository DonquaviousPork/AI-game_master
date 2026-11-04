# AI Game Master (EECS3311 course project)

A single-player, D&D-inspired text RPG for one to four characters. An AI game
master writes the scenario and narrates the story. Deterministic Java code owns
the rules, dice and game state: the AI proposes, the engine decides.

## Prerequisites
- JDK 17 or newer
- Maven 3.9 or newer
- An API key for an OpenAI-compatible LLM service. The default is the free tier
  of [Groq](https://console.groq.com); any service with a chat-completions
  endpoint works.

## Setup
1. Clone the repository and open it in your IDE (or a terminal).
2. Copy `.env.example` to `.env`.
3. Edit `.env` and enter your own values:
   ```
   LLM_API_KEY=your key here
   LLM_MODEL=openai/gpt-oss-120b
   LLM_BASE_URL=https://api.groq.com/openai/v1
   ```
   `.env` is listed in `.gitignore`. Never commit it, and never put a real key
   in `.env.example`.
4. Dependencies (JUnit, Gson) are downloaded by Maven on the first build.

## Build and test
    mvn test

The tests use scripted dice and a fake model, so they need no network and no key.

## Run
Command-line game (run from the project folder, where `.env` is):

    mvn -q compile exec:java

Or open `src/main/java/gamemaster/cli/CliApp.java` in your IDE and run its `main` method.

Swing GUI (dark green theme, story pane, party and enemy cards with health bars):

    mvn -q compile exec:java -Dexec.mainClass=gamemaster.gui.GuiApp

Or run `gamemaster.gui.GuiApp` from your IDE. Both front ends use the same `GameFacade`. Use the View menu (or Ctrl+D / Ctrl+L) to switch between dark and light mode.

To check that your key and model work without playing, run `gamemaster.ai.LiveCheck`
from your IDE. It makes one real model call and prints a generated scenario.

## Example usage
Start the game, build a party, and let the AI create an adventure. Anything that is not a
command is treated as what your party does.

    setup> /add Fighter Brenna
    setup> /add Wizard Mira
    setup> /start willow grove
    > I swing my sword at the wolf
    (narration from the AI game master)
      [rules] attack: Brenna hits Shadow Wolf (rolled 17 vs AC 13) for 7 damage. ...
    Party: Brenna 21/24 | Mira 20/20   Enemies: Shadow Wolf 5/12
    > /save mygame

Commands: `/add <Fighter|Wizard|Rogue> <name>`, `/custom <class> <name>` (spend 20 stat
points yourself), `/remove <name>`, `/party`, `/start [theme]`, `/status`, `/undo`,
`/save <name>`, `/load <name>`, `/saves`, `/help`, `/quit`.
The `[rules]` lines show what the game engine actually rolled and changed; the AI only
narrates and requests actions.

## Project layout
| Package | Contents |
|---|---|
| `gamemaster.model` | Characters, classes, party, monsters, weapons, `CharacterFactory` |
| `gamemaster.rules` | `RuleEngine` (ability checks, attacks, damage, saving throws) and the `Dice` interface |
| `gamemaster.game` | `GameState`, game phases, player actions with undo, `GameEngine` |
| `gamemaster.llm` | `LLMClient`, `OpenAIClient`, `LLMConfig`, `ChatMessage` |
| `gamemaster.ai` | Scenario generation, the agent loop (`AgentOrchestrator`), tools (`ToolRegistry`) and story memory (`MemoryManager`) |
| `gamemaster.app` | `GameFacade`, the single entry point used by the CLI and GUI |
| `gamemaster.cli` | `CliSession` and `CliApp`, the command-line game |
| `gamemaster.gui` | Swing GUI: `GuiApp`, `MainWindow`, `SetupPanel`, `GamePanel` (a `GameView`), `GuiTheme` |
| `gamemaster.save` | `SaveRepository` and `SaveService` for saving and loading games |

## Design patterns in the code so far
- **Factory Method:** `CharacterFactory` builds characters from classes or custom stat choices.
- **State:** `GamePhase` with `SetupPhase`, `StoryPhase`, `CombatPhase`, `EndedPhase`.
- **Command:** `Action` with `AttackAction`, `AbilityCheckAction`, `FleeAction`, and `ActionHistory` for undo.
- **Observer:** `GameState` notifies attached `GameView`s on every change.
- **Adapter:** `LLMClient` with `OpenAIClient` adapts any OpenAI-compatible service.
- **Facade:** `GameFacade` hides the engine, AI agent, memory and saving behind a few methods.
- **Memento:** `GameSnapshot` (memento), `GameState` (originator) and `SaveRepository` (caretaker) save and restore a game.

## Status
Work in progress. Done so far: characters and party, dice rules, monsters, the
game engine with phases and undo, AI scenario generation, the AI agent loop with
tools and story memory, save and load, the facade, the command-line game and the Swing GUI.
Saved games are written to a `saves/` folder, which is ignored by git.
See `CHANGES.md` for every difference from the Stage 1 design.
