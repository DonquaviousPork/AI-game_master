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
GUI and CLI launch commands will be added as those parts are implemented.

To check that your key and model work, run `gamemaster.ai.LiveCheck` from your
IDE (the Run button above its `main` method). It makes one real model call and
prints a generated scenario. An optional theme can be passed as a program argument.

## Example usage
Example sessions for the CLI and GUI will be added with those interfaces.

## Project layout
| Package | Contents |
|---|---|
| `gamemaster.model` | Characters, classes, party, monsters, weapons, `CharacterFactory` |
| `gamemaster.rules` | `RuleEngine` (ability checks, attacks, damage, saving throws) and the `Dice` interface |
| `gamemaster.game` | `GameState`, game phases, player actions with undo, `GameEngine` |
| `gamemaster.llm` | `LLMClient`, `OpenAIClient`, `LLMConfig`, `ChatMessage` |
| `gamemaster.ai` | Scenario generation, the agent loop (`AgentOrchestrator`), tools (`ToolRegistry`) and story memory (`MemoryManager`) |
| `gamemaster.save` | `SaveRepository` and `SaveService` for saving and loading games |

## Design patterns in the code so far
- **Factory Method:** `CharacterFactory` builds characters from classes or custom stat choices.
- **State:** `GamePhase` with `SetupPhase`, `StoryPhase`, `CombatPhase`, `EndedPhase`.
- **Command:** `Action` with `AttackAction`, `AbilityCheckAction`, `FleeAction`, and `ActionHistory` for undo.
- **Observer:** `GameState` notifies attached `GameView`s on every change.
- **Adapter:** `LLMClient` with `OpenAIClient` adapts any OpenAI-compatible service.
- **Memento:** `GameSnapshot` (memento), `GameState` (originator) and `SaveRepository` (caretaker) save and restore a game.

## Status
Work in progress. Done so far: characters and party, dice rules, monsters, the
game engine with phases and undo, AI scenario generation, the AI agent loop with
tools and story memory, and save and load. Still to come: the CLI and the GUI.
Saved games are written to a `saves/` folder, which is ignored by git.
See `CHANGES.md` for every difference from the Stage 1 design.
