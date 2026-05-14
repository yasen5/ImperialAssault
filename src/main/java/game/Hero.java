package game;

import java.awt.Graphics;

import game.Die.DefenseDieType;
import game.Die.OffenseDieType;
import game.Die.OffenseRoll;
import util.MyArrayList;

public abstract class Hero extends Personnel implements FullDeployment {
    // Instance variables
    private int endurance;
    protected boolean wounded;
    private Equipment.Weapon weapon;
    private Equipment.Item defaultEquipment;
    private MyArrayList<Equipment.Item> equipment = new MyArrayList<>();
    private boolean exhausted = false;
    private DeploymentCard deploymentCard;
    private boolean displayStats = false;

    // Constructor
    public Hero(String name, int startingHealth, int speed, int endurance, Equipment.Weapon weapon, Pos pos,
            boolean hasSpecial, DefenseDieType[] defenseDice, boolean specialRequiresSelection) {
        super(name, startingHealth, speed, pos, defenseDice, hasSpecial, specialRequiresSelection);
        this.endurance = endurance;
        this.weapon = weapon;
        this.defaultEquipment = Equipment.asItem(weapon);
        this.equipment.add(defaultEquipment);
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
        this.deploymentCard.setExhausted(exhausted);
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public Integer getMaxStrain() {
        return endurance;
    }

    @Override
    public int getMaxHealth() {
        return getStartingHealth();
    }

    @Override
    public boolean isExhausted() {
        return exhausted;
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
            result[i] = attackDice[i].roll(game);
        }
        if (focused) {
            result[result.length - 1] = OffenseDieType.GREEN.roll(game);
            focused = false;
        }
        return result;
    }

    public Equipment.SurgeOptions[] getSurgeOptions() {
        return weapon.surgeOptions();
    }

    public void addEquipment(Equipment.Item item) {
        if (item == null || hasEquipment(item.id())) {
            return;
        }
        equipment.add(item);
    }

    @Override
    public MyArrayList<Equipment.Item> getEquipment() {
        return new MyArrayList<>(equipment);
    }

    public MyArrayList<String> getEquipmentIds() {
        MyArrayList<String> ids = new MyArrayList<>();
        for (Equipment.Item item : equipment) {
            ids.add(item.id());
        }
        return ids;
    }

    public void applyEquipmentIds(MyArrayList<String> equipmentIds) {
        equipment.clear();
        equipment.add(defaultEquipment);
        if (equipmentIds == null) {
            return;
        }
        for (String equipmentId : equipmentIds) {
            if (equipmentId == null || equipmentId.equals(defaultEquipment.id()) || hasEquipment(equipmentId)) {
                continue;
            }
            Equipment.Item item = Equipment.findItem(equipmentId);
            if (item != null) {
                equipment.add(item);
            }
        }
    }

    private boolean hasEquipment(String equipmentId) {
        for (Equipment.Item item : equipment) {
            if (item.id().equals(equipmentId)) {
                return true;
            }
        }
        return false;
    }

    public DeploymentCard getDeploymentCard() {
        return deploymentCard;
    }

    @Override
    public String[] getStatusLabels() {
        return new String[] { getName() };
    }

    public int getEndurance() {
        return endurance;
    }

    @Override
    public int getRange() {
        if (weapon.melee()) {
            return weapon.reach() ? 2 : 1;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public PersonnelStatus[] getStatuses() {
        PersonnelStatus[] statuses = new PersonnelStatus[] { getStatus() };
        return statuses;
    }

    @Override
    public void toggleDisplay() {
        displayStats = !displayStats;
    }
}
