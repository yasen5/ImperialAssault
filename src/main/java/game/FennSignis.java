package game;

import game.Die.DefenseDieType;
import game.Die.OffenseDieType;

public class FennSignis extends Hero {
    private boolean havocShotActive;

    public FennSignis(Pos pos) {
        super("FennSignis", 12, 4, 4, new Equipment.Weapon("Infantry Rifle",
                new OffenseDieType[] { OffenseDieType.BLUE, OffenseDieType.GREEN }, new Equipment.SurgeOptions[] {
                        Equipment.SurgeOptions.DAMAGE1,
                        Equipment.SurgeOptions.ACCURACY2 },
                false, false, "FennSignis"),
                pos, true, new DefenseDieType[] { DefenseDieType.BLACK }, false);
    }

    @Override
    public void performSpecial() {
        havocShotActive = true;
        try {
            game.handleAttack(this);
        } finally {
            havocShotActive = false;
        }
    }

    @Override
    public int getBlastValue() {
        return havocShotActive ? 1 : 0;
    }

    @Override
    public boolean specialNeedsAttackTarget() {
        return true;
    }
}
