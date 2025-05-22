package src.game;

import src.game.Die.*;

public class DialaPassil extends Hero {
    public DialaPassil(Pos pos, int deploymentX, int deploymentY) {
        super("DialaPassil", 12, 300, 5, new Equipment.Weapon("Plasteel Staff",
                new OffenseDieType[] { OffenseDieType.GREEN, OffenseDieType.BLUE }, new Equipment.SurgeOptions[] {
                        Equipment.SurgeOptions.STUN,
                        Equipment.SurgeOptions.DAMAGE1 }),
                pos, deploymentX, deploymentY, false, new DefenseDieType[] {DefenseDieType.WHITE});
    }

    @Override
    public DefenseRoll[] getDefense() {
        DefenseRoll[] result = super.getDefense();
        if (InputUtils.getYesNo("You rolled: " + result.toString(), "Would you like to reroll?")) {
            ApplyStrain(1);
            result = super.getDefense();
        }
        return result;
    }

    @Override
    public DefenseRoll[] getDefense(Personnel other) {
        if (InputUtils.getYesNo("Ability", "Remove a die from defense pool? (2 Strain)")) {
            ApplyStrain(2);
            DefenseRoll[] defense = other.getDefense();
            DefenseRoll[] modifiedDefense = new DefenseRoll[defense.length - 1];
            for (int i = 1; i < defense.length; i++) {
                modifiedDefense[i - 1] = defense[i];
            }
            return modifiedDefense;
        }
        return other.getDefense();
    }
}
