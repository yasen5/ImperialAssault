package game;

import game.Die.DefenseDieType;
import game.Die.OffenseDieType;

public class MakEshray extends Hero {
    public MakEshray(Pos pos) {
        super("MakEshray", 10, 4, 5, new Equipment.Weapon("Longblaster",
                new OffenseDieType[] { OffenseDieType.BLUE, OffenseDieType.YELLOW }, new Equipment.SurgeOptions[] {
                        Equipment.SurgeOptions.PIERCE2,
                        Equipment.SurgeOptions.ACCURACY2 },
                false, false, "MakEshray"),
                pos, false, new DefenseDieType[] { DefenseDieType.WHITE }, false);
    }
}
