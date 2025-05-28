package src.game;

import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;

public class GideonArgus extends Hero {
    public GideonArgus(Pos pos) {
        super("GideonArgus", 10, 4, 5, new Equipment.Weapon("Plasteel Staff",
                new OffenseDieType[] { OffenseDieType.BLUE, OffenseDieType.YELLOW }, new Equipment.SurgeOptions[] {
                        Equipment.SurgeOptions.STUN,
                        Equipment.SurgeOptions.DAMAGE1 }),
                pos, false, new DefenseDieType[] { DefenseDieType.WHITE });
    }
}
