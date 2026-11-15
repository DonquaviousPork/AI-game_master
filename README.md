# AI Game Master (EECS3311 course project)

A single-player, D&D-inspired text RPG for one to four characters. An AI game
master writes the scenario and narrates the story. Deterministic Java code owns
the rules, dice and game state: the AI proposes, the engine decides.

Every adventure has one fixed **objective** and lasts at most **20 turns**. Reach
the objective before the turns run out (or the party falls) and the story ends
in victory or defeat. It works as a Swing GUI and as a command-line game, both
built on the same `GameFacade`.

## Prerequisites
- JDK 17 or newer
- Maven 3.9 or newer
- An API key for an OpenAI-compatible LLM service. Any service with a
  chat-completions endpoint works (the examples below use Groq and OpenAI).

## Setup
1. Clone the repository and open it in your IDE (or a terminal).
2. Copy `.env.example` to `.env`.
3. Edit `.env` and enter your own values. Example for Groq:
   ```
   LLM_API_KEY=your key here
   LLM_MODEL=openai/gpt-oss-120b
   LLM_BASE_URL=https://api.groq.com/openai/v1
   ```
   Example for OpenAI:
   ```
   LLM_API_KEY=your key here
   LLM_MODEL=gpt-4o-mini
   LLM_BASE_URL=https://api.openai.com/v1
   ```
   `.env` is listed in `.gitignore`. Never commit it, and never put a real key
   in `.env.example`.
4. Dependencies (JUnit, Gson) are downloaded by Maven on the first build.

## Build and test
    mvn test

The tests use scripted dice and a fake model, so they need no network and no key.

## Run
Swing GUI (run from the project folder, where `.env` is):

    mvn -q compile exec:java -Dexec.mainClass=gamemaster.gui.GuiApp

or run `gamemaster.gui.GuiApp` from your IDE.

Command-line game:

    mvn -q compile exec:java

or run `gamemaster.cli.CliApp` from your IDE.

To check that your key and model work without playing, run `gamemaster.ai.LiveCheck`
from your IDE. It makes one real model call and prints a generated scenario.

## Double-click version (Windows)
Run `build-exe.bat` from the project folder (needs the JDK and Maven). It builds
`target/AIGameMaster.jar` (a runnable jar: `java -jar target/AIGameMaster.jar`) and then
uses `jpackage` to make `dist\AI Game Master\AI Game Master.exe`, which bundles its own
Java, so it starts with a double-click. The script copies your `.env` next to the exe;
the game also looks for `.env` in its own folder. Do not share or commit the `dist` folder,
because it contains your key. Saved games go to a `saves` folder next to where the game starts.

## How to play
1. **Build a party** of one to four heroes. Pick a class (Fighter, Rogue or Wizard)
   and a name, or spend 20 stat points on a custom hero.
2. **Start an adventure** with an optional theme. The AI writes the scenario, the
   opening scene, a fixed objective and any enemies.
3. **Read the objective screen** (GUI). The turn counter (`TURN n / 20`) and the
   objective stay at the top of the play screen.
4. **Act.** Type what your party does, or use the Battle menu in combat.
5. **The story ends** when the AI concludes it, the party is defeated, or the turn
   limit is reached. The engine itself guarantees the ending at turn 20.

### Rules at a glance
- Each hero has four attacks, each with its own damage die and to-hit bonus.
  Open the Party card or use `/party` to see them.
- A natural 20 is a critical hit (always hits, doubled damage dice). A natural 1
  is a fumble (always misses).
- Naming a move the hero does not have makes the attempt fail and wastes the turn.
- The `[rules]` lines in the story show what the engine actually rolled and changed.
  The AI only narrates and requests actions through tools.
- A failed model call is rolled back, so a turn is never half applied.

### GUI features
- **Party and enemy cards** show health bars; click a card to see abilities,
  modifiers, HP, AC and attacks.
- **Battle menu** (combat only): pick each hero's attack and target from menus,
  then press Fight! Switch back with "Type instead".
- **Undo**, **Save**, **Load** and **Main menu** buttons (the menu warns about
  unsaved progress; closing the window does too).
- **View menu:** dark and light mode (Ctrl+D / Ctrl+L), remembered between runs.
- **Settings menu:**
  - *Sound...* sets music volume, effects volume and mute (remembered).
  - *Demo mode: 5-turn adventures* shortens the next adventure or loaded game
    to 5 turns so every feature can be shown quickly. It still uses the real model.
- **Sound:** button clicks, menu music, and victory and defeat jingles. All sounds
  are generated in code; no audio files are needed. If the computer has no sound
  device the game simply stays silent.
- The status line shows how many model calls and tokens the session has used.

