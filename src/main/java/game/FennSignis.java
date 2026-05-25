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
                false, false, "fennsignis_default"),
                pos, false, new DefenseDieType[] { DefenseDieType.BLACK }, false);
    }

    @Override
    public void performAttack(Personnel other) {
        boolean useHavocShot = game != null
                ? game.promptYesNo(getOwnerSeat(), "Havoc Shot", "Gain 1 strain to apply Blast 1 to this attack?")
                : InputUtils.getYesNo("Havoc Shot", "Gain 1 strain to apply Blast 1 to this attack?");
        if (useHavocShot) {
            ApplyStrain(1);
            havocShotActive = true;
        }
        try {
            super.performAttack(other);
        } finally {
            havocShotActive = false;
        }
    }

    @Override
    public int getBlastValue() {
        return havocShotActive ? 1 : 0;
    }

    @Override
    public void onActivationEnd() {
        if (!hasFriendlyFigureAdjacent()) {
            ApplyStrain(-1);
        }
    }

    private boolean hasFriendlyFigureAdjacent() {
        if (game == null) {
            return false;
        }
        for (Hero hero : game.getHeroes()) {
            if (hero == this || hero.isDefeated()) {
                continue;
            }
            for (Pos ownSpace : getOccupiedSpaces()) {
                for (Pos friendlySpace : hero.getOccupiedSpaces()) {
                    int xDistance = Math.abs(ownSpace.getX() - friendlySpace.getX());
                    int yDistance = Math.abs(ownSpace.getY() - friendlySpace.getY());
                    if (xDistance <= 1 && yDistance <= 1 && (xDistance + yDistance) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
