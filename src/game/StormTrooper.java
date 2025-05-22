package src.game;

import src.Constants;

public class StormTrooper extends Imperial {
    public StormTrooper(Pos pos) {
        super("StormTrooper", 3, 3, pos,
                ImperialType.TROOPER);
    }

    @Override
    public Die.DefenseDieResult[] getDefense() {
        return new Die.DefenseDieResult[] { Die.rollDefense(Die.DefenseDieType.BLACK) };
    }

    @Override
    public Die.OffenseDieResult[] getOffense() {
        return new Die.OffenseDieResult[] { Die.rollOffense(Die.OffenseDieType.BLUE),
                Die.rollOffense(Die.OffenseDieType.GREEN) };
    }

    @Override
    public Equipment.SurgeOptions[] getSurgeOptions() {
        return new Equipment.SurgeOptions[] { Equipment.SurgeOptions.DAMAGE1, Equipment.SurgeOptions.ACCURACY1 };
    }
}
