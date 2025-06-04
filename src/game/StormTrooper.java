package src.game;

import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;
import src.game.Die.OffenseRoll;

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

    @Override
    public OffenseRoll[] getOffense() {
        OffenseRoll[] results = new OffenseRoll[offenseDice.length + (focused ? 1 : 0)];
        for (int i = 0; i < offenseDice.length; i++) {
            results[i] = offenseDice[i].roll();
        }
        if (focused) {
            results[results.length - 1] = OffenseDieType.GREEN.roll();
            focused = false;
        }
        Game.repaintScreen();
        if (InputUtils.getYesNo("Ability", "Reroll an attack die?")) {
            int chosenDie = InputUtils.getMultipleChoice("Reroll", "Choose which die to reroll", offenseDice);
            Game.removeOffenseDie(chosenDie);
            results[chosenDie] = offenseDice[chosenDie].roll();
        }
        return results;
    }

    public boolean trooperNear() {
        for (Directions dir : Directions.values()) {
            Personnel adjacentPersonnel = Game.getPersonnelAtPos(getPos().getNextPos(dir));
            if (adjacentPersonnel instanceof Imperial && ((Imperial) (adjacentPersonnel)).getType() == getType()) {
                return true;
            }
        }
        return false;
    }
}
