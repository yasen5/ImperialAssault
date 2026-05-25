package game;

import game.Die.DefenseDieType;
import game.Die.OffenseDieType;
import game.Die.OffenseRoll;

public class ProbeDroid extends Imperial {
    public ProbeDroid(Pos pos) {
        super("ProbeDroid", 7, 4, pos,
                ImperialType.DROID, new DefenseDieType[] { DefenseDieType.BLACK },
                new OffenseDieType[] { OffenseDieType.BLUE, OffenseDieType.YELLOW, OffenseDieType.YELLOW },
                false, false);
    }

    @Override
    public Equipment.SurgeOptions[] getSurgeOptions() {
        return new Equipment.SurgeOptions[] { Equipment.SurgeOptions.PIERCE2, Equipment.SurgeOptions.DAMAGE2,
                Equipment.SurgeOptions.RECOVER2 };
    }

    @Override
    public OffenseRoll[] getOffense() {
        OffenseRoll[] results = super.getOffense();
        if (game.promptYesNo(getOwnerSeat(), "Ability", "Reroll an attack die?")) {
            int chosenDie = game.promptMultipleChoice(getOwnerSeat(), "Reroll", "Choose which die to reroll",
                    offenseDice);
            game.removeOffenseDie(chosenDie);
            results[chosenDie] = offenseDice[chosenDie].roll(game);
        }
        return results;
    }
}
