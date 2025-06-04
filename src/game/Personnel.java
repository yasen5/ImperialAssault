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
import src.Screen.SelectingType;
import src.game.Die.*;
import src.game.FullDeployment.PersonnelStatus;
import src.game.Pathfinder.FullPos;

public abstract class Personnel {
    // Instance variables
    private int startingHealth, health, speed;
    private int xSize = 1, ySize = 1;
    private Pos pos;
    private Pos[] corners;
    protected boolean stunned = false, focused = false;
    private BufferedImage image;
    private String name;
    protected DefenseDieType[] defenseDice;
    private static int imageSideSpace = 7;
    private boolean possibleTarget = false, active = false;
    protected ArrayList<Actions> actions;
    protected int strain = 0;
    private boolean specialRequiresSelection;

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

    public Personnel(String name, int startingHealth, int speed, Pos pos, DefenseDieType[] defenseDice,
            boolean hasSpecial, boolean specialRequiresSelection) {
        this.name = name;
        this.startingHealth = startingHealth;
        this.health = startingHealth;
        this.speed = speed;
        this.defenseDice = defenseDice;
        this.specialRequiresSelection = specialRequiresSelection;
        // Tries both .jpg and .png
        try {
            this.image = ImageIO.read(new File(Constants.baseImgFilePath + name + ".jpg"));
        } catch (IOException ex) {
            try {
                this.image = ImageIO.read(new File(Constants.baseImgFilePath + name + ".png"));
            } catch (IOException e) {
                throw new java.lang.RuntimeException("Couldn't read either in jpg or png, tried " + Constants.baseImgFilePath + name
                        + ".jpg" + " and " + Constants.baseImgFilePath
                        + name + ".png" + ex);
            }
        }
        this.pos = pos;
        this.corners = new Pos[] { pos, pos.getNextPos(Directions.RIGHT), pos.getNextPos(Directions.DOWN),
                pos.getNextPos(Directions.DOWNRIGHT) };
        this.actions = new ArrayList<>(Arrays.asList(Actions.MOVE, Actions.ATTACK));
        if (hasSpecial) {
            actions.add(Actions.SPECIAL);
        }
    }

    // Perform an attack by getting the defender's results, subtracting them from
    // your results, and then doing surge effects, then figuring out if you had
    // enough range
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
        // Pierce only acts when there are shields present, which is why it is separate
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
        // Do all the surge options
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
        // Check if there is high enough accuracy
        if ((getRange() != Integer.MAX_VALUE
                || Pathfinder.canReachPoint(pos, other.getPos(), totalResults.getAccuracy(), false))
                && totalResults.getDamage() > 0) {
            other.dealDamage(totalResults.getDamage());
        }
    }

    // Roll all the defense dice
    public DefenseRoll[] getDefense() {
        DefenseRoll[] results = new DefenseRoll[defenseDice.length];
        for (int i = 0; i < defenseDice.length; i++) {
            results[0] = defenseDice[i].roll();
        }
        return results;
    }

    public abstract OffenseRoll[] getOffense();

    public abstract Equipment.SurgeOptions[] getSurgeOptions();

    // Deal damage, can't go above starting health
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

    public void setStunned(boolean value) {
        stunned = value;
    }

    public DefenseRoll[] getDefense(Personnel other) {
        return other.getDefense();
    }

    // Draw the image, do various shades on top based on whether it is active, being
    // targeted, etc.
    public void draw(Graphics g) {
        g.drawImage(image, pos.getFullX() + imageSideSpace,
                pos.getFullY() + imageSideSpace,
                Constants.tileSize * (pos.getX() + xSize) - imageSideSpace,
                Constants.tileSize * (pos.getY() + ySize) - imageSideSpace, 0, 0, image.getWidth(null),
                image.getHeight(null),
                null);
        if (Screen.getSelectionType() == SelectingType.COMBAT || Screen.getSelectionType() == SelectingType.SPECIAL && !possibleTarget) {
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRect(pos.getFullX() + imageSideSpace,
                    pos.getFullY() + imageSideSpace, Constants.tileSize * xSize - 2 * imageSideSpace,
                    Constants.tileSize * ySize - 2 * imageSideSpace);
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
        return pos.canMove(dir, true, true);
    }

    public Pos getPos() {
        return pos;
    }

    // Optional functions that define special action behavior
    public void performSpecial() {
        if (specialRequiresSelection) {
            throw new java.lang.RuntimeException("Performing special without required selection");
        }
    }

    public void performSpecial(Personnel selected) {
        if (!specialRequiresSelection) {
            throw new java.lang.RuntimeException("Performing special with selection that isn't require it");
        }
    }

    public int getRange() {
        return Integer.MAX_VALUE;
    }

    // Checks if the defender is in line of sight and if melee, if they are < range
    // spaces away
    public boolean canAttack(Personnel other) {
        int range = getRange();
        if (range != Integer.MAX_VALUE && !Pathfinder.canReachPoint(pos, other.getPos(), range, false)) {
            return false;
        }
        for (Pos corner : corners) {
            Pos[] cornersUsed = new Pos[2];
            int sightCount = 0;
            for (Pos enemyCorner : other.getCorners()) {
                if (Pathfinder.straightlineToPos(corner, enemyCorner)) {
                    cornersUsed[sightCount >= 2 ? 0 : sightCount] = enemyCorner;
                    sightCount++;
                    if (sightCount >= 2) {
                        if (!Pos.onOneLine(pos, cornersUsed[0], cornersUsed[1])) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public Pos[] getCorners() {
        return corners;
    }

    // Get the closest corner to the other Personnel
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
        return new PersonnelStatus(health, strain, stunned, focused);
    }

    public boolean stunned() {
        return stunned;
    }

    @Override
    public String toString() {
        return name;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public ArrayList<Personnel> getSpecialTargets() {
        return null;
    }

    public boolean specialRequiresSelection() {
        return specialRequiresSelection;
    }
}
