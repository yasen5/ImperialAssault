package src.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import src.Constants;
import src.game.Die.*;

public abstract class Personnel {
    private int startingHealth, health, speed;
    private int xSize = 1, ySize = 1;
    private Pos pos;
    private boolean stunned, bleeding;
    private BufferedImage image;
    private String name;
    private DefenseDieType[] defenseDice;
    protected boolean hasSpecial;

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

    public Personnel(String name, int startingHealth, int speed, Pos pos, DefenseDieType[] defenseDice,
            boolean hasSpecial) {
        this.name = name;
        this.startingHealth = startingHealth;
        this.health = startingHealth;
        this.speed = speed;
        this.stunned = false;
        this.bleeding = false;
        this.defenseDice = defenseDice;
        this.hasSpecial = hasSpecial;
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
                null);
        g.setColor(new Color(255, 255, 255));
        g.fillRect(pos.getFullX(), pos.getFullY(), Constants.tileSize,
                (int) (Constants.tileSize * 0.1));
        g.fillRect(pos.getFullX(), pos.getFullY() + (int) (Constants.tileSize * 0.9),
                Constants.tileSize,
                (int) (Constants.tileSize * 0.1));
        g.fillRect(pos.getFullX(), pos.getFullY(), (int) (Constants.tileSize * 0.1),
                Constants.tileSize);
        g.fillRect(pos.getFullX() + (int) (Constants.tileSize * 0.9), pos.getFullY(),
                (int) (Constants.tileSize * 0.1),
                Constants.tileSize);
        g.setColor(new Color(0, 0, 0));
        g.drawRect(pos.getFullX(), pos.getFullY(), Constants.tileSize,
                Constants.tileSize);
        g.setFont(new Font("Bookman Old Style", Font.BOLD, 11));
        g.setColor(new Color(140, 0, 0));
        g.drawString("" + health, pos.getFullX(), (pos.getY() + 1) * Constants.tileSize);
    }

    public String getName() {
        return name;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean canMove(Directions dir) {
        return pos.canMove(dir);
    }

    public Pos getPos() {
        return pos;
    }

    public void performSpecial() {
    }
}
