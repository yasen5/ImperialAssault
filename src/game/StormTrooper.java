package src.game;

import src.Constants;
import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;

public class StormTrooper extends Imperial {
    public StormTrooper(Pos pos) {
        super("StormTrooper", 3, 3, pos,
                ImperialType.TROOPER, new DefenseDieType[] {DefenseDieType.BLACK}, 
                new OffenseDieType[] { OffenseDieType.BLUE,
                        OffenseDieType.GREEN });
    }

    @Override
    public Equipment.SurgeOptions[] getSurgeOptions() {
        return new Equipment.SurgeOptions[] { Equipment.SurgeOptions.DAMAGE1, Equipment.SurgeOptions.ACCURACY1 };
    }
}
