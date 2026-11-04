# Changes from Stage 1 design

Format for every entry: Original design -> Implemented change -> Reason -> Impact.
Entries are grouped by the commit that introduced them. Commit messages are in
the git history; this file is the design-level record.

## Commit 1: model layer (F02, F03)

- `CharacterFactory.createFromClass(classType)` -> `createFromClass(name, classType)`
  -> a character needs a name
  -> F02 signature only.
- `CharacterFactory.createCustom(statAllocations)` -> `createCustom(name, classType, allocations)`
  -> every `Character` holds a `CharacterClass`, and a custom build still needs a name
  -> F03 signature only. Allocations are points added on top of a base score of 8.
- Stage 1 left the stat numbers open -> base score 8, maximum 15, 20-point pool, class defaults total 68,
  health = 10 + CON, armor class = 10 + DEX modifier, modifier = floor((score - 10) / 2)
  -> the rules engine and tests need exact numbers
  -> affects F02, F03 and every roll in F07 to F10.

## Commit 2: rule engine and dice (F07, F08, F09, F10)

- `RuleEngine.rollAttack(attacker, target)` -> `rollAttack(attacker, weapon, target)`
  -> the attack stat and damage die depend on the weapon
  -> F08 and F09 signatures.
- No `Weapon` class in the diagram -> `Weapon` record (name, damage die, stat) with LONGSWORD, QUARTERSTAFF, DAGGER
  -> weapons decide which stat modifier applies and which die is rolled
  -> new class; used by F08 and F09.
- Dice used directly inside `RuleEngine` -> `Dice` interface with `RandomDice` for play and a scripted `FixedDice` in tests
  -> dice rolls must be reproducible for deterministic tests
  -> new interface and two implementations; no change to feature behaviour.
- Roll outcomes as plain values -> `CheckResult`, `AttackResult`, `SaveResult` records
  -> callers need the roll, modifier, total and target number to narrate and log the result
  -> new classes; `RuleEngine` still never changes any state.
- Damage could be zero or negative -> a hit always deals at least 1 damage
  -> avoids "hit for 0" outcomes with negative modifiers
  -> F09 rule detail.

## Commit 3: combatants (F08, F09, F10)

- Only `Character` in the design -> `Combatant` interface implemented by `Character` and a new `Monster` class
  -> enemies also need health, armor class and attacks, but have no class or point-buy
  -> new interface and class; `Monster` is not in the Stage 1 class diagram.
- `RuleEngine` methods took `Character` -> they take `Combatant`
  -> the same dice rules now cover players and monsters
  -> F07, F08, F09, F10 signatures.
- `Character` had no healing -> `Combatant.heal(int)` added, capped at maximum health
  -> needed to undo damage and for later healing effects
  -> new method on `Character` and `Monster`.
- `FixedDice` was package-private -> public
  -> tests in other packages (the game engine) need scripted dice
  -> test code only.

## Commit 4: game core

- `GameState`, `GamePhase` and subclasses, `Action` and subclasses, `ActionHistory`, `GameEngine` as in Stage 1
  -> implemented as designed
  -> Observer, State and Command patterns are now in code.
- `Scenario` and `StoryEvent` appeared only as diagram boxes -> small records with validation
  -> they are plain data and need no behaviour
  -> no design impact.
- `GamePhase.allows(Action)` (implied by the diagram) -> `allows(ActionKind)` with a new `ActionKind` enum
  -> avoids a circular dependency between phases and actions
  -> internal; the State pattern is unchanged.
- `Action.execute` returned nothing specific -> returns `ActionResult` (one summary line)
  -> the log and views need readable text without re-deriving it from state
  -> new record.
- Undo of dice rolls was not addressed -> undo reverses state changes only (damage, removed monsters), never a roll
  -> a roll cannot be un-rolled and replaying it would change the outcome
  -> `AbilityCheckAction.undo` does nothing; documented limit of the undo feature.
- `FleeAction` was not in the diagram -> new action: a DEXTERITY check that removes all monsters on success
  -> players need a way out of combat
  -> new class.
- `setScenario` and `setCurrentEvent` on `GameEngine` -> implemented as designed
  -> these were the Stage 1 method names after the consistency fixes
  -> none.
- Added `GameEngine.addMonster`, `concludeStory`, `undoLast`, `checkEndCondition`, `advancePhase`
  -> encounters and the end condition need entry points the engine owns
  -> `concludeStory` is how the story ends by choice; the AI will call it only through the engine.

## Commit 5: LLM layer and scenario generation (F01)

- `OpenAIClient` as a provider-specific adapter -> `OpenAIClient` targets any OpenAI-compatible
  chat-completions endpoint, chosen by `LLM_BASE_URL` in `.env` (default: Groq)
  -> a free provider is needed for a school project, and this avoids tying the code to one vendor
  -> Adapter pattern unchanged; switching provider needs no code change.
- `LLMClient` returned the model's reply -> `LLMClient.complete(List<ChatMessage>)` returns plain text
  -> the simplest interface that the scenario generator and later agent code can share
  -> new `ChatMessage` record and `LLMException` class, neither in the Stage 1 diagram.
- No configuration class in the design -> `LLMConfig` reads `.env` and environment variables
  -> keys must stay out of source code and out of git
  -> new class; the key is hidden in `toString()`, and placeholder values are rejected.
