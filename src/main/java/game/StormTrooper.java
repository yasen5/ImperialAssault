package game;

import game.Die.DefenseDieType;
import game.Die.OffenseDieType;
import game.Die.OffenseRoll;

public class StormTrooper extends Imperial {
    public StormTrooper(Pos pos) {
        super("StormTrooper", 3, 3, pos,
                ImperialType.TROOPER, new DefenseDieType[] { DefenseDieType.BLACK },
                new OffenseDieType[] { OffenseDieType.BLUE,
                        OffenseDieType.GREEN },
                false, false);
    }

    @Override
    public Equipment.SurgeOptions[] getSurgeOptions() {
        return new Equipment.SurgeOptions[] { Equipment.SurgeOptions.DAMAGE1, Equipment.SurgeOptions.ACCURACY1 };
    }

    // Same as the superclass's implementation, except you can reroll if a trooper
    // is near
    @Override
    public OffenseRoll[] getOffense() {
        OffenseRoll[] results = new OffenseRoll[offenseDice.length + (focused ? 1 : 0)];
        for (int i = 0; i < offenseDice.length; i++) {
            results[i] = offenseDice[i].roll(game);
        }
        if (focused) {
            results[results.length - 1] = OffenseDieType.GREEN.roll(game);
            focused = false;
        }
        if (game != null) {
            game.repaint();
        }
        if (trooperNear() && (game != null ? game.promptYesNo(getOwnerSeat(), "Ability", "Reroll an attack die?")
                : InputUtils.getYesNo("Ability", "Reroll an attack die?"))) {
            int chosenDie = game != null
                    ? game.promptMultipleChoice(getOwnerSeat(), "Reroll", "Choose which die to reroll", offenseDice)
                    : InputUtils.getMultipleChoice("Reroll", "Choose which die to reroll", offenseDice);
            if (game != null) {
                game.removeOffenseDie(chosenDie);
            }
            results[chosenDie] = offenseDice[chosenDie].roll(game);
        }
        return results;
    }

    // Checks if there are adjacent troopers
    public boolean trooperNear() {
        for (Directions dir : Directions.values()) {
            Personnel adjacentPersonnel = game != null ? game.getPersonnelAtPos(getPos().getNextPos(dir)) : null;
            if (adjacentPersonnel instanceof Imperial && ((Imperial) (adjacentPersonnel)).getType() == getType()) {
                return true;
            }
        }
        return false;
    }
}
