package gamemaster.rules;

/** Outcome of a saving throw (F10). Success means the effect is avoided. */
public record SaveResult(int roll, int modifier, int total, int difficulty, boolean success) {}
