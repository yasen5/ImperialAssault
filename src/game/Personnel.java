package src.game;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JButton;

import src.Constants;
import src.game.Die.*;

public abstract class Personnel {
    private int startingHealth, health, speed;
    private int xSize = 1, ySize = 1;
    private Pos pos;
    private boolean stunned, bleeding;
    private BufferedImage image;
    private String name;
    private JButton selectionButton;
    private DefenseDieType[] defenseDice;

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

    public Personnel(String name, int startingHealth, int speed, Pos pos, DefenseDieType[] defenseDice) {
        this.name = name;
        this.startingHealth = startingHealth;
        this.health = startingHealth;
        this.speed = speed;
        this.stunned = false;
        this.bleeding = false;
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
        this.selectionButton = new JButton();
        this.selectionButton.setOpaque(false);
        this.selectionButton.setContentAreaFilled(false);
        this.selectionButton.setBorderPainted(false);
        moveButtonToCurrentLocation();
    }

    public void performAttack(Personnel other) {
        int surges = 0;
        TotalAttackResult totalResults = new TotalAttackResult();
        for (OffenseRoll roll : getOffense()) {
            OffenseDieResult result = roll.result();
            totalResults.addDamage(result.damage());
            surges += result.surge();
            totalResults.addAccuracy(result.accuracy());
        }
        ArrayList<Equipment.SurgeOptions> surgeOptions = new ArrayList<Equipment.SurgeOptions>();
        Equipment.SurgeOptions[] possibleActions = getSurgeOptions();
        for (Equipment.SurgeOptions option : possibleActions) {
            surgeOptions.add(option);
        }
        while (surges > 0 && !surgeOptions.isEmpty()) {
            int selectedIndex = InputUtils.getMultipleChoice("Surge Selection", "Spend Surges: " + surges, surgeOptions.toArray());
            if (selectedIndex < 0 || selectedIndex >= surgeOptions.size()) {
                break;
            }
            Equipment.surgeEffects.get(surgeOptions.remove(selectedIndex)).accept(new Personnel[] { this, other },
                    totalResults);
            surges--;
        }
        for (DefenseRoll roll : getDefense(other)) {
            DefenseDieResult result = roll.result();
            if (result.dodge()) {
                return;
            }
            totalResults.addDamage(-result.shields());
            surges -= result.surgeCancel();
        }
        if (totalResults.getDamage() > 0) {
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
        switch (dir) {
            case UP -> incrementY(-1);
            case DOWN -> incrementY(1);
            case LEFT -> incrementX(-1);
            case RIGHT -> incrementX(1);
            case UPLEFT -> {
                incrementX(-1);
                incrementY(-1);
            }
            case UPRIGHT -> {
                incrementX(1);
                incrementY(-1);
            }
            case DOWNLEFT -> {
                incrementX(-1);
                incrementY(1);
            }
            case DOWNRIGHT -> {
                incrementX(1);
                incrementY(1);
            }
        }
        // moveButtonToCurrentLocation();
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

    public void setBleeding(boolean value) {
        bleeding = value;
    }

    public DefenseRoll[] getDefense(Personnel other) {
        return other.getDefense();
    }

    public void draw(Graphics g) {
        g.drawImage(image, Constants.tileSize * pos.getX(),
                Constants.tileSize * pos.getY(),
                Constants.tileSize * (pos.getX() + xSize),
                Constants.tileSize * (pos.getY() + ySize), 0, 0, image.getWidth(null), image.getHeight(null),
                null);    }

    public String getName() {
        return name;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean canMove(Directions dir) {
        int newX = pos.getX();
        int newY = pos.getY();
        switch (dir) {
            case UP -> {
                newY -= 1;
            }
            case DOWN -> {
                newY += 1;
            }
            case LEFT -> {
                newX -= 1;
            }
            case RIGHT -> {
                newX += 1;
            }
            case UPLEFT -> {
                newX -= 1;
                newY -= 1;
            }
            case UPRIGHT -> {
                newX += 1;
                newY -= 1;
            }
            case DOWNLEFT -> {
                newX -= 1;
                newY += 1;
            }
            case DOWNRIGHT -> {
                newX += 1;
                newY += 1;
            }
        }
        boolean inBounds = newX >= 0 && newY >= 0 && newX < Constants.tileMatrix[0].length
                && newY < Constants.tileMatrix.length;
        return (inBounds && Constants.tileMatrix[newY][newX] == 1 && Game.isSpaceAvailable(new Pos(newX, newY)));
    }

    public Pos getPos() {
        return pos;
    }

    public JButton getSelectionButton() {
        return selectionButton;
    }

    public void moveButtonToCurrentLocation() {
        this.selectionButton.setBounds(pos.getX() * Constants.tileSize, pos.getY() * Constants.tileSize,
                Constants.tileSize, Constants.tileSize);
    }
}
