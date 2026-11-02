package gamemaster.rules;

/** Outcome of an ability check (F07). */
public record CheckResult(int roll, int modifier, int total, int difficulty, boolean success) {}
