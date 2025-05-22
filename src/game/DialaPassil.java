package src.game;

import javax.swing.JOptionPane;

import src.Constants;
import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;

public class DialaPassil extends Hero {
    public DialaPassil(Pos pos, int deploymentX, int deploymentY) {
        super("DialaPassil", 12, 300, 5, new Equipment.Weapon("Plasteel Staff",
                new OffenseDieType[] { OffenseDieType.GREEN, OffenseDieType.BLUE }, new Equipment.SurgeOptions[] {
                        Equipment.SurgeOptions.STUN,
                        Equipment.SurgeOptions.DAMAGE1 }),
                pos, deploymentX, deploymentY);
    }

    @Override
    public Die.DefenseDieResult[] getDefense() {
        Die.DefenseDieResult[] result = new Die.DefenseDieResult[] { Die.rollDefense(DefenseDieType.WHITE) };
        int response = JOptionPane.showConfirmDialog(
                null,
                "You rolled: " + result.toString(),
                "Would you like to reroll?",
                JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            ApplyStrain(1);
            result = new Die.DefenseDieResult[] { Die.rollDefense(DefenseDieType.WHITE) };
        }
        return result;
    }

    @Override
    public Die.OffenseDieResult[] getOffense() {
        Die.OffenseDieType[] attackDice = weapon.attackDice();
        Die.OffenseDieResult[] result = new Die.OffenseDieResult[attackDice.length];
        for (int i = 0; i < attackDice.length; i++) {
            result[i] = Die.rollOffense(attackDice[i]);
        }
        return result;
    }

    @Override
    public Die.DefenseDieResult[] getDefense(Personnel other) {
        int response = JOptionPane.showConfirmDialog(
                null,
                "Ability",
                "Remove a die from defense pool? (2 Strain)",
                JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            ApplyStrain(2);
            Die.DefenseDieResult[] defense = other.getDefense();
            Die.DefenseDieResult[] modifiedDefense = new Die.DefenseDieResult[defense.length - 1];
            for (int i = 1; i < defense.length; i++) {
                modifiedDefense[i - 1] = defense[i];
            }
            return modifiedDefense;
        }
        return other.getDefense();
    }

    @Override
    public Equipment.SurgeOptions[] getSurgeOptions() {
        return weapon.surgeOptions();
    }
}
