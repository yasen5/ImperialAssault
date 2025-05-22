package src.game;

import java.awt.Graphics;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import src.Constants;

public abstract class Hero extends Personnel {
    private int endurance, strain;
    protected boolean wounded;
    protected Equipment.Weapon weapon;
    private boolean exhausted = false;
    private DeploymentCard deploymentCard;

    public Hero(String name, int startingHealth, int speed, int endurance, Equipment.Weapon weapon, Pos pos,
            int deploymentX, int deploymentY) {
        super(name, startingHealth, speed, pos);
        this.endurance = endurance;
        this.weapon = weapon;
        this.wounded = false;
        this.strain = 0;
        this.deploymentCard = new DeploymentCard(Constants.baseImgFilePath + name + "Deployment.jpg", deploymentX,
                deploymentY);
        
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
}
