package gamemaster.rules;

/** Outcome of an attack roll (F08). */
public record AttackResult(int roll, int modifier, int total, int targetArmorClass, boolean hit) {}
