package src.game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import src.Constants;
import src.Screen;
import src.game.Die.*;
import src.game.FullDeployment.PersonnelStatus;

public abstract class Personnel {
    private int startingHealth, health, speed;
    private int xSize = 1, ySize = 1;
    private Pos pos;
    private Pos[] corners;
    protected boolean stunned = false, focused = false;
    private BufferedImage image;
    private String name;
    private DefenseDieType[] defenseDice;
    private static int imageSideSpace = 7;
    private boolean possibleTarget = false, active = false;
    protected ArrayList<Actions> actions;

    public static enum Directions {
        UP,
        UPLEFT,
        LEFT,
        DOWNLEFT,
        DOWN,
        DOWNRIGHT,
        RIGHT,
        UPRIGHT
    }

    public static enum Actions {
        MOVE,
        ATTACK,
        RECOVER,
        SPECIAL,
        INTERACT
    }

    public Personnel(String name, int startingHealth, int speed, Pos pos, DefenseDieType[] defenseDice, boolean hasSpecial) {
        this.name = name;
        this.startingHealth = startingHealth;
        this.health = startingHealth;
        this.speed = speed;
        this.defenseDice = defenseDice;
        try {
            this.image = ImageIO.read(new File(Constants.baseImgFilePath + name + ".jpg"));
        } catch (IOException ex) {
            try {
                this.image = ImageIO.read(new File(Constants.baseImgFilePath + name + ".png"));
            } catch (IOException e) {
                System.out.println("Couldn't read both in jpg or png" + ex);
                System.exit(0);
            }
        }
        this.pos = pos;
        this.corners = new Pos[] {pos, pos.getNextPos(Directions.RIGHT), pos.getNextPos(Directions.DOWN), pos.getNextPos(Directions.DOWNRIGHT)};
        this.actions = new ArrayList<>(Arrays.asList(Actions.MOVE, Actions.ATTACK));
        if (hasSpecial) {
            actions.add(Actions.SPECIAL);
        }
    }

    public void performAttack(Personnel other) {
        Game.clearDice();
        int surges = 0;
        TotalAttackResult totalResults = new TotalAttackResult();
        for (DefenseRoll roll : getDefense(other)) {
            DefenseDieResult result = roll.result();
            if (result.dodge()) {
                return;
            }
            totalResults.addDamage(-result.shields());
            surges -= result.surgeCancel();
        }
        int postPierceDamage = 0;
        for (OffenseRoll roll : getOffense()) {
            OffenseDieResult result = roll.result();
            postPierceDamage += result.damage();
            surges += result.surge();
            totalResults.addAccuracy(result.accuracy());
        }
        Game.repaintScreen();
        ArrayList<Equipment.SurgeOptions> surgeOptions = new ArrayList<Equipment.SurgeOptions>();
        Equipment.SurgeOptions[] possibleActions = getSurgeOptions();
        for (Equipment.SurgeOptions option : possibleActions) {
            surgeOptions.add(option);
        }
        while (surges > 0 && !surgeOptions.isEmpty()) {
            int selectedIndex = InputUtils.getMultipleChoice("Surge Selection", "Spend Surges: " + surges,
                    surgeOptions.toArray());
            if (selectedIndex < 0 || selectedIndex >= surgeOptions.size()) {
                break;
            }
            Equipment.surgeEffects.get(surgeOptions.remove(selectedIndex)).accept(new Personnel[] { this, other },
                    totalResults);
            surges--;
        }
        totalResults.addDamage(postPierceDamage);
        if ((getRange() != Integer.MAX_VALUE || Pathfinder.canReachPoint(pos, other.getPos(), totalResults.getAccuracy(), false)) && totalResults.getDamage() > 0) {
            other.dealDamage(totalResults.getDamage());
        }
    }

    public DefenseRoll[] getDefense() {
        DefenseRoll[] results = new DefenseRoll[defenseDice.length];
        for (int i = 0; i < defenseDice.length; i++) {
            results[0] = defenseDice[i].roll();
        }
        return results;
    }