- Scenario taken directly from the AI -> `ScenarioGenerator` asks for JSON, validates every field
  (monsters 0 to 3, health 1 to 30, armor class 8 to 18, known weapons only) and retries once,
  telling the model what was wrong
  -> the AI must not be able to set arbitrary numbers
  -> F01 is hybrid: the AI proposes, code decides.
- `GeneratedScenario.applyTo(engine)` hands an accepted scenario to `GameEngine`
  -> the engine, not the AI, performs the phase changes
  -> new record; follows the "AI proposes, engine decides" rule.
- Added the Gson library (2.10.1) as a dependency
  -> JSON parsing and building by hand is error-prone
  -> new entry in `pom.xml`.
- Added `LiveCheck`, a manual class that makes one real model call
  -> verifies that a local `.env` works
  -> development aid only; it will be replaced by the CLI.

## Commit 6: AI tools and tool-call support (F06, F12 groundwork)

- `LLMClient.complete(messages)` returned text only -> `LLMClient.chat(messages, tools)` returns an
  `LLMResponse` (text and/or tool requests); `complete` stays as a default method
  -> the model must be able to ask the game to do things, not only talk
  -> new `ToolSpec`, `ToolCall` and `LLMResponse` records; `ChatMessage` gained tool-call and tool-result
  fields; `OpenAIClient` sends and parses the OpenAI tool-calling format. Scenario generation is unaffected.
- `ToolRegistry` was in the Stage 1 sequence diagrams as a box but never used
  -> implemented as the only door between the AI and the game: it lists tools, runs one requested call,
  and turns every bad request (unknown tool, malformed JSON, invalid values, wrong phase) into an
  "Error: ..." message for the model instead of an exception
  -> the agent now shows real tool use.
- New `Tool` interface with four tools: `roll_check`, `attack`, `spawn_monster`, `end_story`
  -> each is a thin wrapper that validates arguments and calls `GameEngine`; the AI never rolls dice,
  sets damage or changes health itself
  -> these map onto the existing `Action` commands, so undo and the phase rules apply to AI-driven moves too.
- Monster limits were inside `ScenarioGenerator` -> moved to a shared `JsonFields` helper
  -> `spawn_monster` must judge a monster by exactly the same limits as scenario generation
  -> internal refactor; behaviour unchanged. New limit: at most 4 living monsters at once.
- Combatants referred to by name -> `Lookup` resolves names case-insensitively, and prefers a living
  combatant when two share a name (two wolves)
  -> the model talks in names, not object references
  -> new package-private helper; attacks are only allowed between heroes and monsters.
- Characters had no weapon -> weapon chosen from the class (Fighter longsword, Rogue dagger, otherwise quarterstaff)
  -> attacks need a weapon and Stage 1 did not say where it comes from
  -> lives in `Lookup.weaponOf`; revisit if players should choose weapons.

## Commit 7: agent loop (F06, F12)

- The Stage 1 sequence diagrams showed the AI reply going straight to the game
  -> `AgentOrchestrator.takeTurn` runs a bounded loop: show the model the real state, let it request
  tools, run them through `ToolRegistry`, feed the results back, repeat until it narrates
  -> this is the multi-step, tool-using agent behavior; the AI proposes, the registry and engine decide.
- No limit on AI activity was specified -> at most 6 model rounds and 8 tool runs per turn; if the model
  keeps asking for tools it is made to narrate without them
  -> a confused or misbehaving model must not loop forever or flood the game
  -> extra tool requests receive an "Error: too many actions" result instead of running.
- The model had to remember the game -> `StateDescriber` sends the true party, enemy, phase, scene and recent
  event list every turn
  -> the model never has to guess or recall hit points
  -> new package-private class; the full story memory is a later commit.
- Narration was plain text -> `TurnResult` carries the narration, one line per tool the game ran, and a game-over flag
  -> the CLI and GUI need to show both the story and what the rules actually did
  -> new record; the narration becomes the engine's current scene.
- Models use typographic hyphens and quotes some consoles show as "?" -> `TextCleaner` replaces them with plain characters
  -> seen during the live test
  -> small helper; no design impact.

## Commit 8: story memory

- Stage 1 had a memory component for the story -> `MemoryManager` keeps the last 8 turns word for word
  (player input, what the rules did, the narration) and folds the 4 oldest into a running summary
  once that limit is passed
  -> the model has no memory of its own between calls, and sending the whole history would exceed
  the free model's token limit
  -> the summary is written by the model, so this is AI-based memory compression; it adds one extra
  model call every few turns, not every turn.
- Nothing said what happens if summarizing fails -> on an `LLMException` the folded turns are kept as plain
  truncated text
  -> a network error or rate limit must not lose the story or stop the game
  -> memory degrades gracefully instead of throwing.
- Summary size was unbounded -> capped at 1,500 characters (cut at a sentence end), narration at 400 and each
  tool result at 160 characters when stored
  -> keeps every prompt small
  -> hard limits in `MemoryManager`.
- `AgentOrchestrator` built each prompt from the game state only -> it now adds the memory text ("STORY SO FAR")
  and records each finished turn, including what the rules did
  -> the model can stay consistent with earlier events and names
  -> new constructor argument (the old constructors still work).
- Added `getSummary`, `getRecentTurns` and `restore` on `MemoryManager`
  -> save and load (next commits) must store and rebuild the memory
  -> small API that the Memento will use.

## Still to log (later commits)

- Save and load with the Memento pattern (F14).
- CLI, GUI and `GameFacade`.
- Updated class diagram, plus sequence diagrams that changed.
