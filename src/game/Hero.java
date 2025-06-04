package src.game;

import java.awt.Graphics;

import src.game.Die.DefenseDieType;
import src.game.Die.OffenseDieType;
import src.game.Die.OffenseRoll;

public abstract class Hero extends Personnel implements FullDeployment {
    // Instance variables
    private int endurance;
    protected boolean wounded;
    private Equipment.Weapon weapon;
    private boolean exhausted = false;
    private DeploymentCard deploymentCard;
    private boolean displayStats = false;

    // Constructor
    public Hero(String name, int startingHealth, int speed, int endurance, Equipment.Weapon weapon, Pos pos,
            boolean hasSpecial, DefenseDieType[] defenseDice, boolean specialRequiresSelection) {
        super(name, startingHealth, speed, pos, defenseDice, hasSpecial, specialRequiresSelection);
        this.endurance = endurance;
        this.weapon = weapon;
        this.wounded = false;
        this.deploymentCard = new DeploymentCard(name, true, this);
        this.actions.add(Actions.RECOVER);
    }

    // Add strain, if too much strain, turn it into damage instead
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

    public void setExhausted(boolean exhausted) {
        this.exhausted = exhausted; 
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
        deploymentCard.draw(g);
        if (displayStats) {
            drawStats(g);
        }
    }

    // Get the offense from the weapon's dice, if focused, add a green die
    @Override
    public OffenseRoll[] getOffense() {
        OffenseDieType[] attackDice = weapon.attackDice();
        OffenseRoll[] result = new OffenseRoll[attackDice.length + (focused ? 1 : 0)];
        for (int i = 0; i < attackDice.length; i++) {
            result[i] = attackDice[i].roll();
        }
        if (focused) {
            result[result.length - 1] = OffenseDieType.GREEN.roll();
            focused = false;
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
    
    @Override
    public int getRange() {
        if (weapon.melee()) {
            return weapon.reach() ? 2 : 1;
        }
        else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public PersonnelStatus[] getStatuses() {
        PersonnelStatus[] statuses = new PersonnelStatus[] {getStatus()};
        return statuses;
    }

    @Override
    public void toggleDisplay() {
        displayStats = !displayStats;
    }
}
