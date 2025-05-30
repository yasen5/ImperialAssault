package src.game;

import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;

public class Gaarkhan extends Hero {
    public Gaarkhan(Pos pos) {
        super("Gaarkhan", 12, 4, 4, new Equipment.Weapon("Vibro-Ax",
                new OffenseDieType[] { OffenseDieType.YELLOW, OffenseDieType.YELLOW }, new Equipment.SurgeOptions[] {
                        Equipment.SurgeOptions.PIERCE1,
                        Equipment.SurgeOptions.DAMAGE1 }, true, true),
                pos, true, new DefenseDieType[] { DefenseDieType.WHITE });
    }

    @Override
    public void performSpecial() {
        Game.handleMoves(this, getSpeed());
        Game.handleAttack(this);
    }
}
