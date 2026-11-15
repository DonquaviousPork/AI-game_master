package gamemaster.game;

import gamemaster.model.Character;
import gamemaster.model.Combatant;
import gamemaster.rules.RuleEngine;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A wizard's Healing Light: every living hero recovers a d6 plus the wizard's WISDOM modifier (at least 1).
 * Fallen heroes are not revived. It can be cast {@link GameState#HEALS_PER_ADVENTURE} times per adventure.
 */
public class HealAction implements Action {

    private final Combatant caster;
    private final Map<Character, Integer> healed = new LinkedHashMap<>();

    public HealAction(Combatant caster) {
        this.caster = Objects.requireNonNull(caster, "caster");
    }

    @Override public ActionKind kind() { return ActionKind.HEAL; }

    @Override
    public ActionResult execute(GameState state, RuleEngine rules) {
        if (!(caster instanceof Character c) || !c.getCharacterClass().name().equalsIgnoreCase("Wizard")) {
            throw new IllegalStateException(caster.getName() + " cannot cast Healing Light: only wizards can.");
        }
        if (!caster.isAlive()) {
            throw new IllegalStateException(caster.getName() + " is defeated and cannot cast.");
        }
        if (state.healsLeft() < 1) {
            throw new IllegalStateException("Healing Light has already been cast "
                    + GameState.HEALS_PER_ADVENTURE + " times this adventure.");
        }
        int amount = rules.rollHealing(caster);
        healed.clear();
        StringBuilder detail = new StringBuilder();
        for (Character hero : state.getParty().getCharacters()) {
            if (!hero.isAlive()) {
                continue;
            }
            int before = hero.getHealth();
            hero.heal(amount);
            int gained = hero.getHealth() - before;
            healed.put(hero, gained);
            detail.append(detail.length() == 0 ? "" : ", ").append(hero.getName()).append(" +").append(gained);
        }
        state.noteHeal();
        return new ActionResult(caster.getName() + " casts Healing Light (rolled " + amount + "): " + detail
                + ". Uses left: " + state.healsLeft() + ".");
    }

    @Override
    public void undo(GameState state) {
        healed.forEach((hero, gained) -> hero.applyDamage(gained));
        if (!healed.isEmpty()) {
            state.undoHeal();
        }
        healed.clear();
    }
}