    public abstract OffenseRoll[] getOffense();

    public abstract Equipment.SurgeOptions[] getSurgeOptions();

    public void dealDamage(int damage) {
        health -= damage;
        if (health > startingHealth) {
            health = startingHealth;
        }
    }

    public boolean getDead() {
        return health <= 0;
    }

    public void move(Directions dir) {
        pos.move(dir);
    }

    public void incrementX(int amt) {
        if (pos.getX() + amt >= Constants.tileMatrix[0].length) {
            System.out.println("You tried to add " + amt + " to " + pos.getX()
                    + "(x), but the length of the row was " + Constants.tileMatrix[0].length);
            System.exit(0);
        }
        pos.incrementX(amt);
    }

    public void incrementY(int amt) {
        if (pos.getY() + amt >= Constants.tileMatrix.length) {
            System.out.println("You tried to add " + amt + " to " + pos.getY()
                    + "(y), but the number of columns was " + Constants.tileMatrix.length);
            System.exit(0);
        }
        pos.incrementY(amt);
    }

    public void setStunned(boolean value) {
        stunned = value;
    }

    public DefenseRoll[] getDefense(Personnel other) {
        return other.getDefense();
    }

    public void draw(Graphics g) {
        g.drawImage(image, pos.getFullX() + imageSideSpace,
                pos.getFullY() + imageSideSpace,
                Constants.tileSize * (pos.getX() + xSize) - imageSideSpace,
                Constants.tileSize * (pos.getY() + ySize) - imageSideSpace, 0, 0, image.getWidth(null), image.getHeight(null),
                null);
        if (Screen.getSelectingCombat() && !possibleTarget) {
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRect(pos.getFullX() + imageSideSpace,
                pos.getFullY() + imageSideSpace, Constants.tileSize * xSize - 2 * imageSideSpace, Constants.tileSize * ySize - 2 * imageSideSpace);
        }
        if (active) {
            g.setColor(new Color(0, 255, 0, 70));
            g.fillRect(pos.getFullX() + imageSideSpace,
                    pos.getFullY() + imageSideSpace, Constants.tileSize * xSize - 2 * imageSideSpace,
                    Constants.tileSize * ySize - 2 * imageSideSpace);
        }
    }

    public String getName() {
        return name;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean canMove(Directions dir) {
        return pos.canMove(dir, true);
    }

    public Pos getPos() {
        return pos;
    }

    public void performSpecial() {
    }

    public int getRange() {
        return Integer.MAX_VALUE;
    }

    public boolean canAttack(Personnel other) {
        int range = getRange();
        if (range != Integer.MAX_VALUE && !Pathfinder.canReachPoint(pos, other.getPos(), range, false)) {
            return false;
        }
        for (Pos corner : corners) {
            int sightCount = 0;
            for (Pos enemyCorner : other.getCorners()) {
                if (Pathfinder.straightlineToPos(corner, enemyCorner)) {
                    sightCount++;
                    if (sightCount >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Pos[] getCorners() {
        return corners;
    }

    public Pos getClosestCorner(Personnel other) {
        double minDistance = Integer.MAX_VALUE;
        Pos otherPos = other.getPos();
        Pos closestCorner = null;
        for (Pos corner : corners) {
            double distance = Pos.getDistance(otherPos, corner);
            if (distance < minDistance) {
                minDistance = distance;
                closestCorner = corner;
            }
        }
        return closestCorner;
    }

    public void setPossibleTarget(boolean value) {
        possibleTarget = value;
    }

    public void setActive(boolean value) {
        active = value;
    }

    public ArrayList<Actions> getActions() {
        return actions;
    }

    public PersonnelStatus getStatus() {
        return new PersonnelStatus(health, stunned, focused);
    }

    public boolean stunned() {
        return stunned;
    }
}
