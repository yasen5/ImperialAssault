package game;

import game.Die.DefenseDieType;
import game.Die.OffenseDieType;

public class EWebEngineer extends Imperial {
    public EWebEngineer(Pos pos) {
        super("EWebEngineer", 5, 2, pos,
                ImperialType.TROOPER, new DefenseDieType[] { DefenseDieType.BLACK },
                new OffenseDieType[] { OffenseDieType.BLUE, OffenseDieType.RED, OffenseDieType.YELLOW },
                false, false);
    }

    @Override
    public Equipment.SurgeOptions[] getSurgeOptions() {
        return new Equipment.SurgeOptions[] { Equipment.SurgeOptions.RECOVER2, Equipment.SurgeOptions.DAMAGE1,
                Equipment.SurgeOptions.ACCURACY2 };
    }
}