### Command-line example
Anything that is not a command is treated as what your party does.

    setup> /add Fighter Brenna
    setup> /add Wizard Mira
    setup> /start willow grove
    > Brenna: Power Strike on the wolf
    (narration from the AI game master)
      [rules] attack: Brenna hits Shadow Wolf with Power Strike (rolled 17 vs AC 13) for 7 damage. ...
    [Turn 1/20] Party: Brenna 21/24 | Mira 20/20   Enemies: Shadow Wolf 5/12
    > /save mygame

Commands: `/add <Fighter|Wizard|Rogue> <name>`, `/custom <class> <name>` (spend 20 stat
points yourself), `/remove <name>`, `/party`, `/start [theme]`, `/status`, `/undo`,
`/save <name>`, `/load <name>`, `/saves`, `/new` (abandon the adventure and go back to
setup), `/turns <n>` (turn limit for the next adventure, 1-20, handy for demos),
`/usage` (model calls and tokens used), `/help`, `/quit`.

## Rate limits and costs
- If the service answers "too many requests" (HTTP 429), the game waits for the time
  the service asks for (up to 65 s per wait, up to 6 retries, 150 s in total) and then
  resends the same request. The GUI shows a countdown and a **Stop waiting** button;
  the command line prints the countdown.
- If the service asks for a much longer wait, that is a daily limit: the game stops
  retrying and tells you so. Wait for the limit to reset, or change `LLM_MODEL`,
  `LLM_BASE_URL` and `LLM_API_KEY` in `.env`.
- A turn makes several model calls, each carrying the prompt, story memory and game
  state. Free tiers with small per-minute token limits can be reached quickly;
  `/usage` (CLI) or the status line (GUI) shows what you have used.

## Project layout
| Package | Contents |
|---|---|
| `gamemaster.model` | Characters, classes, party, monsters, weapons, `CharacterFactory` |
| `gamemaster.rules` | `RuleEngine` (ability checks, attacks, critical hits, damage, saving throws) and the `Dice` interface |
| `gamemaster.game` | `GameState`, game phases, player actions with undo, `GameEngine`, `Scenario` |
| `gamemaster.llm` | `LLMClient`, `OpenAIClient` (with rate-limit retry and token counting), `LLMConfig`, `ChatMessage` |
| `gamemaster.ai` | Scenario generation, the agent loop (`AgentOrchestrator`), tools (`ToolRegistry`), `StateDescriber`, name matching (`Lookup`) and story memory (`MemoryManager`) |
| `gamemaster.app` | `GameFacade`, the single entry point used by the CLI and GUI |
| `gamemaster.cli` | `CliSession` and `CliApp`, the command-line game |
| `gamemaster.gui` | Swing GUI: `GuiApp`, `MainWindow`, `AppPanel`, `SetupPanel`, `BriefingPanel`, `GamePanel` (a `GameView`), `BattlePanel`, `SoundDialog`, `GuiTheme` |
| `gamemaster.audio` | `ToneSynth` (generates the sounds) and `Sound` (plays them) |
| `gamemaster.save` | `SaveRepository` and `SaveService` for saving and loading games |

## Design patterns in the code
- **Factory Method:** `CharacterFactory` builds characters from classes or custom stat choices.
- **State:** `GamePhase` with `SetupPhase`, `StoryPhase`, `CombatPhase`, `EndedPhase`.
- **Command:** `Action` with `AttackAction`, `AbilityCheckAction`, `FleeAction`, and `ActionHistory` for undo.
- **Observer:** `GameState` notifies attached `GameView`s on every change.
- **Adapter:** `LLMClient` with `OpenAIClient` adapts any OpenAI-compatible service.
- **Facade:** `GameFacade` hides the engine, AI agent, memory and saving behind a few methods.
- **Memento:** `GameSnapshot` (memento), `GameState` (originator) and `SaveRepository` (caretaker) save and restore a game.

UMLet diagrams (`.uxf`) of the classes and of one game turn are in the repository.

## Status
All planned features are implemented: characters and party, dice rules with critical
hits, monsters, the game engine with phases and undo, AI scenario generation with a
fixed objective and turn limit, the AI agent loop with tools and story memory, save and
load, the facade, the command-line game, the Swing GUI (battle menu, card stats, dark
and light mode, sound, demo mode) and automatic rate-limit handling.

Known limitations:
- The story is written by a language model, so narration quality varies, and it can
  occasionally describe something the rules did not do. The `[rules]` lines are
  always the truth.
- Free model tiers may rate-limit long sessions (see above).
- Sound quality is basic because all sounds are generated in code.
- Saved games are written to a `saves/` folder, which is ignored by git.

See `CHANGES.md` for every difference from the Stage 1 design.
