# Changes from Stage 1 design

Format: Original design -> Implemented change -> Reason -> Impact.

- `CharacterFactory.createFromClass(classType)` -> `createFromClass(name, classType)`
  -> a character needs a name -> F02 signature only.
- `CharacterFactory.createCustom(statAllocations)` -> `createCustom(name, classType, allocations)`
  -> every Character holds a CharacterClass, and a custom build still needs a name
  -> F03 signature only. Allocations are points added on top of a base score of 8.
