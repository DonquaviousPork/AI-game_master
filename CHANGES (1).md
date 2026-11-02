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

## Still to log (later commits)

- Agent loop, tools and memory (F11, F12, F13).
- Save and load with the Memento pattern (F14).
- CLI, GUI and `GameFacade`.
- Updated class diagram, plus sequence diagrams that changed.
