package game;

import game.Die.DefenseDieType;
import game.Die.OffenseDieType;

public class FennSignis extends Hero {
    public FennSignis(Pos pos) {
        super("FennSignis", 12, 4, 4, new Equipment.Weapon("Infantry Rifle",
                new OffenseDieType[] { OffenseDieType.BLUE, OffenseDieType.GREEN }, new Equipment.SurgeOptions[] {
                        Equipment.SurgeOptions.DAMAGE1,
                        Equipment.SurgeOptions.ACCURACY1 },
                false, false, "FennSignis"),
                pos, false, new DefenseDieType[] { DefenseDieType.BLACK }, false);
    }
}
