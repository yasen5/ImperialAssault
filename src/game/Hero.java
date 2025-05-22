package src.game;

import java.awt.Graphics;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import src.Constants;
import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;
import src.game.Die.OffenseRoll;

public abstract class Hero extends Personnel {
    private int endurance, strain;
    protected boolean wounded;
    private Equipment.Weapon weapon;
    private boolean exhausted = false;
    private DeploymentCard deploymentCard;
    private boolean hasSpecial;

    public static enum Actions {
        ATTACK,
        RECOVER,
        SPECIAL
    }

    public Hero(String name, int startingHealth, int speed, int endurance, Equipment.Weapon weapon, Pos pos,
            int deploymentX, int deploymentY, boolean hasSpecial, DefenseDieType[] defenseDice) {
        super(name, startingHealth, speed, pos, defenseDice);
        this.endurance = endurance;
        this.weapon = weapon;
        this.wounded = false;
        this.strain = 0;
        this.deploymentCard = new DeploymentCard(Constants.baseImgFilePath + name + "Deployment.jpg", deploymentX,
                deploymentY);
        this.hasSpecial = hasSpecial;
    }

    public void ApplyStrain(int strain) {
        this.strain += strain;
        if (this.strain > endurance) {
            dealDamage(strain - endurance);
            strain = endurance;
        }
    }

    public boolean getExhausted() {
        return exhausted;
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
        deploymentCard.draw(g);
    }

    public Actions[] getActions() {
        return hasSpecial ? new Actions[] {Actions.ATTACK,
                Actions.RECOVER, Actions.SPECIAL} : new Actions[] {Actions.ATTACK, Actions.RECOVER};
    }

    @Override
    public OffenseRoll[] getOffense() {
        OffenseDieType[] attackDice = weapon.attackDice();
        OffenseRoll[] result = new OffenseRoll[attackDice.length];
        for (int i = 0; i < attackDice.length; i++) {
            result[i] = attackDice[i].roll();
        }
        return result;
    }

    public Equipment.SurgeOptions[] getSurgeOptions() {
        return weapon.surgeOptions();
    }
}
