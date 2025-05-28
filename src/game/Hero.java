package src.game;

import java.awt.Graphics;

import src.Constants;
import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;
import src.game.Die.OffenseRoll;

public abstract class Hero extends Personnel implements FullDeployment {
    private int endurance, strain;
    protected boolean wounded;
    private Equipment.Weapon weapon;
    private boolean exhausted = false;
    private DeploymentCard deploymentCard;

    public static enum HeroActions {
        MOVE,
        ATTACK,
        RECOVER,
        SPECIAL
    }

    public Hero(String name, int startingHealth, int speed, int endurance, Equipment.Weapon weapon, Pos pos,
            boolean hasSpecial, DefenseDieType[] defenseDice) {
        super(name, startingHealth, speed, pos, defenseDice, hasSpecial);
        this.endurance = endurance;
        this.weapon = weapon;
        this.wounded = false;
        this.strain = 0;
        this.deploymentCard = new DeploymentCard(Constants.baseImgFilePath + name + "Deployment.jpg", true);
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

    public HeroActions[] getActions() {
        return hasSpecial ? new HeroActions[] { HeroActions.MOVE, HeroActions.ATTACK,
                HeroActions.RECOVER, HeroActions.SPECIAL }
                : new HeroActions[] { HeroActions.MOVE, HeroActions.ATTACK, HeroActions.RECOVER };
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

    public DeploymentCard getDeploymentCard() {
        return deploymentCard;
    }

    public int getEndurance() {
        return endurance;
    }
}
