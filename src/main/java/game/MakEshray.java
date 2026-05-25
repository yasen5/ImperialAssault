package game;

import game.Die.DefenseDieType;
import game.Die.OffenseDieType;

public class MakEshray extends Hero {
    public MakEshray(Pos pos) {
        super("MakEshray", 10, 4, 5, new Equipment.Weapon("Longblaster",
                new OffenseDieType[] { OffenseDieType.BLUE, OffenseDieType.BLUE }, new Equipment.SurgeOptions[] {
                        Equipment.SurgeOptions.DAMAGE1,
                        Equipment.SurgeOptions.PIERCE1 },
                false, false, "makeshray_default"),
                pos, false, new DefenseDieType[] { DefenseDieType.WHITE }, false);
    }

    @Override
    public void applyAttackAbilities(Personnel defender, TotalAttackResult totalResults) {
        if (defender.hasLineOfSightTo(this)) {
            return;
        }
        boolean useAmbush = game != null
                ? game.promptYesNo(getOwnerSeat(), "Ambush", "Use Ambush to gain Pierce 2?")
                : InputUtils.getYesNo("Ambush", "Use Ambush to gain Pierce 2?");
        if (!useAmbush) {
            return;
        }
        ApplyStrain(1);
        for (int i = 0; i < 2 && totalResults.getDamage() < 0; i++) {
            totalResults.addDamage(1);
        }
    }

    public boolean isCovertAgainst(Personnel other) {
        if (other == null) {
            return false;
        }
        for (Pos ownSpace : getOccupiedSpaces()) {
            for (Pos otherSpace : other.getOccupiedSpaces()) {
                if (Pathfinder.canReachPoint(ownSpace, otherSpace, 3, false, game)) {
                    return false;
                }
            }
        }
        return true;
    }
}
